package objectnodes;

import java.util.List;

import javax.swing.tree.MutableTreeNode;

import com.sun.jdi.ArrayReference;
import com.sun.jdi.Field;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.PrimitiveValue;
import com.sun.jdi.StringReference;
import com.sun.jdi.Value;

public class DumpingVisitor extends TreeVisitorBase {

	@Override
	public void takeActionAtNode(VariableNode node, int depth) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i <= depth; i++) {
			sb.append("  ");
		}
		sb.append(node.getTypeName() + "  " + node.getName() + " = " + node.getStringValue());
		System.out.println(sb.toString());
	}

	
	
}
