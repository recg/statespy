package objectnodes;

import com.sun.jdi.Value;

import kevin.Utils;

public class PrimitiveNode extends VariableNode {

	Object primitiveValue;
	
	
	public PrimitiveNode(String name, String type, Value value, VariableNode parent) {
		super(name, type, value, parent);
		this.children.clear();
		setPrimitiveValue(value);
	}
	

	public void setPrimitiveValue(Value value) {
		this.primitiveValue = Utils.getPrimitiveFromValue(value);
	}
	
}
