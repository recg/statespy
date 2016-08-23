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


public class SwingTree extends JFrame {
	
	JTree tree;
	JScrollPane scrollPane;
	JTextField textField = new JTextField();

	Renderer renderer = new Renderer();


	public SwingTree(MutableTreeNode root) {
		tree = new JTree(root);

		tree.putClientProperty("JTree.lineStyle", "Angled");
		getContentPane().setLayout(new BorderLayout());
		tree.setCellRenderer(renderer);
		tree.addTreeSelectionListener(new TreeHandler());
		scrollPane = new JScrollPane(tree);
		getContentPane().add("Center", scrollPane);
		getContentPane().add("South", textField);

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		setSize(500, 500);
		setVisible(true);
	}

	public class TreeHandler implements TreeSelectionListener {
		public void valueChanged(TreeSelectionEvent e) {
			TreePath path = e.getPath();
			String text = path.getPathComponent(path.getPathCount() - 1).toString();
			if (path.getPathCount() > 3) {
				text += ": ";
				text += Integer.toString((int) (Math.random() * 50)) + " Wins ";
				text += Integer.toString((int) (Math.random() * 50)) + " Losses";
			}
			textField.setText(text);
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
		
		setText(value.toString());
		return this;
	}
}