package kevin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.jdi.Field;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.Value;

public class CapturedState {

	/**
	 * maps field name to the value of that field

	 */
	Map<String, Value> values;

	
	public CapturedState() {
		values = new HashMap<String, Value>();
	}
	
	public CapturedState(List<Field> fields, ObjectReference currentThis) {
		this();
		addFieldValues(fields, currentThis);
	}

	
	public void addFieldValues(List<Field> fields, ObjectReference currentThis) {
		for (Field f : fields)
		{
			// ignore unmodifiable variables	
			if (f.isEnumConstant() || f.isFinal() || f.name().equals("shadow$_klass_") || f.name().equals("shadow$_monitor_")) {
				continue;
			}
			
			values.put(f.name(), currentThis.getValue(f));
		}
	}
	
	
	// TOOD: write this recursive routine that descends into each object's structure and deep copies them
	public void captureObjectState(Value object, ObjectReference currentThis) {
		
	}
	
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false; 
		
		if (obj instanceof CapturedState) {
			CapturedState other = (CapturedState)obj;
			
			for (String f : this.values.keySet()) 
			{
				Value thisVal = this.values.get(f);
				Value otherVal = other.values.get(f);
				
				if (thisVal == null && otherVal == null) {
					continue;
				}
				else if (thisVal == null || otherVal == null) {
					return false;
				}
				
				if (thisVal.equals(otherVal))
				{
					continue;
				}
				else {
					return false;
				}
			}
			
		}
		else {
			return false;
		}
		
		return true;
	}

}
