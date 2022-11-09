package console;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import config.AbstractSetting;
import config.FunctionConfig;
import config.IFunctionConfig;
import config.ISettingv2;
import config.ParameterBound;
import config.Paths;
import parser.projectparser.ProjectParser;
import testdatagen.AbstractTestdataGeneration;
import testdatagen.FastTestdataGeneration;
import testdatagen.ITestdataGeneration;
import tree.object.FunctionNode;
import tree.object.IFunctionNode;
import tree.object.INode;
import tree.object.IProjectNode;
import utils.Utils;
import utils.search.FunctionNodeCondition;
import utils.search.Search;

/**
 * Created by ducanhnguyen on 5/26/2017.
 */
public class ConsoleInput {
	final static Logger logger = Logger.getLogger(ConsoleInput.class);
	protected File projectFile;
	protected File variableConfigurationFile;
	protected File testFunctionsFile;
	protected int coverage;
	protected List<ConsoleOutput> output = new ArrayList<ConsoleOutput>();
	protected List<String> unreachCoverageMethods = new ArrayList<>();
	protected List<String> overCoverageMethods = new ArrayList<>();
	protected List<String> unreachNumTestpaths = new ArrayList<>();
	protected List<String> exceptionMethods = new ArrayList<>();

	public void findTestdata() throws Exception {
		FunctionConfig functionConfig = loadVariablesConfiguration(variableConfigurationFile);

		File cloneProject = Utils.copy(projectFile.getCanonicalPath());
		Paths.CURRENT_PROJECT.CLONE_PROJECT_PATH = cloneProject.getAbsolutePath();
		logger.info("Create a clone project done of project " + projectFile.getCanonicalPath());

		IProjectNode root = new ProjectParser(cloneProject, null).getRootTree();

		for (String function : loadTestedFunctions(testFunctionsFile)) {
			logger.info("");
			logger.info("");
			logger.info("Function: " + function);
			List<INode> functionNodes = Search.searchNodes(root, new FunctionNodeCondition(), function);

			if (functionNodes.size() == 1) {
				logger.info("Founded only a function");
				IFunctionNode functionNode = (IFunctionNode) functionNodes.get(0);
				((FunctionNode) functionNode).setFunctionConfig(functionConfig);
				logger.info("Function: \n" + functionNode.getAST().getRawSignature());

				ConsoleOutput consoleOutput = generateTestdata(functionNode);
				consoleOutput.setFunctionNode(functionNode);
				output.add(consoleOutput);

			} else if (functionNodes.size() == 0)
				logger.error("Function dont exist");

			else if (functionNodes.size() > 1)
				logger.error("Find many functions. Ignore.");
		}

		logger.debug("Delete the clone project");
		Utils.deleteFileOrFolder(cloneProject);
	}

