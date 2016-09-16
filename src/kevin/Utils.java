package kevin;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.BooleanValue;
import com.sun.jdi.Bootstrap;
import com.sun.jdi.ByteValue;
import com.sun.jdi.CharValue;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.ClassObjectReference;
import com.sun.jdi.ClassType;
import com.sun.jdi.DoubleValue;
import com.sun.jdi.Field;
import com.sun.jdi.FloatValue;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.IntegerValue;
import com.sun.jdi.InvalidTypeException;
import com.sun.jdi.InvocationException;
import com.sun.jdi.Location;
import com.sun.jdi.LongValue;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.PrimitiveType;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ShortValue;
import com.sun.jdi.StackFrame;
import com.sun.jdi.StringReference;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Type;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.VirtualMachineManager;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;


public class Utils {


	/**
	 * a reference to the target JVM's static "Class" identifier,
	 * which we will use to invoke <code>Class.forName()</code> on the target JVM
	 * to force it to load a particular class.
	 * Only need to obtain this once at the beginning, 
	 * then we can save it for repeated future use. 
	 */
	private static ClassType targetJvmClass = null;

	/**
	 * pairs with the {@link targetJvmClass} above
	 * A static reference to the <code>Class.forName()</code> method on the target JVM. 
	 * Used as a dirty hack to load classes into the remote JVM on demand. 
	 */
	private static Method targetJvmForName = null;

	private static HashSet<String> loadedClasses = new HashSet<String>();

	public static String adbPath;


	private static final String TYPE_MAPPINGS_FILE = "static_to_runtime_type_mappings.txt";
	private static HashSet<String> typeMappings = new HashSet<String>();
	static {
		try(BufferedReader in = new BufferedReader(new FileReader(TYPE_MAPPINGS_FILE))) {
		    String s = in.readLine();
		    typeMappings.add(s);
		}catch (IOException e) {
		    System.err.println("No existing type mappings file, creating one ...");
		}
	}
	
