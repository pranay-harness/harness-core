package io.harness.functional;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import graphql.GraphQL;
import io.harness.CategoryTest;
import io.harness.beans.ExecutionStatus;
import io.harness.multiline.MultilineStringMixin;
import io.harness.rest.RestResponse;
import io.harness.rule.FunctionalTestRule;
import io.harness.rule.LifecycleRule;
import io.harness.testframework.framework.DelegateExecutor;
import io.harness.testframework.framework.Setup;
import io.harness.testframework.framework.utils.FileUtils;
import io.harness.testframework.graphql.GraphQLTestMixin;
import io.harness.testframework.restutils.PipelineRestUtils;
import io.harness.testframework.restutils.WorkflowRestUtils;
import io.restassured.RestAssured;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.dataloader.DataLoaderRegistry;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import software.wings.beans.Account;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.User;
import software.wings.beans.WorkflowExecution;
import software.wings.graphql.datafetcher.DataLoaderRegistryHelper;
import software.wings.service.intfc.WorkflowExecutionService;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.core.GenericType;

@Slf4j
public abstract class AbstractFunctionalTest extends CategoryTest implements GraphQLTestMixin, MultilineStringMixin {
  protected static final String ADMIN_USER = "admin@harness.io";

  protected static String bearerToken;
  @Rule public LifecycleRule lifecycleRule = new LifecycleRule();
  @Rule public FunctionalTestRule rule = new FunctionalTestRule(lifecycleRule.getClosingFactory());
  @Inject DataLoaderRegistryHelper dataLoaderRegistryHelper;

  @Override
  public DataLoaderRegistry getDataLoaderRegistry() {
    return dataLoaderRegistryHelper.getDataLoaderRegistry();
  }

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
    account = accountSetupService.ensureAccount();
    delegateExecutor.ensureDelegate(account, AbstractFunctionalTest.class);
    bearerToken = Setup.getAuthToken(ADMIN_USER, "admin");
    logger.info("Basic setup completed");
  }

  protected void resetCache() {
    RestResponse<User> userRestResponse = Setup.portal()
                                              .auth()
                                              .oauth2(bearerToken)
                                              .queryParam("accountId", account.getUuid())
                                              .put("/users/reset-cache")
                                              .as(new GenericType<RestResponse<User>>() {}.getType());
    assertThat(userRestResponse).isNotNull();
  }

  @AfterClass
  public static void cleanup() {
    FileUtils.deleteModifiedConfig(AbstractFunctionalTest.class);
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
}
