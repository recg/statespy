package kevin;

import com.sun.jdi.Method;
import com.sun.jdi.request.BreakpointRequest;

public class BreakpointEntry {

	public BreakpointRequest bReq;
	public BreakpointType type;
	public Method mthd;
	
	public BreakpointEntry(BreakpointRequest b, BreakpointType t, Method m) {
		this.bReq = b;
		this.type = t;
		this.mthd = m;
	}
}
