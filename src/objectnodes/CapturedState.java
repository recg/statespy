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

public class CapturedState {

	/**
	 * the top-level parent object contained within this CapturedState. 
	 */
	VariableNode parentObject;
	
	/**
	 * The fields already captured. Used to avoid infinite circular recursion,
	 * which happens when there are circular references 
	 */
	HashSet<Value> objectsCaptured = new HashSet<Value>();

	
	public CapturedState() { }
	
	public CapturedState(ObjectReference obj) {
		this();
		captureObjectState(obj);
	}

	
	public void captureObjectState(Value object) {
		parentObject = captureState(object, "top_level", object.type().name(), null, 0, 5, true);
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
        	if (this.objectsCaptured.contains(obj))
				return null;
        	this.objectsCaptured.add(obj);
        	
        	
        	if (obj instanceof PrimitiveValue) {
        		return new PrimitiveNode(fieldName, fieldType, (PrimitiveValue)obj, parent);
        	}
        	else if (obj instanceof StringReference) {
        		return new StringNode(fieldName, fieldType, (StringReference)obj, parent);
        	}
        	else if (obj instanceof ArrayReference) {
                return captureArray(fieldName, (ArrayReference)obj, parent, depth, maxDepth, includeInherited);
            } 
            else if (obj instanceof ObjectReference) { // must come after StringReference and ArrayReference
            	// here we have a normal class object
            	ObjectReference objectRef = (ObjectReference) obj;
            	
            	VariableNode varnode = new VariableNode(fieldName, objectRef.type().name(), objectRef, parent);

                
                // capture the fields of this object
                List<Field> fields = objectRef.referenceType().fields(); // .allFields();  //includeInherited ? obj.referenceType().visibleFields() : obj.referenceType().fields();
                for (Field field : fields) {
                	if (shouldExcludeField(field)) {
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
        return null;
    }


    /**
     * Get the fields of an array for insertion into a {@link JTree}.
     *
     * @param array the array reference
     * @return list of array fields
     */
    protected VariableNode captureArray(String fieldName, ArrayReference arr, VariableNode parent, int depth, int maxDepth, boolean includeInherited) {
        ArrayNode arrnode = new ArrayNode(fieldName, arr.type().toString(), arr, parent);
    	for (int i = 0; i < arr.length(); i++) {
        	arrnode.contents.add(captureState(arr.getValue(i), fieldName, arr.type().name(), arrnode, depth + 1, maxDepth, includeInherited));
        }
        return arrnode;
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
	

	private boolean shouldExcludeField(Field f) {
		// ignore unmodifiable variables
		try {
			return (f.isEnumConstant() || 
					(f.isFinal() && (f.type() instanceof PrimitiveType)) || //only ignore final primitives, not final Objects (a final arraylist can still be modified)  
					f.name().equals("shadow$_klass_") || 
					f.name().equals("shadow$_monitor_")
					);
		} catch (ClassNotLoadedException e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
}

