package objectnodes;

import com.sun.jdi.ArrayReference;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.InvalidTypeException;
import com.sun.jdi.Value;

import sun.awt.VariableGridLayout;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;


public class ArrayNode extends VariableNode {

    protected ArrayReference arrayObj;

    /**
     * Construct an {@link ArrayNode}.
     * There is one ArrayNode per JDI ArrayReference, and it contains a list of the array's contents. 
     *
     * @param name the name
     * @param type the type
     * @param value the value
     * @param arr a reference to the array
     * @param index the index inside the array
     */
    public ArrayNode(String name, String type, ArrayReference arr, VariableNode parent) {
        super(name, type, arr, parent);
        this.arrayObj = arr;
    }

}