	private static void populateTypeMappingsFile(Type staticType, Type runtimeType) {
		String mappingString = staticType.name() + ", " + runtimeType.name();
		if (!typeMappings.contains(mappingString)) {
			try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(TYPE_MAPPINGS_FILE, true)))) {
				out.println(mappingString);
				typeMappings.add(mappingString);
			}catch (IOException e) {
				System.err.println(e);
			}
		}
	}
	
		
	
	/**
	 * Currently, this function will return true if the field is:
	 * <li> an enumeration constant
	 * <li> a final primitive or final string
	 * <li> the shadow fields: shadow$_klass and shadow$_monitor
	 * @param f
	 * @return
	 */
	public static boolean shouldExcludeField(Field f, Type runtimeType) {
		// ignore unmodifiable variables
		try {
			populateTypeMappingsFile(f.type(), runtimeType);
			
			return (f.isEnumConstant() ||   // ignore constants
					(f.isFinal() && (f.typeName().contains("java.lang.String") || (f.type() instanceof PrimitiveType))) || //only ignore final primitives, not final Objects (a final arraylist can still be modified)  
					f.name().contains("shadow$_klass_") ||  // always exclude GC-related stuff
					f.name().contains("shadow$_monitor_")  // always exclude GC-related stuff
					);

		} catch (ClassNotLoadedException e) {
			//			e.printStackTrace();
			System.err.println(e.toString() + ". " + e.getMessage());
		}

		return false; // by default, we should include a state in our analysis capture just to be cautious
	}


	/**
	 * This doesn't necessarily return the last line of a method that you can set a breakpoint on. 
	 * @param m the method to find the end of.
	 * @return the {@link Location} of the last line in Method m
	 * @throws AbsentInformationException 
	 */
	public static Location getEndOfMethodLocation(Method m) throws AbsentInformationException {
		// find last line of the method
		List<Location> mthdLocations = m.allLineLocations(); // this is not sorted by default
		Location endOfMethod = m.location(); // start at beg of method
		int maxLineNumber = 0;
		for (Location l : mthdLocations) {
			if (l.lineNumber() > maxLineNumber) {
				maxLineNumber = l.lineNumber();
				endOfMethod = l;
			}
		}

		return endOfMethod;
	}


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
		String output = runShellCommand(adbPath + " jdwp");

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
		runShellCommand(adbPath + " forward tcp:" + tcpPort + " jdwp:" + jdwpPid);
		return true;
	}



	/**
	 * returns true <i>only the very first time</i> that the class was newly loaded. 
	 * Repeated attempts will return false because the class would already have been loaded.
	 * @param ref
	 * @param classToLoad
	 * @return
	 */
	public static boolean loadClassInTargetJvm(ThreadReference ref, String classToLoad) {
		if (classToLoad == null)
			return false;

		// don't repeatedly attempt to load the same class
		if (loadedClasses.contains(classToLoad))
			return false; 

		if (targetJvmClass == null) {
			targetJvmClass = (ClassType) ref.virtualMachine().classesByName("java.lang.Class").get(0);
		}
		if (targetJvmForName == null) {
			targetJvmForName = targetJvmClass.methodsByName("forName", "(Ljava/lang/String;)Ljava/lang/Class;").get(0);
		}
		if (targetJvmClass == null || targetJvmForName == null) {
			return false; 
		}

		List<Value> args = new ArrayList<Value>();
		args.add(ref.virtualMachine().mirrorOf(classToLoad));
		try {
			Value result = targetJvmClass.invokeMethod(ref, targetJvmForName, args, ObjectReference.INVOKE_SINGLE_THREADED);
			if (result instanceof ClassObjectReference) { 
				if (((ClassObjectReference)result).reflectedType().name().contains(classToLoad)) {
					loadedClasses.add(classToLoad);
					return true;
				}
			}
		} catch (InvalidTypeException | ClassNotLoadedException | IncompatibleThreadStateException
				| InvocationException e) {
			return false;
		}

		return false;
	}


	public static VirtualMachine connectToDebuggeeJVM(int tcpPort) throws IOException, IllegalConnectorArgumentsException {
		return connectToDebuggeeJVM("localhost", tcpPort);
	}

	public static VirtualMachine connectToDebuggeeJVM(String hostname, int tcpPort) throws IOException, IllegalConnectorArgumentsException {
		VirtualMachineManager vmMgr = Bootstrap.virtualMachineManager();
		AttachingConnector socketConnector = null;
		for (AttachingConnector ac: vmMgr.attachingConnectors())
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
		System.out.println("Attached to process '" + vm.name() + "' at " + paramsMap.get("hostname") + ", " + paramsMap.get("port") + "\n");

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


	public static String findMatchingClasses(VirtualMachine vm, String keyword) {
		StringBuilder sb = new StringBuilder();

		for (ReferenceType c : vm.allClasses())
		{
			if (c.toString().contains(keyword)) {
				sb.append(c + "\n");
			}
		}
		return sb.toString();
	}


	public static String getAllClasses(VirtualMachine vm) {
		return getAllClasses(vm, false);
	}

	/**
	 * dumps all classes and their methods currently loaded by the debuggee VM
	 */
	public static String getAllClasses(VirtualMachine vm, boolean includeMethods) {
		StringBuilder sb = new StringBuilder();

		for (ReferenceType c : vm.allClasses())
		{
			sb.append(c + "\n");
			if (includeMethods) {
				for (Method m : c.methods())
					sb.append("   " + m + "\n");
			}
		}

		return sb.toString();
	}


	public static void getUniqueFieldTypes(VirtualMachine vm) {
		HashSet<String> serviceFieldTypes = new HashSet<>();

		for (ReferenceType c : vm.allClasses())
		{
			if (c.name().toLowerCase().contains("service")) {
				for (Field f : c.allFields()) {
					if (f.typeName().toLowerCase().contains("array") || 
						f.typeName().toLowerCase().contains("map") || 
						f.typeName().toLowerCase().contains("registry")) {
						serviceFieldTypes.add(f.typeName());
					}
				}
			}
		}

		for (String s : serviceFieldTypes) {
			System.out.println(s);
		}
	}


	/**
	 * prints the call stack of the given thread
	 * @param threadRef the thread to print the callstack for
	 */
	public static void dumpCallStack(ThreadReference threadRef) {
		System.out.println("    --- Callstack --- ");
		try {
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
		}
		catch (IncompatibleThreadStateException ex) {
			System.err.println("Couldn't get callstack: " + ex);
		}
	}
	
	
	
	
	public static String getAdbPath() {
		return adbPath;
	}
	public static void setAdbPath(String adbPath) {
		Utils.adbPath = adbPath;
	}

}
