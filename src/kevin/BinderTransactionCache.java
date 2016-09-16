package kevin;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Field;
import com.sun.jdi.IntegerValue;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Method;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.Type;
import com.sun.jdi.Value;

public class BinderTransactionCache {

	//	private static final String TRANSACTION_CACHE_ROOT_DIR = "transaction_cache";
	//
	//	static {
	//		try(BufferedReader in = new BufferedReader(new FileReader(TRANSACTION_CACHE_ROOT_DIR))) {
	//		    String s = in.readLine();
	//		    typeMappings.add(s);
	//		}catch (IOException e) {
	//		    System.err.println("No existing type mappings file, creating one ...");
	//		}
	//	}
	//	
	//	private static void populateTypeMappingsFile(Type staticType, Type runtimeType) {
	//		String mappingString = staticType.name() + ", " + runtimeType.name();
	//		if (!typeMappings.contains(mappingString)) {
	//			try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(TYPE_MAPPINGS_FILE, true)))) {
	//				out.println(mappingString);
	//				typeMappings.add(mappingString);
	//			}catch (IOException e) {
	//				System.err.println(e);
	//			}
	//		}
	//	}

	// TODO: modify this to cache things to disk for next time

	private static HashMap<String, HashMap<Integer, String>> allTransactions = new HashMap<>();


	private static void cacheTransaction(String className, int code, String transactionStr) {
		HashMap<Integer, String> classTransactions = allTransactions.get(className);
		if (classTransactions == null) {
			classTransactions = new HashMap<>(); // thanks Java 7 for enabling my laziness :)
		}
		classTransactions.put(code, transactionStr);
		allTransactions.put(className, classTransactions);
	}


	private static String getCachedTransactionString(String className, int code) {
		HashMap<Integer, String> classTransactions = allTransactions.get(className);
		if (classTransactions != null) {
			return classTransactions.get(code);
		}

		return null;
	}


	public static String getCurrentTransaction(Method currentMethod, StackFrame stackFrame) {
		// get the code of this binder transaction
		int binderCode = -1;
		if (currentMethod.name().contains("onTransact")) {
			try {
				LocalVariable codeArg0 = currentMethod.arguments().get(0);
				Value codeValue = stackFrame.getValue(codeArg0);
				if (codeValue instanceof IntegerValue) {
					binderCode = ((IntegerValue) codeValue).value();
				}
			} catch (AbsentInformationException | IndexOutOfBoundsException e1) {
				e1.printStackTrace();
			}			
		}

		// next, get the transaction variable name matching that code (just for better human readability) 
		if (binderCode != -1) {
			// see if we have already resolved that transaction code (and it's been cached)
			String cachedTransactionStr = getCachedTransactionString(currentMethod.declaringType().name(), binderCode);
			if (cachedTransactionStr != null)
				return cachedTransactionStr;


			// if we haven't seen that class's transaction code before, look it up for the first time
			for (Field f : currentMethod.declaringType().fields()) {
				if (f.name().startsWith("TRANSACTION_")) {
					IntegerValue val = (IntegerValue) currentMethod.declaringType().getValue(f);
					if (binderCode == val.value()) {
						String binderTransaction = f.name() + " (" + binderCode + ")";
						cacheTransaction(currentMethod.declaringType().name(), binderCode, binderTransaction);
						return binderTransaction;
					}
				}
			}
		}
		
		return null;
	}

}
