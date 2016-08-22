package kevin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.sun.jdi.BooleanValue;
import com.sun.jdi.Bootstrap;
import com.sun.jdi.ByteValue;
import com.sun.jdi.CharValue;
import com.sun.jdi.DoubleValue;
import com.sun.jdi.Field;
import com.sun.jdi.FloatValue;
import com.sun.jdi.IntegerValue;
import com.sun.jdi.Location;
import com.sun.jdi.LongValue;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ShortValue;
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


	public static void main(String[] args) throws Exception
	{
		boolean firstPid = true; // true selects the system service
		int chosenTcpPort = 8888;

		ArrayList<Integer> pids = Utils.getJdwpPids();
		int chosenPid = firstPid ? pids.get(0) : pids.get(pids.size() - 1);

		Utils.forwardAdbPort(chosenTcpPort, chosenPid);
		VirtualMachine vm = Utils.connectToDebuggeeJVM(chosenTcpPort);


		//dumpAllClasses();
		
		BreakpointEventHandler bkptHandler = new BreakpointEventHandler(vm);
		
		String className = "com.android.server.AlarmManagerService";
		String methodName = "setImpl";
		ReferenceType classRef = vm.classesByName(className).get(0);
		Method mthd = classRef.methodsByName(methodName).get(0);
		
		bkptHandler.addBreakpointAtMethod(mthd, BreakpointType.ENTRY, true);
		bkptHandler.addBreakpointAtMethod(mthd, BreakpointType.EXIT, true);
		
		bkptHandler.start();
		







	}	
}
