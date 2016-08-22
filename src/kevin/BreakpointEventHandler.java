package kevin;

import java.util.ArrayList;
import java.util.List;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Field;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventIterator;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.request.BreakpointRequest;

import objectnodes.CapturedState;



public class BreakpointEventHandler extends Thread {

	ArrayList<BreakpointEntry> breakpoints = new ArrayList<BreakpointEntry>();
	private VirtualMachine vm;
	private boolean connected = true; // are we connected to the vm?
	
	ArrayList<CapturedState> capturedStates = new ArrayList<>();
	CapturedState beginState, endState;


	public BreakpointEventHandler(VirtualMachine vm) { 
		this.vm = vm;
	}

	/**
	 * 
	 * @param loc the source code location to set the breakpoint at
	 * @param type BreakpointType, entry or exit
	 * @throws AbsentInformationException 
	 */
	public void addBreakpointAtMethod(Method m, BreakpointType type, boolean enable) throws AbsentInformationException {
		
		Location loc = null;
		if (type.equals(BreakpointType.ENTRY)) {
			loc = m.location();
		}
		else if (type.equals(BreakpointType.EXIT)) {
			loc = Utils.getEndOfMethodLocation(m);
		}
		else {
			System.err.println("addBreakpoint(): Unsupported BreakpointType = " + type);
			return;
		}
				
		BreakpointRequest b = vm.eventRequestManager().createBreakpointRequest(loc);
		b.setSuspendPolicy(BreakpointRequest.SUSPEND_ALL);
		if (enable) {
			b.enable();
		}
		this.breakpoints.add(new BreakpointEntry(b, type, m));
	}



	public void run() {

		EventQueue evtQueue = vm.eventQueue();
		while (connected)
		{
			EventSet evtSet;
			try {
				evtSet = evtQueue.remove();
			} catch (InterruptedException e) {
				System.err.println("evtQueue.remove() failed!");
				e.printStackTrace();
				return;
			}
			EventIterator evtIter = evtSet.eventIterator();
			while (evtIter.hasNext())
			{
				try
				{
					Event evt = evtIter.next();
					if (evt instanceof BreakpointEvent)
					{
						try {
							handleBreakpointEvent((BreakpointEvent)evt);
						} catch (IncompatibleThreadStateException e) {
							e.printStackTrace();
						}
					}

				}

				//				catch (Exception exc)
				//				{
				//					System.out.println(exc.getClass().getName() + ": " + exc.getMessage());
				//				}

				finally
				{
					// restart the thread after the breakpoint stopped it
					evtSet.resume();
				}
			}

		}

	}


	private void handleBreakpointEvent(BreakpointEvent evt) throws IncompatibleThreadStateException {
		BreakpointRequest bpReq = (BreakpointRequest)evt.request();
	
		// ensure that this breakpoint is one we actually care about and have registered
		BreakpointEntry be = null;
		for (BreakpointEntry b : this.breakpoints) {
			if (b.bReq.equals(bpReq)) {
				be = b;
				break;
			}
		}
		if (be == null)
			return; 
					
		
		
		System.out.println("=======================================================================");
		System.out.println("================ Breakpoint at line " + bpReq.location().lineNumber() + "  (" + bpReq.location().method().name() + ") ================");

		ThreadReference threadRef = evt.thread();
		StackFrame stackFrame = threadRef.frame(0);
		ObjectReference currentThis = stackFrame.thisObject(); // dynamic reference to "this" current instance object

		// get call stack
		System.out.println("    --- Callstack --- ");
		for (int i = threadRef.frameCount() - 1; i >= 0; i--) 
		{
			StackFrame sf = threadRef.frame(i);
			try {
				System.out.println("        " + sf.location().declaringType().name() + " : " + sf.location().method().name() + "  (" + sf.location().sourceName() + " : " + sf.location().lineNumber() + ")");
			} catch (AbsentInformationException e) {
				System.err.println("AbsentInformationException: did you compile your target application with -g option?");
				e.printStackTrace();
			}
		}
		System.out.println("\n");


		/*
		 * To find out where the binder call came from, we can invoke the following static Binder methods
		 *   Binder.getCallingPid()
		 *   Binder.getCallingUid()
		 *   Binder.getCallingUserHandle()
		 */


		ReferenceType ref = evt.location().declaringType(); // static reference to enclosing class
		for (Field f : ref.allFields())
		{
			// ignore unmodifiable variables
			if (Utils.shouldExcludeField(f)) {
				continue;
			}

			System.out.println("       " + (f.isStatic() ? "static " : "") + f.typeName() + "  " + f.name() + " = " + Utils.getValueAsString(currentThis.getValue(f)) + "\n             [in " + f.declaringType() + "]\n");

		}
		
		CapturedState capState = new CapturedState(threadRef, currentThis, be);
		this.capturedStates.add(capState);	
		capState.dump();

		if (be.type.equals(BreakpointType.EXIT)) {
			CapturedState beginState = findMatchingBeginState(capState);
			compareStates(beginState, endState);
		}

		//TODO:  write routine to capture/save the state of a class object, which recursively iterates through all of the subobjects 
		// TODO:  use that routine to capture the state of a whole service, which may span multiple methods that touch different objects. 
		//        all of those objects should be included\
		// TODO: so, given a starting point "a" below, we should capture "a", "b", and "c" states as the before states, and then c', b', and a' as the after states.
		//		 we should be able to tell when entering and exiting a method, hopefully don't have to step through individual statements, that will kill performance
		/*
		 *  a
		 *   \
		 *    b
		 *     \
		 *      c
		 *      |
		 *      c' 
		 *     /
		 *    b'
		 *   /  
		 *  a'
		 */
		// TODO: next, once all the states are captured, we need to write comparison routines for states. It seems like the .equals() methods work as we want them to.



		// prints local stack variables
		//			List<LocalVariable> visVars = stackFrame.visibleVariables();
		//			for (LocalVariable visibleVar : visVars)
		//			{
		//				Value val = stackFrame.getValue(visibleVar);
		//				System.out.print(visibleVar.name() + " = " + Utils.getValueAsString(val));	
		//			}


	}
	
	
	/**
	 * returns the beginning state (at method entry) that matches the given endState
	 * @param endState the ending state (at method exit) to match
	 * @return
	 */
	private CapturedState findMatchingBeginState(CapturedState endState) {
		// start from the end of the list to find the most recent matching one first
		for (int i = this.capturedStates.size() - 1; i >= 0; i--) 
		{
			CapturedState curr = this.capturedStates.get(i);
			if (endState.getMethod().equals(curr.getMethod()) &&
				endState.getThreadRef().equals(curr.getThreadRef()) && 
				endState.getType().equals(curr.getType()))
			{
				return curr;
			}
		}
		
		System.err.println("Couldn't find a matching beginState for endState = " + endState);
		return null;
	}
	
	
	
	public void compareStates(CapturedState beg, CapturedState end) {
		// TODO write this state comparison routine
	}
}
