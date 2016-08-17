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
import com.sun.jdi.FloatValue;
import com.sun.jdi.IntegerValue;
import com.sun.jdi.LongValue;
import com.sun.jdi.Method;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ShortValue;
import com.sun.jdi.StringReference;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.VirtualMachineManager;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;

public class Utils {

	
	public static String runShellCommand(String cmd) {
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



	public static VirtualMachine connectToDebuggeeJVM(int tcpPort) throws IOException, IllegalConnectorArgumentsException
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

	public static Object getPrimitiveFromValue(Value val)
	{
		if (val instanceof StringReference)
		{
			return ((StringReference)val).value();
		}
		else if (val instanceof IntegerValue)
		{
			return ((IntegerValue)val).value();
		}
		else if (val instanceof ShortValue)
		{
			return ((ShortValue)val).value();
		}
		else if (val instanceof LongValue)
		{
			return ((LongValue)val).value();
		}
		else if (val instanceof BooleanValue)
		{
			return ((BooleanValue)val).value();
		}
		else if (val instanceof FloatValue)
		{
			return ((FloatValue)val).value();
		}
		else if (val instanceof DoubleValue)
		{
			return ((DoubleValue)val).value();
		}
		else if (val instanceof ByteValue)
		{
			return ((ByteValue)val).value();
		}
		else if (val instanceof CharValue)
		{
			return ((CharValue)val).value();	
		}
		else {
			return null;
		}
	}
	
	public static String getValueAsString(Value val)
	{
		if (val instanceof StringReference)
		{
			String varNameValue = ((StringReference)val).value();
			return "" + varNameValue;
		}
		else if (val instanceof IntegerValue)
		{
			return "" + ((IntegerValue)val).value();
		}
		else if (val instanceof ShortValue)
		{
			return "" + ((ShortValue)val).value();
		}
		else if (val instanceof LongValue)
		{
			return "" + ((LongValue)val).value();
		}
		else if (val instanceof BooleanValue)
		{
			return "" + ((BooleanValue)val).value();
		}
		else if (val instanceof FloatValue)
		{
			return "" + ((FloatValue)val).value();
		}
		else if (val instanceof DoubleValue)
		{
			return "" + ((DoubleValue)val).value();
		}
		else if (val instanceof ByteValue)
		{
			return "" + ((ByteValue)val).value();
		}
		else if (val instanceof CharValue)
		{
			return "" + ((CharValue)val).value();	
		}
		else {
			return "";
		}
	}
	
	/**
	 * dumps all classes and their methods currently loaded by the debuggee VM
	 */
	public static void dumpAllClasses(VirtualMachine vm) {
		for (ReferenceType c : vm.allClasses())
		{
			System.out.println(c);
			for (Method m : c.methods())
				System.out.println("   " + m);
		}
	}
}
