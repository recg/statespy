package objectnodes;

import com.sun.jdi.StringReference;

public class StringNode extends VariableNode {
	
	String stringValue;

	public StringNode(String name, String type, StringReference ref, VariableNode parent) {
		super(name, type, ref, parent);
		this.children = null;
		this.stringValue = ref.value();
	}
	
}
