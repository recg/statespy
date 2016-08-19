package objectnodes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.sun.jdi.ArrayReference;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.Field;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.PrimitiveType;
import com.sun.jdi.PrimitiveValue;
import com.sun.jdi.StackFrame;
import com.sun.jdi.StringReference;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;

import kevin.Utils;

public class CapturedState {

	/**
	 * the top-level root object of the tree contained within this CapturedState. 
	 */
	VariableNode rootObject;
	
	/**
	 * The objects already captured. Used to avoid infinite recursion,
	 * which happens when there are circular references 
	 */
	HashMap<Value, VariableNode> objectToNodeMap = new HashMap<>();

	
	public CapturedState() { }
	
	public CapturedState(ObjectReference obj) {
		this();
		captureObjectState(obj);
	}

	
	public void captureObjectState(Value object) {
		rootObject = captureState(object, "top_level", object.type().name(), null, 0, 10, true);
	}
	
	
    /**
     * Recursively get the fields of a {@link Value} for insertion into a
     * {@link JTree}.
     *
     * @param value must be an instance of {@link ObjectReference}
     * @param depth the current depth
     * @param maxDepth the depth to stop at (inclusive)
     * @return list of child fields of the given value
     */
    private VariableNode captureState(Value obj, String fieldName, String fieldType, VariableNode parent, int depth, int maxDepth, boolean includeInherited) {
        
        if (depth <= maxDepth) {
        	
        	// avoid circular references by stopping the chain here
        	VariableNode redundant = this.objectToNodeMap.get(obj);
        	if (redundant != null)
        		return redundant;
        	
        	
        	if (obj instanceof PrimitiveValue) {
        		PrimitiveNode node = new PrimitiveNode(fieldName, fieldType, (PrimitiveValue)obj, parent);
        		this.objectToNodeMap.put(obj, node);
        		return node;
        	}
        	else if (obj instanceof StringReference) {
        		StringNode node = new StringNode(fieldName, fieldType, (StringReference)obj, parent);
        		this.objectToNodeMap.put(obj, node);
        		return node;
        	}
        	else if (obj instanceof ArrayReference) {
        		ArrayReference arr = (ArrayReference)obj;
                ArrayNode arrnode = new ArrayNode(fieldName, arr.type().toString(), arr, parent);
                this.objectToNodeMap.put(arr, arrnode);
            	for (int i = 0; i < arr.length(); i++) {
                	arrnode.addChild(captureState(arr.getValue(i), fieldName, arr.type().name(), arrnode, depth + 1, maxDepth, includeInherited));
                }
                return arrnode;
            } 
            else if (obj instanceof ObjectReference) { // must come after StringReference and ArrayReference
            	// here we have a normal class object
            	ObjectReference objectRef = (ObjectReference) obj;
            	
            	VariableNode varnode = new VariableNode(fieldName, objectRef.type().name(), objectRef, parent);
            	this.objectToNodeMap.put(obj, varnode);
                
                // capture the fields of this object
                List<Field> fields = objectRef.referenceType().fields(); // .allFields();  //includeInherited ? obj.referenceType().visibleFields() : obj.referenceType().fields();
                for (Field field : fields) {
                	if (Utils.shouldExcludeField(field)) {
                		continue;
                	}
                	
                	Value childValue = objectRef.getValue(field);  	
                	if (childValue != null) {
	                	VariableNode child = captureState(childValue, field.name(), field.typeName(), varnode, depth + 1, maxDepth, includeInherited);
	                    varnode.addChild(child);
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
	
}

