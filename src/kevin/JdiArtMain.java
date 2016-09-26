package kevin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ClassNotPreparedException;
import com.sun.jdi.Method;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;

import filter.FilterManager;




// https://dzone.com/articles/generating-minable-event

// a bunch of JDI snippets: http://www.programcreek.com/java-api-examples/index.php?api=com.sun.jdi.Field

// may be useful: http://stackoverflow.com/questions/35420987/how-do-you-set-a-breakpoint-before-executing

// http://alvinalexander.com/java/jwarehouse/eclipse/org.eclipse.jdt.debug.jdi.tests/tests/org/eclipse/debug/jdi/tests/ObjectReferenceTest.java.shtml


// check out pg 10:  https://fivedots.coe.psu.ac.th/~ad/jg/javaArt3/traceJPDA.pdf


/**
 * @author Kevin Boos
 * Before running this, an Android device must be connected and visible to the <code>adb devices</code> command.
 * 
 *  On the android device, to test a simple process, run the following dalvik command:
 *  <pre> dalvikvm  -agentlib:jdwp=transport=dt_android_adb,server=y,suspend=y -cp /sdcard/Hello.dex Hello </pre>
 *  The <tt>dt_android_adb</tt> option is necessary because that's the only way we can connect to existing system services
 *  (using dt_socket isn't scalable to more than one dalvikvm instance on the device).
 */
public class JdiArtMain {

	
	/*
	 * command line args are handled by the jcommander framework, which automatically parses them and generates usage messages
	 */
	private JCommander jcommander; 
	
	@Parameter(names = { "-c", "--class" }, description = "the fully-qualified classname that contains the method for analysis")
	private String className = null;

	@Parameter(names = { "-m", "--method" }, description = "the method name to set a breakpoint at")
	private String methodName = "onTransact"; 
	
	@Parameter(names = { "-f", "--filter" }, description = "the name of the filter root directory that contains class/method field filters")
	private String filterRootDirectory = "./filters/";

	@Parameter(names = { "-d", "--depth" }, description = "max recursion depth when capturing object graphs")
	private int maxDepth = 10;
	
	@Parameter(names = { "-v", "--visualize" }, description = "whether to show every object's JTree visualization window")
	private boolean visualize = false;
	
	@Parameter(names = { "-s", "--system-service" }, description = "whether to debug the system_server process (true) or the most recently-started JDWP process (false)")
	boolean firstPid = true; // "true" selects the system service, "false" selects the latest JDWP-enabled process 

	@Parameter(names = { "-p", "--port" }, description = "the host's TCP port to forward adb JDWP requests to")
	private int chosenTcpPort = 8888;
	
	@Parameter(names = { "--adb-path" }, description = "the full, absolute path to the adb executable")
	String adbPath = "/home/delmilio/Android/Sdk/platform-tools/adb";
	
	@Parameter(names = { "-h", "--help" }, description = "prints this help message", help = true)
	private boolean help;


	public static void main(String[] args) throws Exception
	{
		JdiArtMain driver = new JdiArtMain();
		driver.jcommander = new JCommander(driver, args); // parses command-line args
		driver.jcommander.setProgramName("JDI State Spill Tool");
		driver.go();
	}
	
	/**
	 * do the actual work from the main function, under non-static context
	 * @throws IllegalConnectorArgumentsException 
	 * @throws IOException 
	 * @throws AbsentInformationException 
	 */
	private void go() throws IOException, IllegalConnectorArgumentsException, AbsentInformationException {
		
		if (!validateCmdLineArgs()) {
			jcommander.usage();
			System.exit(-1);
		}
		
		Utils.setAdbPath(adbPath); // shitty way to do it, I know
		
		ArrayList<Integer> pids = Utils.getJdwpPids();
		int chosenPid = firstPid ? pids.get(0) : pids.get(pids.size() - 1);

		Utils.forwardAdbPort(chosenTcpPort, chosenPid);
		VirtualMachine vm = Utils.connectToDebuggeeJVM(chosenTcpPort);

		FilterManager.initialize(filterRootDirectory);
		
		//			System.out.println(Utils.getAllClasses(vm, true));
		//			Utils.getUniqueFieldTypes(vm);

		BreakpointEventHandler bkptHandler = new BreakpointEventHandler(vm, maxDepth, visualize);
		
		//			System.out.println(Utils.findMatchingClasses(vm, "onChange"));

		
		try {
			ReferenceType classRef = vm.classesByName(className).get(0);
			Method mthd = classRef.methodsByName(methodName).get(0);

			bkptHandler.addBreakpointAtMethod(mthd, BreakpointType.ENTRY, true);
			bkptHandler.start();
		} 
		catch (ClassNotPreparedException | IndexOutOfBoundsException e) {
			System.err.println("Error: could not find class " + className + " and/or method " + methodName);
			jcommander.usage();
			System.exit(-1);
		}

	}	
	
	
	private boolean validateCmdLineArgs() {
		boolean success = true;
		
		// depth must be a positive integer or 0
		if (maxDepth < 0) {
			System.err.println("Error: depth cannot be negative, must be >= 0");
			success = false;
		}
		
		// must have a valid class name for analysis (method name defaults to be "onTransact")
		if (className == null) {
			System.err.println("Error: expected classname argument (-c, --class)");
			success = false;
		}
		
		// must have a filter root directory
		if (!(new File(filterRootDirectory).isDirectory())) {
			System.err.println("Error: couldn't find valid filter root directory at " + filterRootDirectory);
			success = false;
		}
		
		// must be able to execute adb shell command
		if (!(new File(adbPath).canExecute())) {
			System.err.println("Error: couldn't find or execute adb command at " + adbPath);
			success = false;
		}
		
		return success;
	}
}
