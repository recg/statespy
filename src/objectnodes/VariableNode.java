/*
 * Copyright (C) 2012 Martin Leopold <m@martinleopold.com>
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */

/*
 * Taken from https://github.com/processing/processing-experimental/blob/master/src/processing/mode/experimental/VariableNode.java
 * Modified by Kevin Boos, August 15th, 2016
 */

package objectnodes;

import com.sun.jdi.ArrayReference;
import com.sun.jdi.IntegerValue;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.PrimitiveValue;
import com.sun.jdi.StringReference;
import com.sun.jdi.Value;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;

import org.javers.core.metamodel.annotation.DiffIgnore;
import org.javers.core.metamodel.annotation.Id;
import org.javers.core.metamodel.annotation.ValueObject;

/**
 * Model for a variable in the variable inspector. Has a type and name and
 * optionally a value. Can have sub-variables (as is the case for objects, and
 * arrays).
 *
 * @author Martin Leopold <m@martinleopold.com>
 */
public class VariableNode implements MutableTreeNode {

	public static final int TYPE_UNKNOWN = -1;
	public static final int TYPE_OBJECT = 0;
	public static final int TYPE_ARRAY = 1;
	public static final int TYPE_INTEGER = 2;
	public static final int TYPE_FLOAT = 3;
	public static final int TYPE_BOOLEAN = 4;
	public static final int TYPE_CHAR = 5;
	public static final int TYPE_STRING = 6;
	public static final int TYPE_LONG = 7;
	public static final int TYPE_DOUBLE = 8;
	public static final int TYPE_BYTE = 9;
	public static final int TYPE_SHORT = 10;
	public static final int TYPE_VOID = 11;

	protected String type;
	protected String name;

	protected Value value;
	protected List<VariableNode> children = new ArrayList<>();
	protected VariableNode parent;

	/**
	 * Construct a {@link VariableNode}.
	 * 
	 * @param name
	 *            the name
	 * @param type
	 *            the type
	 * @param value
	 *            the value
	 */
	public VariableNode(String name, String type, Value value, VariableNode parent) {
		this.name = name;
		this.type = type;
		this.value = value;
		this.parent = parent;
	}

	public void setValue(Value value) {
		this.value = value;
	}

	public Value getValue() {
		return value;
	}

	/**
	 * Get a String representation of this variable nodes value.
	 *
	 * @return a String representing the value.
	 */
	public String getValueAsString() {

		if (this.value instanceof PrimitiveValue) {
			return ((PrimitiveValue) this.value).toString();
		}

		if (this.value instanceof StringReference) {
			return "\"" + ((StringReference) this.value).value() + "\"";
		}

		if (this.value instanceof ArrayReference) {
			ArrayReference arrRef = (ArrayReference) this.value;
			if (this.getChildCount() != arrRef.length()) {
				System.err.println("childCount(" + this.getChildCount() + ") should equal arrRef length (" + arrRef.length() + ")!!");
			}
			return this.type + "[" + this.getChildCount() + "]";
		}

		if (this.value instanceof ObjectReference) {
			return "instance of " + this.type;
		}

		//
		// String str;
		// if (value != null) {
		// if (getType() == TYPE_OBJECT) {
		// str = "instance of " + type;
		// } else if (getType() == TYPE_ARRAY) {
		// //instance of int[5] (id=998) --> instance of int[5]
		// try {
		// str = value.toString().substring(0, value.toString().lastIndexOf("
		// "));
		// } catch (StringIndexOutOfBoundsException ex) {
		// ex.printStackTrace();
		// return null;
		// }
		// } else if (getType() == TYPE_STRING) {
		// str = ((StringReference) value).value(); // use original string value
		// (without quotes)
		// } else {
		// str = value.toString();
		// }
		// } else {
		// str = "null";
		// }
		return "error getting value";
	}

	public String getTypeString() {
		return type;
	}

	// public int getType() {
	// if (type == null) {
	// return TYPE_UNKNOWN;
	// }
	// if (this.value instanceof ArrayReference) {
	// return TYPE_ARRAY;
	// }
	// if (this.value instanceof StringReference) {
	// return TYPE_STRING;
	// }
	// if (this.value instanceof IntegerValue) {
	// return TYPE_INTEGER;
	// }
	// if (type.equals("long")) {
	// return TYPE_LONG;
	// }
	// if (type.equals("byte")) {
	// return TYPE_BYTE;
	// }
	// if (type.equals("short")) {
	// return TYPE_SHORT;
	// }
	// if (type.equals("float")) {
	// return TYPE_FLOAT;
	// }
	// if (type.equals("double")) {
	// return TYPE_DOUBLE;
	// }
	// if (type.equals("char")) {
	// return TYPE_CHAR;
	// }
	// if (type.equals("java.lang.String")) {
	// return TYPE_STRING;
	// }
	// if (type.equals("boolean")) {
	// return TYPE_BOOLEAN;
	// }
	// if (type.equals("void")) {
	// return TYPE_VOID; //TODO: check if this is correct
	// }
	// return TYPE_OBJECT;
	// }

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Add a {@link VariableNode} as child.
	 *
	 * @param c
	 *            the {@link VariableNode} to add.
	 */
	public void addChild(VariableNode c) {
		if (children == null)
			throw new NullPointerException("trying to add child " + c + " to a null children list");
		children.add(c);
		if (c != null)
			c.setParent(this);
	}

