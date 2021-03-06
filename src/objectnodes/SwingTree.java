package objectnodes;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;


// a good JTree resource: http://www.informit.com/articles/article.aspx?p=26327&seqNum=13
// simple example: http://docstore.mik.ua/orelly/java-ent/jfc/ch03_19.htm

public class SwingTree extends JFrame {
	
	JTree tree;
	JScrollPane scrollPane;
	JTextField bottomTextField = new JTextField();

	Renderer renderer = new Renderer();


	public SwingTree(MutableTreeNode root, String windowTitle) {
		tree = new JTree(root);

		tree.putClientProperty("JTree.lineStyle", "Angled");
		getContentPane().setLayout(new BorderLayout());
		tree.setCellRenderer(renderer);
		tree.addTreeSelectionListener(new TreeHandler());
		scrollPane = new JScrollPane(tree);
		getContentPane().add("Center", scrollPane);
		getContentPane().add("South", bottomTextField);

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		setSize(800, 500);
		this.setTitle(windowTitle);
		setVisible(true);
	}

	public class TreeHandler implements TreeSelectionListener {
		public void valueChanged(TreeSelectionEvent e) {
			TreePath path = e.getPath();
			Object component = path.getPathComponent(path.getPathCount() - 1);
			
			if (component instanceof VariableNode) {
				VariableNode node = (VariableNode)component;
				bottomTextField.setText(node.getValueAsString());
			}
			else {
				bottomTextField.setText("Node is not a VariableNode, couldn't get value.");
			}
		}
	}
}

class Renderer extends JLabel implements TreeCellRenderer {
	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected,
			boolean expanded, boolean leaf, int row, boolean hasFocus) 
	{
		if (value == null) {
			setText("null");
			return this;
		}
		
		if (value instanceof VariableNode) {
			VariableNode node = (VariableNode)value;
			setText(node.getName()+ "      (" + node.getTypeString() + ")");
		}
		return this;
	}
}