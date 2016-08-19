package objectnodes;

public class DumpingVisitor extends TreeVisitorBase {

	@Override
	public void takeActionAtNode(VariableNode node, int depth) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i <= depth; i++) {
			sb.append("  ");
		}
		sb.append(node.getTypeString() + "  " + node.getName() + " = " + node.getValueAsString());
		System.out.println(sb.toString());
	}

	
	
}
