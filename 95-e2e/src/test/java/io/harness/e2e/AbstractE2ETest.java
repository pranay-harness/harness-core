package io.harness.e2e;

import com.google.inject.Inject;

import graphql.GraphQL;
import io.harness.CategoryTest;
import io.harness.beans.ExecutionStatus;
import io.harness.rule.LifecycleRule;
import io.harness.rule.LocalPortalTestRule;
import io.harness.testframework.framework.DelegateExecutor;
import io.harness.testframework.framework.Setup;
import io.harness.testframework.framework.constants.FrameworkConstants;
import io.harness.testframework.framework.utils.FileUtils;
import io.harness.testframework.framework.utils.TestUtils;
import io.harness.testframework.graphql.GraphQLTestMixin;
import io.harness.testframework.restutils.AccountRestUtils;
import io.harness.testframework.restutils.PipelineRestUtils;
import io.harness.testframework.restutils.WorkflowRestUtils;
import io.restassured.RestAssured;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import software.wings.beans.Account;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.WorkflowExecution;
import software.wings.service.intfc.WorkflowExecutionService;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class AbstractE2ETest extends CategoryTest implements GraphQLTestMixin {
  protected static String bearerToken;
  protected static String qaAccount1 = null;
  @Rule public LifecycleRule lifecycleRule = new LifecycleRule();
  @Rule public LocalPortalTestRule rule = new LocalPortalTestRule(lifecycleRule.getClosingFactory());

  @Override
  public GraphQL getGraphQL() {
    return rule.getGraphQL();
  }

  @BeforeClass
  public static void setup() {
    Setup.portal();
    RestAssured.useRelaxedHTTPSValidation();
  }

  //  @Inject private AccountGenerator accountGenerator;
  @Inject private DelegateExecutor delegateExecutor;
  //  @Inject OwnerManager ownerManager;
  @Inject private AccountSetupService accountSetupService;
  @Inject private WorkflowExecutionService workflowExecutionService;

  @Getter static Account account;

  @Before
  public void testSetup() throws IOException {
    switch (TestUtils.getExecutionEnvironment()) {
      case FrameworkConstants.LOCAL_ENV:
        logger.info("Setup and Tests running against Local environment");
        doLocalSetup();
        break;
      case FrameworkConstants.QA_ENV:
        logger.info("Setup and Tests running against QA environment");
        doQASetup();
        break;
      default:
        logger.error("Unknown setup detected to run the test");
        System.exit(1);
    }
  }

  @AfterClass
  public static void cleanup() {
    FileUtils.deleteModifiedConfig(AbstractE2ETest.class);
    logger.info("All tests exit");
  }

  public WorkflowExecution runWorkflow(String bearerToken, String appId, String envId, ExecutionArgs executionArgs) {
    WorkflowExecution original = WorkflowRestUtils.startWorkflow(bearerToken, appId, envId, executionArgs);

    Awaitility.await().atMost(120, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
      final WorkflowExecution workflowExecution =
          workflowExecutionService.getWorkflowExecution(appId, original.getUuid());
      return workflowExecution != null && ExecutionStatus.isFinalStatus(workflowExecution.getStatus());
    });

    return workflowExecutionService.getWorkflowExecution(appId, original.getUuid());
  }

  public WorkflowExecution runPipeline(String bearerToken, String appId, String envId, ExecutionArgs executionArgs) {
    WorkflowExecution original = PipelineRestUtils.startPipeline(bearerToken, appId, envId, executionArgs);

    Awaitility.await().atMost(120, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
      final WorkflowExecution workflowExecution =
          workflowExecutionService.getWorkflowExecution(appId, original.getUuid());
      return workflowExecution != null && ExecutionStatus.isFinalStatus(workflowExecution.getStatus());
    });

    return workflowExecutionService.getWorkflowExecution(appId, original.getUuid());
  }

  private void doLocalSetup() throws IOException {
    account = accountSetupService.ensureAccount();
    delegateExecutor.ensureDelegate(account, AbstractE2ETest.class);
    bearerToken = Setup.getAuthToken("admin@harness.io", "admin");
  }

  private void doQASetup() {
    qaAccount1 = TestUtils.getDecryptedValue("e2etest_qa_account1");
    bearerToken = Setup.getAuthToken("autouser1@harness.io", TestUtils.getDecryptedValue("e2etest_autouser_password"));
    account = AccountRestUtils.getAccount(qaAccount1, bearerToken);
  }
}
