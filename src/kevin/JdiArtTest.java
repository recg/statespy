package kevin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Bootstrap;
import com.sun.jdi.Field;
import com.sun.jdi.IntegerValue;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.StringReference;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.VirtualMachineManager;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventIterator;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.EventRequest;




// https://dzone.com/articles/generating-minable-event
//http://wayne-adams.blogspot.com/search?q=JDI
// http://stackoverflow.com/questions/8700988/java-debug-interface-put-the-breakpoints-at-arbitrary-locations-in-the-code

// how to monitor classloaders: http://www.jguru.com/faq/view.jsp?EID=463990

// a bunch of JDI snippets: http://www.programcreek.com/java-api-examples/index.php?api=com.sun.jdi.Field

// may be useful: http://stackoverflow.com/questions/35420987/how-do-you-set-a-breakpoint-before-executing

// http://alvinalexander.com/java/jwarehouse/eclipse/org.eclipse.jdt.debug.jdi.tests/tests/org/eclipse/debug/jdi/tests/ObjectReferenceTest.java.shtml
// http://stackoverflow.com/questions/24236361/jdi-objectreference-setvalue-on-an-inherited-field


/**
 * 
 * @author kevin
 * Before running this, run the following command:
 * <pre> adb forward tcp:8000 jdwp:$JDWP_PORT     </pre>
 * in which <tt>$JDWP_PORT</tt> is the PID of the JDWP-enabled process you wish to debug on the Android device.
 * <br>
 * 
 *  On the android device, run the following as a sample dalvik process:
 *  <pre> dalvikvm  -agentlib:jdwp=transport=dt_android_adb,server=y,suspend=y -cp /sdcard/Hello.dex Hello </pre>
 *  The <tt>dt_android_adb</tt> option is necessary because that's the only way we can connect to existing system services
 *  (using dt_socket isn't scalable to more than one dalvikvm instance on the device).
 */
public class JdiArtTest {

	// The system_server process is given a different PID every time the Android device reboots,
	// so we'll have to find it dynamically at some point.


	private static String runShellCommand(String cmd) {
		try {
			Process p = Runtime.getRuntime().exec("/usr/bin/timeout 1s " + cmd);

			BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
			BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));

			// read the output from the command
			StringBuilder sb = new StringBuilder();
			String s;
			while ((s = stdInput.readLine()) != null) {
				sb.append(s).append("\n");
			}

			// read any errors from the attempted command
			//			System.out.println("Here is the standard error of the command (if any):\n");
			while ((s = stdError.readLine()) != null) {
				System.out.println(s);
			}

