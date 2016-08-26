package kevin;

import com.sun.jdi.Method;
import com.sun.jdi.request.EventRequest;

public class BreakpointEntry {

	public EventRequest req;
	public BreakpointType type;
	public Method mthd;
	
	public BreakpointEntry(EventRequest b, BreakpointType t, Method m) {
		this.req = b;
		this.type = t;
		this.mthd = m;
	}
}