	/**
	 * Add multiple {@link VariableNode}s as children.
	 *
	 * @param children
	 *            the list of {@link VariableNode}s to add.
	 */
	public void addChildren(List<VariableNode> children) {
		for (VariableNode child : children) {
			addChild(child);
		}
	}

	@Override
	public TreeNode getChildAt(int i) {
		if (children == null)
			return null;
		return children.get(i);
	}

	@Override
	public int getChildCount() {
		if (children == null)
			return 0;
		return children.size();
	}

	@Override
	public TreeNode getParent() {
		return parent;
	}

	@Override
	public int getIndex(TreeNode tn) {
		if (children == null)
			return -1;
		return children.indexOf(tn);
	}

	@Override
	public boolean getAllowsChildren() {
		if (value == null) {
			return false;
		}

		// handle strings
		if ((value instanceof StringReference) || (value instanceof PrimitiveValue)) {
			return false;
		}

		if ((value instanceof ArrayReference) || (value instanceof ObjectReference)) {
			return true;
		}

		return true; // default: don't disable children unnecessarily
	}

	/**
	 * This controls the default icon and disclosure triangle.
	 *
	 * @return true, will show "folder" icon and disclosure triangle.
	 */
	@Override
	public boolean isLeaf() {
		if (children == null)
			return true;
		return children.size() == 0;
	}

	@Override
	public Enumeration children() {
		return Collections.enumeration(children);
	}

	/**
	 * Get a String representation of this {@link VariableNode}.
	 *
	 * @return the name of the variable (for sorting to work).
	 */
	@Override
	public String toString() {
		return getName(); // for sorting
	}

	/**
	 * Get a String description of this {@link VariableNode}. Contains the type,
	 * name and value.
	 *
	 * @return the description
	 */
	public String getDescription() {
		String str = "";
		if (type != null) {
			str += type + " ";
		}
		str += name;
		str += " = " + getValueAsString();
		return str;
	}

	@Override
	public void insert(MutableTreeNode mtn, int i) {
		if (children == null)
			throw new NullPointerException(
					"trying to insert child " + mtn + " at index " + i + " into a null children list");
		children.add(i, this);
	}

	@Override
	public void remove(int i) {
		if (children == null)
			throw new NullPointerException("trying to remove child at index " + i + " from a null children list");
		MutableTreeNode mtn = children.remove(i);
		if (mtn != null) {
			mtn.setParent(null);
		}
	}

	@Override
	public void remove(MutableTreeNode mtn) {
		if (children == null)
			throw new NullPointerException("trying to remove child " + mtn + " from a null children list");
		children.remove(mtn);
		mtn.setParent(null);
	}

	/**
	 * Remove all children from this {@link VariableNode}.
	 */
	public void removeAllChildren() {
		if (children == null)
			return;

		for (VariableNode vn : children) {
			vn.setParent(null);
		}
		children.clear();
	}

	@Override
	public void setUserObject(Object o) {
		if (o instanceof Value) {
			value = (Value) o;
		}
	}

	@Override
	public void removeFromParent() {
		if (parent == null)
			return;
		parent.remove(this);
		this.parent = null;
	}

	@Override
	public void setParent(MutableTreeNode mtn) {
		if (mtn instanceof VariableNode) {
			parent = (VariableNode) mtn;
		} else {
			System.err.println("Error: tried to setParent of this=" + this + " to parent mtn=" + mtn);
		}
	}

	public List<VariableNode> getChildren() {
		return children;
	}

	/**
	 * Test for equality. To be equal, two {@link VariableNode}s need to have
	 * equal type, name and value.
	 *
	 * @param obj
	 *            the object to test for equality with this {@link VariableNode}
	 * @return true if the given object is equal to this {@link VariableNode}
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final VariableNode other = (VariableNode) obj;
		if ((this.type == null) ? (other.type != null) : !this.type.equals(other.type)) {
			// System.out.println("type not equal");
			return false;
		}
		if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
			// System.out.println("name not equal");
			return false;
		}
		if (this.value != other.value && (this.value == null || !this.value.equals(other.value))) {
			// System.out.println("value not equal");
			return false;
		}
		// if (this.parent != other.parent && (this.parent == null ||
		// !this.parent.equals(other.parent))) {
		// System.out.println("parent not equal: " + this.parent + "/" +
		// other.parent);
		// return false;
		// }
		return true;
	}

	/**
	 * Returns a hash code based on type, name and value.
	 */
	@Override
	public int hashCode() {
		int hash = 3;
		hash = 97 * hash + (this.type != null ? this.type.hashCode() : 0);
		hash = 97 * hash + (this.name != null ? this.name.hashCode() : 0);
		hash = 97 * hash + (this.value != null ? this.value.hashCode() : 0);
		// hash = 97 * hash + (this.parent != null ? this.parent.hashCode() :
		// 0);
		return hash;
	}

}
