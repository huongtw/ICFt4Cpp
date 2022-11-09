package console;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import config.AbstractSetting;
import config.IFunctionConfig;
import config.Paths;
import config.Settingv2;
import instrument.FunctionInstrumentationForStatementvsBranch_Marker;
import testdatagen.AbstractTestdataGeneration;
import testdatagen.TestdataInReport;
import utils.Utils;

/**
 * Run cft4cpp on console
 * <p>
 * Created by ducanhnguyen on 5/26/2017.
 */
public class Console {
	public static final String LOAD_PROJECT = "-p";
	public static final String TESTED_FUNCTIONS = "-f";
	public static final String CONFIG = "-c";
	public static final String LOG4J_LEVEL = "-log4j";
	final static Logger logger = Logger.getLogger(Console.class);
	private ConsoleInput input = new ConsoleInput();

	public Console(String[] args) {
		Paths.START_FROM_COMMANDLINE = true;

		if (!new File(AbstractSetting.settingPath).exists()) {
			logger.info("Setting does not exist. Create!");
			Settingv2.create();
		}

		input = analyzeArgs(args);

		logger.info("Check the correctness of options");
		try {
			input.checkVariablesConfiguration();
			logger.info("OK. Start generating test data");
			try {
				input.findTestdata();
			} catch (Exception e) {
				logger.error("Error in generating test data");
				e.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Example: args = new String[] { Console.LOAD_PROJECT, new
	 * File(Paths.SYMBOLIC_EXECUTION_TEST).getCanonicalPath(),
	 * Console.TESTED_FUNCTIONS, "E:/workspace/java/cft4cpp-core/local/test.txt",
	 * Console.CONFIG, "E:/workspace/java/cft4cpp-core/local/setting.properties",
	 * Console.LOG4J_LEVEL, "debug" };
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		// [executable name] -p [project path] -f [function test] -c
		// [setting.properties path] -log4j "debug"

		/**
		 * AUTHOR: PLEASE CONFIGURE HERE
		 */
		String TESTING_PROJET_PATH = Paths.JOURNAL_TEST;
		String TESTING_FUNCTIONS_LIST = "D:/ICFT4Cpp2/cft4cpp-core/local/test.txt";
		String CONFIGURATION_FILE_PATH = "D:/ICFT4Cpp2/cft4cpp-core/local/setting.properties";
		/**
		 * AUTHOR: THE END OF CONFIGURATION
		 */
		args = new String[] { Console.LOAD_PROJECT, new File(TESTING_PROJET_PATH).getCanonicalPath(),
				Console.TESTED_FUNCTIONS, TESTING_FUNCTIONS_LIST, Console.CONFIG, CONFIGURATION_FILE_PATH,
				Console.LOG4J_LEVEL, "debug" };
		Console console = new Console(args);
		console.exportToHtml(new File(AbstractSetting.getValue(Settingv2.TEST_REPORT)), "xxx");
		
		System.out.println("DONE!");
				
		System.out.println("Output View file at " + AbstractSetting.getValue(Settingv2.TEST_REPORT));
		
		System.out.println("Input unit under test: D:\\ICFT4Cpp2\\cft4cpp-core\\local\\input.txt");
	}

	private ConsoleInput analyzeArgs(String[] args) {
		ConsoleInput input = new ConsoleInput();
		int count = 0;
		while (count < args.length - 1) {
			String current = args[count];
			String next = args[count + 1];

			switch (current) {
			case LOAD_PROJECT:
				input.projectFile = new File(next);
				count += 2;
				break;

			case CONFIG:
				input.variableConfigurationFile = new File(next);
				count += 2;
				break;

			case TESTED_FUNCTIONS:
				input.testFunctionsFile = new File(next);
				count += 2;
				break;

			case LOG4J_LEVEL:
				// Note: If there exists log4j configuration outside, this
				// setLevel is
				// useless!

				// A log request of level p in a logger with level
				// q is enabled if p >= q. This rule is at the
				// heart of log4j. It assumes that levels are
				// ordered. For the standard levels, we have ALL
				// < DEBUG < INFO < WARN < ERROR < FATAL < OFF.
				switch (next.toLowerCase()) {
				case "all":
					Logger.getRootLogger().setLevel(Level.ALL);
					break;
				case "debug":
					Logger.getRootLogger().setLevel(Level.DEBUG);
					break;
				case "error":
					Logger.getRootLogger().setLevel(Level.ERROR);
					break;
				case "info":
					Logger.getRootLogger().setLevel(Level.INFO);
					break;
				case "trace":
					Logger.getRootLogger().setLevel(Level.TRACE);
					break;
				case "warn":
					Logger.getRootLogger().setLevel(Level.WARN);
					break;
				case "off":
					Logger.getRootLogger().setLevel(Level.OFF);
					break;
				default:
					break;
				}
				count += 2;
				break;
			default:
				count++;
				break;
			}
		}
		return input;
	}

	public ConsoleInput getInput() {
		return input;
	}

	/**
	 * Get output in console. Each test data generation result of each function is
	 * stored in an element in the given list.
	 *
	 * @return
	 */
	public List<ConsoleOutput> getOutput() {
		return input.getOutput();
	}

	static String tableHeader = toRow(toCell("Name") + toCell("Coverage") + toCell("Bug num") + toCell("Total time")
			+ toCell("Test data execution") + toCell("Compilation") + toCell("Z3 Solver") + toCell("Symbolic execution")
			+ toCell("Normalization time") + toCell("(Macro normalization time)"));

	static String allFunctionsTestReport = "";

	public void exportToHtml(File htmlFile, String methodName) {
		String fullHtml = "";
		String style = "<style> table { font-size: 12px; font-family: arial, sans-serif; border-collapse: collapse; width: 100%; } td, th { border: 1px solid #dddddd; text-align: left; padding: 8px; } tr:nth-child(even) { background-color: #dddddd; } </style>";

		for (ConsoleOutput item : this.getOutput()) {
			String row = "";

			long totalTime = (item.getRunningTime() - item.getMacroNormalizationTime());

			row += toCell(methodName)
					+ toCell(item.getBranchCoverge() == 100f ? "100%"
							: "<p style=\"color:red;\">" + item.getBranchCoverge() + "</p>")
					+ toCell(item.getBugs().size() + "")//
					+ toCell(round(1.0f * totalTime / 1000) + " s")//
					+ toCell(round(1.0f * item.getExecutionTime() / 1000) + " s" + " (" + item.getNumOfExecutions()
							+ " executions)")
					+ toCell(round(1.0f * item.getMakeCommandRunningTime() / 1000) + " s" + " ("
							+ item.getMakeCommandRunningNumber() + " makes)")

					+ toCell(round(1.0f * item.getSolverRunningTime() / 1000) + " s" + " (" + item.getNumOfSolverCalls()
							+ " calls)")
					+ toCell(round(1.0f * item.getSymbolicExecutionTime() / 1000) + " s ("
							+ item.getNumOfSymbolicExecutions() + " times, " + item.getNumOfSymbolicStatements()
							+ " stms" + ")")
					+ toCell(round(1.0f * (item.getNormalizationTime() - item.getMacroNormalizationTime()) / 1000)
							+ " s")
					+ toCell(item.getMacroNormalizationTime() / 1000 + " second");

			allFunctionsTestReport += "<table>" + tableHeader + toRow(row) + "</table>" + "<pre>"
					+ item.getFunctionNode().getAST().getRawSignature() + "</pre>";
			allFunctionsTestReport += "</pre>";

			// display test data
			for (TestdataInReport testdata : item.getTestdata()) {
				String testdataStatus = "["
						+ (testdata.outputCompleteTestpath() == false ? "<b><font color='red'> incomplete </font></b>"
								: "<b><font color='blue'> complete </font></b>")
						+ "]&nbsp;";

				String testdataStr = testdata.getValue();
				if (testdata.isGeneratedBySDART())
					testdataStr = "<span style='background-color: green'>" + testdataStr + "</span>";
				else
					testdataStr = "<span >" + testdataStr + "</span>";

				String statementCov = "<br/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" + "Stm cov="
						+ testdata.getCurrentStatementCodeCoverage();

				String branchCov = "<br/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" + "Branch cov="
						+ testdata.getCurrentBranchCodeCoverage();

				List<String> stms = testdata.getTestpath()
						.getStandardTestpathByProperty(FunctionInstrumentationForStatementvsBranch_Marker.STATEMENT);
				String testpath = "<br/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"
						+ (stms.size() <= 50 ? stms : "Too long. size=" + stms.size()) + "<br/>";

				if (testdata.isHighlight())
					allFunctionsTestReport += "<span style=\"opacity:1\">" + testdataStatus + testdataStr + statementCov
							+ branchCov + testpath + "</span>";
				else
					allFunctionsTestReport += "<span style=\"opacity:0.5\">" + testdataStatus + testdataStr
							+ statementCov + branchCov + testpath + "</span>";
			}

		}
		// Summary of result
		for (ConsoleOutput item : this.getOutput()) {
			AbstractTestdataGeneration.totalNumOfExecution += item.getNumOfExecutions();
			AbstractTestdataGeneration.totalSolverCalls += item.getNumOfSolverCalls();
			AbstractTestdataGeneration.totalSymbolicStatements += item.getNumOfSymbolicStatements();
		}
		String summary = ("The number of SMT-Solver calls = " + AbstractTestdataGeneration.totalSolverCalls + "<br/>")
				+ ("The number of iterations =  " + AbstractTestdataGeneration.totalNumOfExecution + "<br/>")
				+ ("The number of statements performed symbolically: "
						+ AbstractTestdataGeneration.totalSymbolicStatements + "<br/>")
				+ (AbstractTestdataGeneration.numOfVisitedBranches + " visited branches / "
						+ AbstractTestdataGeneration.numOfBranches + " total branches");
		allFunctionsTestReport += "<br/>" + summary + "<br/>";

		// Add configuration
		IFunctionConfig conf = this.getOutput().get(0).getFunctionNode().getFunctionConfig();
		String config1 = "Character bound: [" + conf.getCharacterBound().getLower() + ".."
				+ conf.getCharacterBound().getUpper() + "]";
		String config2 = "Integer bound: [" + conf.getIntegerBound().getLower() + ".."
				+ conf.getIntegerBound().getUpper() + "]";
		String solver = "Solver: " + conf.getSolvingStrategy();
		String iterations = "Max iteration for each loop: " + conf.getMaximumInterationsForEachLoop();
		String sizeofArray = "Max size of array: " + conf.getSizeOfArray();

		// Help
		String testDataDescription = "";
		testDataDescription += "THIS FILE DESCRIBES THE PROCESS OF TEST DATA GENERATION<br/>";
		testDataDescription += "<h1>Test data description</h1></br>";
		testDataDescription += "<span>xxx</span> &nbsp;&nbsp;&nbsp;Test data is generated by DART<br/>";
		testDataDescription += "<span style='background-color: green'>xxx</span> &nbsp;&nbsp;&nbsp;Test data is generated by using SDART<br/>";
		testDataDescription += "<b><font color='red'> incomplete </font></b>&nbsp;&nbsp;&nbsp;The test data causes an exception<br/>";
		testDataDescription += "<b><font color='blue'> complete </font></b>&nbsp;&nbsp;&nbsp;The test data does not cause exceptions<br/>";

		testDataDescription += "<span style=\"opacity:1\">test data</span>&nbsp;&nbsp;&nbsp;This test data increases code coverage (opacity = 1)<br/>";
		testDataDescription += "<span style=\"opacity:0.5\">test data</span>&nbsp;&nbsp;&nbsp;This test data does not increase code coverage (opacity = 0.5)<br/>";
		testDataDescription += "<h1>CONFIGURATION</h1></br>";
		testDataDescription += "<br/>" + config1 + "<br/>" + config2 + "<br/>" + solver + "<br/>" + iterations + "<br/>"
				+ sizeofArray + "<br/>";
		testDataDescription += "Path selection strategy: " + Settingv2.getValue(Settingv2.PATH_SELECTION_STRATEGY)
				+ "<br/>";
		testDataDescription += "<h1>DETAILS</h1>";
		// Others
		String removeConstraints = "Removed constraints size = " + AbstractTestdataGeneration.removedConstraints
				+ "<br/>";
		String removeTestdata = "Removed test data size = " + AbstractTestdataGeneration.removedTestdata + "<br/>";
		allFunctionsTestReport += removeConstraints + removeTestdata;

		fullHtml = "<!DOCTYPE html> <html> <head>" + style + "</head><body>" + testDataDescription
				+ allFunctionsTestReport + "</body></html>";
		Utils.writeContentToFile(fullHtml, htmlFile.getAbsolutePath());

		// OTHER FILES
		// Present changes in visited branches over iterations
		String visitedBranchesChangesOverIterations = "";
		for (Integer[] iterationInfor : AbstractTestdataGeneration.visitedBranchesInfor) {
			Integer numVisitedBranches = iterationInfor[1];
			visitedBranchesChangesOverIterations += toRow(toCell(numVisitedBranches + ""));
		}
		visitedBranchesChangesOverIterations = "<table>" + visitedBranchesChangesOverIterations + "</table>";
		String iterationInforHtml = "<!DOCTYPE html> <html> <head>" + style + "</head><body>"
				+ visitedBranchesChangesOverIterations + "</body></html>";
		Utils.writeContentToFile(iterationInforHtml, htmlFile.getParent() + File.separator + "infor.html");
	}

	static String toCell(String content) {
		return "<td>" + content + "</td>";
	}

	static String toRow(String content) {
		return "<tr>" + content + "</tr>";
	}

	int toUpperRound(float n) {
		return (int) Math.ceil(n);
	}

	float round(float n) {
		String str = n + "";
		int posDot = str.indexOf(".");
		if (posDot == -1)
			return n;
		else {
			if (posDot + 1 == str.length() || posDot + 2 == str.length())
				return n;
			else
				return Float.parseFloat(str.substring(0, posDot + 2));
		}
	}
}
