package objectnodes;

import java.util.HashSet;
import java.util.List;

import com.sun.crypto.provider.AESParameters;
import com.sun.jdi.ArrayReference;
import com.sun.jdi.Field;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.PrimitiveValue;
import com.sun.jdi.StringReference;
import com.sun.jdi.Value;

public class TreeVisitorBase {

	private HashSet<VariableNode> nodesVisited = new HashSet<VariableNode>();

	public void visitTree(VariableNode root) {
		visitNode(root, null, 0, 5);
	}

	/**
	 * the main recursive routine 
	 * @param node
	 * @param parent
	 * @param depth
	 * @param maxDepth
	 */
	public void visitNode(VariableNode node, VariableNode parent, int depth, int maxDepth) {

		if (depth <= maxDepth) {

			// avoid circular references by stopping the chain here
			if (this.nodesVisited.contains(node))
				return;

			this.nodesVisited.add(node);


			if ((node instanceof PrimitiveNode) || (node instanceof StringNode)) {
				this.nodesVisited.add(node);
				takeActionAtNode(node, depth);
				return;
			}
			else if ((node instanceof ArrayNode) || (node instanceof VariableNode)) {
				this.nodesVisited.add(node);
				takeActionAtNode(node, depth);
				for (int i = 0; i < node.getChildCount(); i++) {
					visitNode((VariableNode)(node.getChildAt(i)), node, depth + 1, maxDepth);
				}
				return;
			} 
			else {
				// unsupported Value type
				if (node != null)
					System.err.println("Unsupported node type: " + node.getClass() + ", name = " + node.name + "\n");
			}
		}
		else {
			// depth limit exceeded
			return;
		}

		return;
	}


	public void takeActionAtNode(VariableNode node, int depth) {
		System.out.println("At node " + node);
	}
}
