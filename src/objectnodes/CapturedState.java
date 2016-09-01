package objectnodes;

import java.util.HashMap;
import java.util.List;

import javax.swing.JTree;

import com.sun.jdi.ArrayReference;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.Field;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.InvalidTypeException;
import com.sun.jdi.InvocationException;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.PrimitiveValue;
import com.sun.jdi.StringReference;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;

import kevin.BreakpointEntry;
import kevin.BreakpointType;
import kevin.Utils;

public class CapturedState {

	/**
	 * the top-level root object of the tree contained within this CapturedState. 
	 */
	VariableNode rootObject;
	
	/**
	 * The objects already captured. Used to avoid infinite recursion,
	 * which happens when there are circular references. 
	 */
	HashMap<Value, VariableNode> objectToNodeMap = new HashMap<>();
	
	/**
	 * A reference to the thread that was running when this state was captured. 
	 */
	ThreadReference threadRef;
	
	/**
	 * The BreakpointEntry corresponding to when this state was captured, including 
	 * <li> the method that was currently executing when this state was captured.
	 *      (this capturedState was captured while inside this method),
	 * <li> The BreakpointType at which this state was captured, 
	 *      e.g., whether this capturedState object was captured at a method's entry or exit.   
	 */
	BreakpointEntry entry;

	/**
	 * the maximum depth to recurse down through the object graph
	 */
	int maxDepth;
	public static int DEFAULT_MAX_DEPTH = 10;

	
	public CapturedState(ThreadReference thr, ObjectReference obj, BreakpointEntry e, int md) {
		this.threadRef = thr;
		this.entry = e;
		
		if (maxDepth >= 0) {
			this.maxDepth = md;
		}
		
		captureObjectState(obj);	
	}
	
	
	public CapturedState(ThreadReference thr, ObjectReference obj, BreakpointEntry e) {
		this(thr, obj, e, DEFAULT_MAX_DEPTH);
	}
	
	
	public void captureObjectState(Value object) {
		long start = System.currentTimeMillis();

		rootObject = captureState(object, "this (top-level)", object.type().name(), null, 0, this.maxDepth, true);
		
		long elapsed = System.currentTimeMillis() - start;
		System.out.println("Captured state in " + ((float)elapsed/1000.0f) + " seconds.");
	}
	
	

	/**
	 * 
	 * @param obj
	 * @param fieldName
	 * @param fieldType
	 * @param parent
	 * @param depth
	 * @param maxDepth
	 * @param includeInherited
	 * @return
	 */
	private VariableNode captureState(Value obj, String fieldName, String fieldType, VariableNode parent, int depth, int maxDepth, boolean includeInherited) {
        
        if (depth <= maxDepth) {

        	// detect circular references for non-leaf (non primitive/string) types
        	if (!(obj instanceof PrimitiveValue || obj instanceof StringReference)) {
        		VariableNode redundant = this.objectToNodeMap.get(obj);
        		if (redundant != null) {
//        			return redundant; 
        			// here we can return null instead to FORCE no circular recursion
        			return null;
        		}
        	}
        	
        	
        	if (obj instanceof PrimitiveValue) {
        		PrimitiveNode node = new PrimitiveNode(fieldName, fieldType, (PrimitiveValue)obj, parent);
        		return node;
        	}
        	else if (obj instanceof StringReference) {
        		StringNode node = new StringNode(fieldName, fieldType, (StringReference)obj, parent);
        		return node;
        	}
        	else if (obj instanceof ArrayReference) {
        		ArrayReference arr = (ArrayReference)obj;
                ArrayNode arrnode = new ArrayNode(fieldName, arr.type().name(), arr, parent);
                this.objectToNodeMap.put(arr, arrnode);
            	for (int i = 0; i < arr.length(); i++) {
            		Value arrayVal = arr.getValue(i);
            		if (arrayVal == null) {
            			continue;
            		}
            	
            		String elementName = fieldName + "[" + i + "]";
            		if (Utils.shouldExcludeElement(elementName, arr.type(), arrayVal.type())) {
            			continue;
            		}
            		
                	VariableNode element = captureState(arrayVal, elementName, arrayVal.type().name(), arrnode, depth + 1, maxDepth, includeInherited);
                	if (element != null) {
                		arrnode.addChild(element);
                	}
                }
                return arrnode;
            } 
        	// must come after StringReference and ArrayReference
            else if (obj instanceof ObjectReference) { 
            	// here we have a normal class object
            	ObjectReference objectRef = (ObjectReference) obj;
            	
            	VariableNode varnode = new VariableNode(fieldName, objectRef.type().name(), objectRef, parent);
//            	if (objectRef.type().name().contains("android.text.SpannableString"))
        		varnode.obtainStringifiedValue(objectRef, threadRef);
            	this.objectToNodeMap.put(obj, varnode);
                
                // capture the fields of this object
                List<Field> fields = objectRef.referenceType().fields(); // .allFields();  //includeInherited ? obj.referenceType().visibleFields() : obj.referenceType().fields();
                for (Field field : fields) {
                	Value childValue = objectRef.getValue(field);  	
                	
                	if (childValue != null) {
                		if (Utils.shouldExcludeField(field, childValue.type())) {
                			continue;
                		}
	                	VariableNode child = captureState(childValue, field.name(), field.typeName(), varnode, depth + 1, maxDepth, includeInherited);
	                	if (child != null) {
	                		varnode.addChild(child);
	                	}
                	}
                }
                return varnode;
            }
            else {
            	// unsupported Value type
            	if (obj != null)
            		System.err.println("Unsupported Value type: " + obj.getClass() + ", runtime type = " + obj.type().name() + "\n");
            }
        }
        else {
        	// depth limit exceeded
        	return null;
        }
        return null;
    }

   
    
    public void dump() {
    	DumpingVisitor dumper = new DumpingVisitor();
    	dumper.visitTree(this.rootObject);
    }
    
    
    public void visualize() {
    	String windowTitle = this.entry.mthd.name() + " " + this.entry.type.toString() + 
    						 "(" + this.entry.mthd.declaringType() + ")";
    	SwingTree treeView = new SwingTree(this.rootObject, windowTitle);
    }
    
    
	@Override
	public boolean equals(Object obj) {
		
		// TODO: fix this to traverse the whole tree of objects
		
		if (obj == null)
			return false; 
		
		if (obj instanceof CapturedState) {
			CapturedState other = (CapturedState)obj;
//			
//			for (String f : this.values.keySet()) 
//			{
//				Value thisVal = this.values.get(f);
//				Value otherVal = other.values.get(f);
//				
//				if (thisVal == null && otherVal == null) {
//					continue;
//				}
//				else if (thisVal == null || otherVal == null) {
//					return false;
//				}
//				
//				if (thisVal.equals(otherVal))
//				{
//					continue;
//				}
//				else {
//					return false;
//				}
//			}
			
		}
		else {
			return false;
		}
		
		return true;
	}
	
	
	public VariableNode getRootObject() {
		return this.rootObject;
	}
	
	public ThreadReference getThreadRef() {
		return threadRef;
	}
	
	public Method getMethod() {
		return entry.mthd;
	}

	public BreakpointType getType() {
		return entry.type;
	}
}

