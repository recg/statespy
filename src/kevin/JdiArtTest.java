package kevin;

import java.util.ArrayList;

import com.sun.jdi.Method;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.VirtualMachine;




// https://dzone.com/articles/generating-minable-event

// a bunch of JDI snippets: http://www.programcreek.com/java-api-examples/index.php?api=com.sun.jdi.Field

// may be useful: http://stackoverflow.com/questions/35420987/how-do-you-set-a-breakpoint-before-executing

// http://alvinalexander.com/java/jwarehouse/eclipse/org.eclipse.jdt.debug.jdi.tests/tests/org/eclipse/debug/jdi/tests/ObjectReferenceTest.java.shtml


// check out pg 10:  https://fivedots.coe.psu.ac.th/~ad/jg/javaArt3/traceJPDA.pdf


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


	private static void getBinderCaller()
	{

	}

//	public static void main(String[] args) throws Exception
//	{
//		VariableNode node1 = new VariableNode("me", "t", null, null);
//		VariableNode node2 = new VariableNode("me", "to", null, null);
//		Javers javers = JaversBuilder.javers().build();
//
//		Diff diff = javers.compare(node1, node2);
//		System.out.println(diff.prettyPrint());
//	}

		public static void main(String[] args) throws Exception
		{
			boolean firstPid = true; // "true" selects the system service, "false" selects the latest JDWP-enabled process 
			int chosenTcpPort = 8888;
	
			ArrayList<Integer> pids = Utils.getJdwpPids();
			int chosenPid = firstPid ? pids.get(0) : pids.get(pids.size() - 1);
	
			Utils.forwardAdbPort(chosenTcpPort, chosenPid);
			VirtualMachine vm = Utils.connectToDebuggeeJVM(chosenTcpPort);
	
	
//			System.out.println(Utils.getAllClasses(vm, true));
			
			BreakpointEventHandler bkptHandler = new BreakpointEventHandler(vm);
			
//			String className = "com.android.server.AlarmManagerService";
//			String methodName = "setImpl";
			String className = "com.android.server.clipboard.ClipboardService";
			String methodName = "onTransact";
			
			
//			String className = "com.android.server.AlarmManagerService$Constants";
//			String methodName = "onChange";
			
			
//			System.out.println(Utils.findMatchingClasses(vm, "onChange"));
			
			ReferenceType classRef = vm.classesByName(className).get(0);
			Method mthd = classRef.methodsByName(methodName).get(0);
			
			bkptHandler.addBreakpointAtMethod(mthd, BreakpointType.ENTRY, true);
			
			bkptHandler.start();
			
		}	
}
