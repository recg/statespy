package filter;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * One instance of this exists for every class filter directory in the filter root directory. 
 * @author Kevin Boos
 *
 */
public class ClassFilter {

	private String filterDirectory;
	private String className;

	private Map<String, MethodFilter> filters = java.util.Collections.emptyMap();	

	private HashSet<String> classwideFilterCached = null;

	public ClassFilter(String filterDirectory, String className) {
		this.filterDirectory = filterDirectory;
		this.className = className;
		parseFilterFile();
	}

	private void parseFilterFile() {
		filters = new HashMap<String, MethodFilter>();

		File[] files = new File(filterDirectory).listFiles();
		for (File f : files) {
			if (f.canRead()) {
				try {
					String methodName = f.getName();
					if (methodName.contains(".")) {
						methodName = methodName.substring(0, methodName.lastIndexOf('.')); // removes .txt file extension
					}
					MethodFilter filter = new MethodFilter(f.getCanonicalPath(), className, methodName);
					filters.put(methodName, filter);
				} catch (IOException e) {
					System.err.println("Failed to parse MethodFilter for file: " + f.toString());
					e.printStackTrace();
				}
			}
			else {
				try {
					System.err.println("Couldn't read file: " + f.getCanonicalPath());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public MethodFilter getMethodFilter(String methodName) {
		return filters.get(methodName);
	}

	public String getClassName() {
		return className;
	}

	/**
	 * A convenience method for getting all possible fields that should be included in this class's filter.
	 * @return a merged set containing the inclusion filter of every method in this class
	 */
	public HashSet<String> getClasswideFilter() {
		if (classwideFilterCached == null) {
			classwideFilterCached = new HashSet<String>();
			for (MethodFilter mf : filters.values()) {
				classwideFilterCached.addAll(mf.getIncludeFilter());
			}
		}
		return classwideFilterCached;
	}

}