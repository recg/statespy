package filter;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Parses and controls the filter directory, which specifies the member fields that may be modified 
 * for a given class and (optionally) a given method. <br> 
 * This class is a singleton, and you must call the static method <b><code>initialize()</code></b> 
 * before invoking any other <tt>FilterManager</tt> methods.
 * <br>
 * The structure of the filter directory is as such: 
 * <li>One directory per class</li>
 * <li>One file per method (or transaction ID) in each class directory</li>
 * <br>
 * The name of the class directories should be the full qualified name of the class.
 * The name of the method files should be the full signature of that method.
 * <br><br>
 * A simple example is shown below: <br>
 * <pre>
 * filterDirectory
 * |-- com.android.server.ActivityManagerService
 * |   |-- java.util.List getRecentTasks()
 * |   |-- void startActivity(int,java.lang.String,boolean)
 * |   |-- ...
 * |-- com.android.server.clipboard.ClipboardService
 * |   |-- android.content.ClipData getPrimaryClip()
 * |   |-- void setPrimaryClip(android.content.ClipData,int)
 * |-- ...
 * </pre>
 * 
 * @author Kevin Boos
 */
public class FilterManager {

	
	private String filterRootDirectory;
	
	private Map<String, ClassFilter> allFilters = java.util.Collections.emptyMap();
	
	private static FilterManager instance = null;			
	
	/**
	 * you must call this first before using the FilterManager
	 * @param filterRootDir
	 */
	public static void initialize(String filterRootDir) {
		if (instance == null)
			instance = new FilterManager(filterRootDir);
	}
	
	private FilterManager(String filterRootDir) {
		this.filterRootDirectory = filterRootDir;
		parseFilterRootDirectory();
	}
	
	
	private void parseFilterRootDirectory() {
		allFilters = new HashMap<String, ClassFilter>();
		
		File[] files = new File(filterRootDirectory).listFiles();
		for (File f : files) {
			if (f.isDirectory()) {
				try {
					String className = f.getName();
					ClassFilter filter = new ClassFilter(f.getCanonicalPath(), className);
					allFilters.put(className, filter);
				} catch (IOException e) {
					System.err.println("Failed to parse ClassFilter for directory: " + f.toString());
					e.printStackTrace();
				}
			}
		}
	}
	
	public static void setFilterRootDirectoryAndParse(String filterRootDirectory) {
		instance.filterRootDirectory = filterRootDirectory;
		instance.parseFilterRootDirectory();
	}
	
	public static boolean shouldInclude(String variableName, String className, String methodName) {
		MethodFilter mf = getMethodFilter(className, methodName);
		if (mf != null) {
			return mf.shouldInclude(variableName);
		}
		
//		System.out.println("WARNING: cannot determine whether to include " + className + "." + variableName + "(method=" + methodName + ")");
		return true; // /always include by default, just to be safe
	}
	
	public static boolean shouldExclude(String variableName, String className, String methodName) {
		return !(shouldInclude(variableName, className, methodName));
	}
	
	public static ClassFilter getClassFilter(String className) {
		return instance.allFilters.get(className);
	}
	
	public static MethodFilter getMethodFilter(String className, String methodName) {
		ClassFilter cf = getClassFilter(className);
		if (cf != null) 
			return cf.getMethodFilter(methodName);
		else
			return null;
	}
	
	
	private void checkInstance() {
		if (instance == null) {
			System.err.println("Error: you must call FilterManager.initialize() before using any other FilterManager methods!");
			System.exit(-1);
		}
	}
	
	
}