	private ConsoleOutput generateTestdata(IFunctionNode function) {
		if (logger.isDebugEnabled())
			logger.debug("Start generating test data for the current function");

		ConsoleOutput consoleOutput = new ConsoleOutput();

		try {
			AbstractTestdataGeneration mars = null;
			switch (AbstractSetting.getValue(ISettingv2.TESTDATA_STRATEGY)) {
			case ITestdataGeneration.TESTDATA_GENERATION_STRATEGIES.FAST_MARS + "":
				mars = new FastTestdataGeneration(function);
				break;
			default:
				throw new Exception("Wrong test data generation strategy");
			}

			if (mars != null) {
				mars.generateTestdata();

				consoleOutput.setRunningTime(AbstractTestdataGeneration.totalRunningTime);
				consoleOutput.setSolverRunningTime(AbstractTestdataGeneration.solverRunningTime);
				consoleOutput.setNormalizationTime(AbstractTestdataGeneration.normalizationTime);
				consoleOutput.setSymbolicExecutionTime(AbstractTestdataGeneration.symbolicExecutionTime);
				consoleOutput.setExecutionTime(AbstractTestdataGeneration.executionTime);
				consoleOutput.setMakeCommandRunningTime(AbstractTestdataGeneration.makeCommandRunningTime);
				consoleOutput.setMakeCommandRunningNumber(AbstractTestdataGeneration.makeCommandRunningNumber);
				consoleOutput.setNumOfSolverCalls(AbstractTestdataGeneration.numOfSolverCalls);
				consoleOutput.setNumOfSymbolicExecutions(AbstractTestdataGeneration.numOfSymbolicExecutions);
				consoleOutput.setNumOfSymbolicStatements(AbstractTestdataGeneration.numOfSymbolicStatements);
				consoleOutput.setNumOfExecutions(AbstractTestdataGeneration.numOfExecutions);
				consoleOutput.setNumOfNoChangeToCoverage(AbstractTestdataGeneration.numOfNotChangeToCoverage);
				consoleOutput
						.setNumOfSolverCallsbutCannotSolve(AbstractTestdataGeneration.numOfSolverCallsbutCannotSolve);
				consoleOutput.setMacroNormalizationTime(AbstractTestdataGeneration.macroNormalizationTime);
				consoleOutput.setBugs(AbstractTestdataGeneration.bugs);
				consoleOutput.setBranchCoverge(AbstractTestdataGeneration.branchCoverage);
				consoleOutput.setStatementCoverge(AbstractTestdataGeneration.statementCoverage);
				// consoleOutput.setLog(Utils.readFileContent("E:\\log4j-application.log"));

				consoleOutput.setTestdata(AbstractTestdataGeneration.testdata);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return consoleOutput;
	}

	public boolean checkVariablesConfiguration() throws Exception {
		if (projectFile == null)
			throw new Exception("Project folder is null");
		if (variableConfigurationFile == null)
			throw new Exception("Variable configuration file is null");
		if (testFunctionsFile == null)
			throw new Exception("Tested functions file is null");
		if (!variableConfigurationFile.exists())
			throw new Exception("Variable configuration file does not exist");
		if (!projectFile.exists())
			throw new Exception("Project file does not exist");
		if (!testFunctionsFile.exists())
			throw new Exception("Tested function file does not exist");

		if (!new File(AbstractSetting.getValue(ISettingv2.SOLVER_Z3_PATH)).exists())
			throw new Exception("Wrong path of SMT-Solver Z3");
		if (!new File(AbstractSetting.getValue(ISettingv2.MCPP_EXE_PATH)).exists())
			throw new Exception("Wrong path of mcpp.exe");
		if (!new File(AbstractSetting.getValue(ISettingv2.GNU_GCC_PATH)).exists())
			throw new Exception("Wrong path of gcc.exe");
		if (!new File(AbstractSetting.getValue(ISettingv2.GNU_GPlusPlus_PATH)).exists())
			throw new Exception("Wrong path of g++.exe");

		// TODO: may need some checkings on variables configuration
		return true;
	}

	private FunctionConfig loadVariablesConfiguration(File config) throws IOException {
		logger.info("Load variables configuration from file at " + config.getCanonicalPath());
		FunctionConfig functionConfig = new FunctionConfig();
		functionConfig.setCharacterBound(
				new ParameterBound(Utils.toInt(AbstractSetting.getValue(ISettingv2.DEFAULT_CHARACTER_LOWER_BOUND)),
						Utils.toInt(AbstractSetting.getValue(ISettingv2.DEFAULT_CHARACTER_UPPER_BOUND))));
		functionConfig.setIntegerBound(
				new ParameterBound(Utils.toInt(AbstractSetting.getValue(ISettingv2.DEFAULT_NUMBER_LOWER_BOUND)),
						Utils.toInt(AbstractSetting.getValue(ISettingv2.DEFAULT_NUMBER_UPPER_BOUND))));
		functionConfig.setSizeOfArray(Utils.toInt(AbstractSetting.getValue(ISettingv2.DEFAULT_TEST_ARRAY_SIZE)));
		functionConfig.setMaximumInterationsForEachLoop(
				Utils.toInt(AbstractSetting.getValue(ISettingv2.MAX_ITERATION_FOR_EACH_LOOP)));
		functionConfig.setSolvingStrategy(AbstractSetting.getValue(ISettingv2.SELECTED_SOLVING_STRATEGY));

		String coverage = AbstractSetting.getValue(ISettingv2.COVERAGE);
		if (coverage.equals(ISettingv2.SUPPORT_COVERAGE_CRITERIA[0]))
			functionConfig.setTypeofCoverage(IFunctionConfig.BRANCH_COVERAGE);
		else if (coverage.equals(ISettingv2.SUPPORT_COVERAGE_CRITERIA[1]))
			functionConfig.setTypeofCoverage(IFunctionConfig.STATEMENT_COVERAGE);
		else if (coverage.equals(ISettingv2.SUPPORT_COVERAGE_CRITERIA[2]))
			functionConfig.setTypeofCoverage(IFunctionConfig.SUBCONDITION);

		// only for testing
		logger.info("Character bound: " + functionConfig.getCharacterBound().toString());
		logger.info("Integer bound: " + functionConfig.getIntegerBound().toString());
		logger.info("Max loop:" + functionConfig.getMaximumInterationsForEachLoop());
		logger.info("Max size of array:" + functionConfig.getSizeOfArray());
		if (functionConfig.getTypeofCoverage() == IFunctionConfig.BRANCH_COVERAGE)
			logger.info("Coverage: branch");
		else if (functionConfig.getTypeofCoverage() == IFunctionConfig.STATEMENT_COVERAGE)
			logger.info("Coverage: statement");
		else if (functionConfig.getTypeofCoverage() == IFunctionConfig.SUBCONDITION)
			logger.info("Coverage: sub-Condition");

		logger.info("Solving strategy: " + functionConfig.getSolvingStrategy());
		logger.info("Test data generation: " + AbstractSetting.getValue(ISettingv2.TESTDATA_STRATEGY));
		return functionConfig;
	}

	private String[] loadTestedFunctions(File fFunctions) throws Exception {
		logger.info("Load tested functions from file at " + fFunctions.getCanonicalPath());
		return Utils.readFileContent(fFunctions.getCanonicalPath()).replace("\r", "").split("\n");
	}

	public List<ConsoleOutput> getOutput() {
		return output;
	}

	protected class ExpectedOutput {

		int nTestPath;
		float reachCoverage;

		public ExpectedOutput(int nTestPath, float reachCoverage) {
			super();
			this.nTestPath = nTestPath;
			this.reachCoverage = reachCoverage;
		}

		public int getnTestPath() {
			return nTestPath;
		}

		public float getReachCoverage() {
			return reachCoverage;
		}
	}
}
