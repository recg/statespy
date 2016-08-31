package kevin;

import java.util.ArrayList;
import java.util.List;

import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.Diff;
import org.javers.core.diff.ListCompareAlgorithm;

import com.cedarsoftware.util.GraphComparator;
import com.cedarsoftware.util.GraphComparator.Delta;
import com.cedarsoftware.util.GraphComparator.ID;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Field;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventIterator;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.MethodExitEvent;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.MethodExitRequest;

import de.danielbechler.diff.ObjectDiffer;
import de.danielbechler.diff.ObjectDifferBuilder;
import de.danielbechler.diff.node.DiffNode;
import de.danielbechler.diff.node.Visit;
import objectnodes.CapturedState;
import objectnodes.VariableNode;



public class BreakpointEventHandler extends Thread {

	ArrayList<BreakpointEntry> breakpoints = new ArrayList<BreakpointEntry>();
	private VirtualMachine vm;
	private boolean connected = true; // are we connected to the vm?
	
	ArrayList<CapturedState> capturedStates = new ArrayList<>();


	public BreakpointEventHandler(VirtualMachine vm) { 
		this.vm = vm;
	}

	/**
	 * 
	 * @param loc the source code location to set the breakpoint at
	 * @param type BreakpointType, only "entry" is currently used
	 * @throws AbsentInformationException 
	 */
	public void addBreakpointAtMethod(Method m, BreakpointType type, boolean enable) throws AbsentInformationException {
		
		Location loc = null;
		if (type.equals(BreakpointType.ENTRY)) {
			loc = m.location();
		}
		else if (type.equals(BreakpointType.EXIT)) {
			System.err.println("EXIT BreakpointTypes are deprecated! They don't work.");
			System.exit(-2);
//			loc = Utils.getEndOfMethodLocation(m);
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
		
		System.out.println("Set " + type + " breakpoint at method " + m + ", " + loc);
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
					try {
						if (evt instanceof BreakpointEvent) {
							handleBreakpointEvent((BreakpointEvent)evt);
						}
						else if (evt instanceof MethodExitEvent) {
							handleMethodExitEvent((MethodExitEvent)evt);
						}
					}
					catch (IncompatibleThreadStateException e) {
						e.printStackTrace();
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
			if (b.req.equals(bpReq)) {
				be = b;
				break;
			}
		}
		if (be == null)
			return; 
				
		if (!be.type.equals(BreakpointType.ENTRY)) {
			System.err.println("Error: handleBreakpointEvent() is currently only intended for ENTRY breakpoints, not " + be.type + " breakpoints.");
			System.exit(-2);
		}
		
		ThreadReference threadRef = evt.thread();
		StackFrame stackFrame = threadRef.frame(0);
		ObjectReference currentThis = stackFrame.thisObject(); // dynamic reference to "this" current instance object
		Method currentMethod = bpReq.location().method();
		
		System.out.println("\n=======================================================================");
		System.out.println("============ " + be.type + " Breakpoint at line " + bpReq.location().lineNumber() + "  (" + currentMethod.declaringType() + "." + currentMethod.name() + ") ================");
		System.out.println("=======================================================================");
		

		
		/*
		 *  Instead of setting "exit" breakpoints on the last line of a method, which doesn't work,
		 *  we create a method exit request so that when the current method ends, we'll receive the proper event.
		 *  Note that we want to restrict it to this thread so that nothing else triggers it.
		 */
		// first, search for an existing MethodExitRequest so we can re-enable it (if we're already keeping track of it)
		BreakpointEntry mthdExit = null;
		for (BreakpointEntry e : this.breakpoints) {
			if (e.mthd.equals(currentMethod) && 
				e.type.equals(BreakpointType.EXIT)) {
				mthdExit = e;
				break;
			}
		}
		if (mthdExit == null)
			this.breakpoints.add(new BreakpointEntry(null, BreakpointType.EXIT, currentMethod));
		
		MethodExitRequest mer = this.vm.eventRequestManager().createMethodExitRequest();
		mer.addThreadFilter(threadRef);
		mer.addInstanceFilter(currentThis);
		// could also add referencetype filter just for this class
		mer.setSuspendPolicy(BreakpointRequest.SUSPEND_ALL);
		mer.enable();
		System.out.println("Set EXIT breakpoint at method " + currentMethod.name());
		
		
		Utils.dumpCallStack(threadRef);


		/*
		 * To find out where the binder call came from, we can invoke the following static Binder methods
		 *   Binder.getCallingPid()
		 *   Binder.getCallingUid()
		 *   Binder.getCallingUserHandle()
		 */


//		ReferenceType ref = evt.location().declaringType(); // static reference to enclosing class
//		for (Field f : ref.fields()) //ref.allFields())
//		{
//			// ignore unmodifiable variables
//			if (Utils.shouldExcludeField(f)) {
//				continue;
//			}
//			System.out.println("       " + (f.isStatic() ? "static " : "") + f.typeName() + "  " + f.name() + " = " + Utils.getValueAsString(currentThis.getValue(f)) + "\n             [in " + f.declaringType() + "]\n");
//		}
		
		
		CapturedState capState = new CapturedState(threadRef, currentThis, be);
		this.capturedStates.add(capState);	
		capState.dump();
		capState.visualize();


		// TODO:  could potentially capture the state of a whole service that may span multiple methods that touch different objects. 
		//        all of those objects should be included
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



		// prints local stack variables
		//			List<LocalVariable> visVars = stackFrame.visibleVariables();
		//			for (LocalVariable visibleVar : visVars)
		//			{
		//				Value val = stackFrame.getValue(visibleVar);
		//				System.out.print(visibleVar.name() + " = " + Utils.getValueAsString(val));	
		//			}

	}
	
	
	
	public void handleMethodExitEvent(MethodExitEvent evt) throws IncompatibleThreadStateException {
		// first, check to see if we've stopped at a method that we actually care about
		// methods we care about should each have a BreakpointEntry created for them
		boolean isMethodOfInterest = false;
		for (BreakpointEntry e : this.breakpoints) {
			if (e.mthd.equals(evt.method()) && 
				e.type.equals(BreakpointType.EXIT)) {
				isMethodOfInterest = true;
				break;
			}
		}
		if (!isMethodOfInterest) {
			System.out.println("\n--> stopped at method exit for don't care method " + evt.method().declaringType() + "." + evt.method().name());
			return;
		}
		
		System.out.println("\n=======================================================================");
		System.out.println("============ EXIT Breakpoint at line " + evt.location().lineNumber() + "  (" + evt.method().declaringType() + "." + evt.method().name() + ") ================");
		System.out.println("=======================================================================");
		
		ThreadReference threadRef = evt.thread();
		StackFrame stackFrame = threadRef.frame(0);
		ObjectReference currentThis = stackFrame.thisObject(); // dynamic reference to "this" current instance object
		
		Utils.dumpCallStack(threadRef);
		
		MethodExitRequest request = (MethodExitRequest)evt.request();
		
		BreakpointEntry be = new BreakpointEntry(request, BreakpointType.EXIT, evt.method());
		final CapturedState capState = new CapturedState(threadRef, currentThis, be);
		this.capturedStates.add(capState);
		
		/*
		 *  now that the state has been saved, disable and remove the request,
		 *  because it's going to be re-created during the next ENTRY breakpoint event handler 
		 */
		request.disable();
		this.vm.eventRequestManager().deleteEventRequest(request);
		
		
		// in order to minimize the time that the remote JVM is paused, run the rest of the stuff in a new thread
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				capState.dump();
				capState.visualize();		
				CapturedState beginState = findMatchingBeginState(capState);
				compareStates(beginState, capState);
			}
		});
		t.start();

	}
	
	
	
	/**
	 * returns the beginning state (at method entry) that matches the given endState.
	 * They must be for the same method, the same thread, and the returned state must be an ENTRY breakpoint. 
	 * @param endState the ending state (at method exit) to match
	 * @return
	 */
	private CapturedState findMatchingBeginState(CapturedState endState) {
		// start from the end of the list to find the most recent matching one first
		for (int i = this.capturedStates.size() - 1; i >= 0; i--) 
		{
			CapturedState curr = this.capturedStates.get(i);
			if (curr.getMethod().equals(endState.getMethod()) &&
				curr.getThreadRef().equals(endState.getThreadRef()) && 
				curr.getType().equals(BreakpointType.ENTRY))
			{
				return curr;
			}
		}
		
		System.err.println("Couldn't find a matching beginState for endState = " + endState);
		return null;
	}
	
	
	public void compareStates(CapturedState beg, CapturedState end) {
//		compareStatesJOD(beg, end);
		
		compareStatesJavaUtil(beg, end);
		
//		compareStatesJavers(beg, end);
	}

	/**
	 * Compares states using the java-object-diff library
	 * @param base
	 * @param working
	 */
	public void compareStatesJOD(final CapturedState base, final CapturedState working) {
		// how to use java-object-diff:
		// http://java-object-diff.readthedocs.io/en/latest/getting-started/
		
		// TODO: exclude the VariableNode.parent field from any analysis, it will always be CIRCULAR
		
		DiffNode diff = ObjectDifferBuilder.buildDefault().compare(working.getRootObject(), base.getRootObject());
		
		diff.visit(new DiffNode.Visitor()
		{
		    public void node(DiffNode node, Visit visit)
		    {
		    	// skip irrelevant unchanged nodes
		    	if ((node.getState().equals(de.danielbechler.diff.node.DiffNode.State.CIRCULAR)) ||
		    	   (node.getState().equals(de.danielbechler.diff.node.DiffNode.State.UNTOUCHED)))	    	
		    	{
		    		return; 
		    	}
		    	
//		    	final Object baseValue = node.canonicalGet(base);
//		        final Object workingValue = node.canonicalGet(working);
//		        final String message = node.getPath() + " changed from " + 
//		                               baseValue + " to " + workingValue;
//		        System.out.println(message);
		    	System.out.println(node.getPath() + " => " + node.getState());
		    }
		});
		
		System.out.println("\n---- finished printing java-object-diff changes ----\n\n");
	}
	
	
	public void compareStatesJavaUtil(CapturedState beg, CapturedState end) {
		List<Delta> diffs = GraphComparator.compare(beg.getRootObject(), end.getRootObject(), new ID() {
			@Override
			public Object getId(Object objectToId) {
				if (objectToId instanceof VariableNode) {
					return ((VariableNode)objectToId).getValue();
				}
				
				// TODO: handle other object types? dunno
				
				return objectToId;
			}
		});
		
		for (Delta d : diffs) {
			Object source = d.getSourceValue();
			Object target = d.getTargetValue();

			if (source instanceof VariableNode && target instanceof VariableNode) {
				VariableNode s = (VariableNode)source;
				VariableNode t = (VariableNode)target;
				System.out.println(s.getName() + " changed, old=" + s.getValueAsString() + ", new=" + t.getValueAsString() + "  (" + s.getTypeString() + ")");
				List<VariableNode> hierarchy = new ArrayList<VariableNode>();
				VariableNode parent = s;
				do {
					hierarchy.add(parent);
					if (parent != null) {
						parent = (VariableNode) parent.getParent();
					}
					else {
						break;
					}
				} while (true);
				System.out.print("      ");
				for (int i = hierarchy.size() - 1; i >= 0; i--) {
					VariableNode n = hierarchy.get(i);
					if (n == null) {
						System.out.print("null -> ");
					}
					else {
						System.out.print(n.getName() + " -> ");
					}
				}
				System.out.println();
			}
			else {
//				System.out.println("unknown type: " + d);
			}
		}
		
		System.out.println("\n---- finished printing java-util GraphComparator changes ----\n\n");
	}
	
	
	/**
	 * a comparison of state using jaVers, which still doesn't work!
	 * @param beg
	 * @param end
	 */
	public void compareStatesJavers(CapturedState beg, CapturedState end) {
		// let's test jaVers here
		Javers javers = JaversBuilder.javers()
				.registerValue(com.sun.jdi.Value.class)
		        .withListCompareAlgorithm(ListCompareAlgorithm.LEVENSHTEIN_DISTANCE)
		        .build();
		
		Diff diff = javers.compare(beg.getRootObject(), end.getRootObject());
//		Diff diff = javers.compareCollections(beg.getRootObject().getChildren(), end.getRootObject().getChildren(), VariableNode.class);
		System.out.println(diff.prettyPrint());
	}
}
