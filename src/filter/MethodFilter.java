package filter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * One instance of this class exists for every method-specific state filter file.
 * @author Kevin Boos
 *
 */
public class MethodFilter {

	private String filterFile;
	private String className;
	private String methodName;
	private boolean filterIsEmpty = false;

	private HashSet<String> includedFields = new HashSet<String>();
	private HashSet<String> excludedFields = new HashSet<String>();
	
	
	public MethodFilter(String filterFile, String className, String methodName) {
		this.filterFile = filterFile;
		this.className = className;
		this.methodName = methodName;
		parseFilterFile();
	}
	
	private boolean parseFilterFile() {
		try (BufferedReader br = new BufferedReader(new FileReader(filterFile))) {
		    String line;
		    while ((line = br.readLine()) != null) {
		    	// find includes
		    	if (line.toLowerCase().startsWith("y:") || line.toLowerCase().startsWith("i:") ) {
		    		includedFields.add(line.substring(2).trim());
		    	}
		    	
		    	// find excludes
		    	else if (line.toLowerCase().startsWith("n:") || line.toLowerCase().startsWith("e:") ) {
		    		excludedFields.add(line.substring(2).trim());
		    	}
		    	
		    	else {
		    		// support legacy filters that were whitelist only
		    		includedFields.add(line.trim());
		    	}
		    }
		    
		    filterIsEmpty = includedFields.isEmpty() && excludedFields.isEmpty();
		    return true;
		    
		} catch (IOException e) {
			e.printStackTrace();
		} 
		
		return false;
	}
	
	public boolean shouldInclude(String varName) {
		// empty filter files revert to a whitelist-all policy
		if (filterIsEmpty)
			return true;

		if (excludedFields.contains(varName))
			return false;
		
		if (includedFields.contains(varName)) 
			return true;

		return false; // exclude by default
	}
	
	public boolean shouldExclude(String varName) {
		return !(shouldInclude(varName));
	}
	
	public Set<String> getIncludeFilter() {
		return includedFields;
	}

	public String getClassName() {
		return className;
	}
	
	public String getMethodName() {
		return methodName;
	}
}