			return sb.toString();
		}
		catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * @return the PID of the first JDWP-enabled process on the 
	 */
	public static ArrayList<Integer> getJdwpPids() {
		String output = runShellCommand("/home/kevin/android/android-sdk-linux/platform-tools/adb jdwp");

		ArrayList<Integer> pids = new ArrayList<Integer>();
		for (String s : output.split("\\s"))
			pids.add(Integer.parseInt(s));;

			return pids;
	}


	/**
	 * Forwards a TCP port on the host machine to a JDWP-enabled process on the device.
	 *  
	 * @param tcpPort the port on the localhost that the host uses
	 * @param jdwpPid the PID of the JDWP-enabled process on the Android device 
	 * @return true if ports are successfully forwarded
	 */
	public static boolean forwardAdbPort(int tcpPort, int jdwpPid) {
		// doesn't handle errors yet
		runShellCommand("/home/kevin/android/android-sdk-linux/platform-tools/adb forward tcp:" + tcpPort + " jdwp:" + jdwpPid);
		return true;
	}



	public static VirtualMachine connectToDebugeeJVM(int tcpPort) throws IOException, IllegalConnectorArgumentsException
	{
		VirtualMachineManager vmMgr = Bootstrap.virtualMachineManager();
		AttachingConnector socketConnector = null;
		List<AttachingConnector> attachingConnectors = vmMgr.attachingConnectors();
		for (AttachingConnector ac: attachingConnectors)
		{
			if (ac.transport().name().equals("dt_socket"))
			{
				socketConnector = ac;
				break;
			}
		}
		if (socketConnector == null)
		{
			System.err.println("No available attaching connector for dt_socket.");
			return null;
		}


		Map paramsMap = socketConnector.defaultArguments();

		Connector.IntegerArgument portArg = (Connector.IntegerArgument)paramsMap.get("port");
		portArg.setValue(tcpPort);

		// set the hostname to be localhost, not the hostname of this machine
		Connector.StringArgument hostnameArg = (Connector.StringArgument)paramsMap.get("hostname");
		hostnameArg.setValue("localhost");

		VirtualMachine vm = socketConnector.attach(paramsMap);
		System.out.println("Attached to process '" + vm.name() + "' at " + paramsMap.get("hostname") + ":" + paramsMap.get("port"));

		return vm;
	}

	
	
	private static String getValueAsString(Value val)
	{
		if (val instanceof StringReference)
		{
			String varNameValue = ((StringReference)val).value();
			return "" + varNameValue + "\n";
		}
		else if (val instanceof IntegerValue)
		{
			return "" + ((IntegerValue)val).value() + "\n";
		}
	
		return "";
	}


	public static void main(String[] args) throws Exception
	{
		boolean firstPid = true; // true selects the system service
		int chosenTcpPort = 8888;

		ArrayList<Integer> pids = getJdwpPids();
		int chosenPid = firstPid ? pids.get(0) : pids.get(pids.size() - 1);

		forwardAdbPort(chosenTcpPort, chosenPid);
		VirtualMachine vm = connectToDebugeeJVM(chosenTcpPort);

		String className = "AlarmManagerService";
		String methodName = "setImpl";

		for (ReferenceType c : vm.allClasses())
		{
			System.out.println(c);
		}
		ReferenceType classRef = vm.classesByName(className).get(0);
		Method mthd = classRef.methodsByName(methodName).get(0);
		BreakpointRequest brF1 = vm.eventRequestManager().createBreakpointRequest(mthd.location());
		brF1.setSuspendPolicy(BreakpointRequest.SUSPEND_ALL);
		brF1.enable();



		EventQueue evtQueue = vm.eventQueue();
		while(true)
		{
			EventSet evtSet = evtQueue.remove();
			EventIterator evtIter = evtSet.eventIterator();
			while (evtIter.hasNext())
			{
				try
				{
					Event evt = evtIter.next();
					EventRequest evtReq = evt.request();
					if (evtReq instanceof BreakpointRequest)
					{
						BreakpointRequest bpReq = (BreakpointRequest)evtReq;
						System.out.println("================ Breakpoint at line " + bpReq.location().lineNumber() + "  (" + bpReq.location().method().name() + ") ================");
						BreakpointEvent brEvt = (BreakpointEvent)evt;
						
						ThreadReference threadRef = brEvt.thread();
						StackFrame stackFrame = threadRef.frame(0);
						ObjectReference currentThis = stackFrame.thisObject(); // dynamic reference to "this" current instance object
						
						
						ReferenceType ref = brEvt.location().declaringType(); // static reference to enclosing class
						for (Field f : ref.allFields())
						{
							System.out.println("  " + f.name() + " = " + getValueAsString(currentThis.getValue(f)));
						}
						

						
						  
						// prints local stack variables
//						List<LocalVariable> visVars = stackFrame.visibleVariables();
//						for (LocalVariable visibleVar : visVars)
//						{
//							Value val = stackFrame.getValue(visibleVar);
//							System.out.print(visibleVar.name() + " = " + getValueAsString(val));	
//						}
						

					}
				}
				
//				catch (AbsentInformationException aie)
//				{
//					System.out.println("AbsentInformationException: did you compile your target application with -g option?");
//					aie.printStackTrace();
//				}
				
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
}
