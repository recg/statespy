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

	private HashSet<String> includedFields = new HashSet<String>();
	
	
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
		    	includedFields.add(line.trim());
		    }
		    return true;
		} catch (IOException e) {
			e.printStackTrace();
		} 
		
		return false;
	}
	
	public boolean shouldInclude(String varName) {
		return includedFields.contains(varName);
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
