package io.harness.functional;

import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.rule.FunctionalTestRule.alpn;
import static io.harness.rule.FunctionalTestRule.alpnJar;

import com.google.inject.Inject;

import graphql.ExecutionInput;
import graphql.GraphQL;
import graphql.GraphQLContext;
import io.harness.CategoryTest;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.WorkflowType;
import io.harness.multiline.MultilineStringMixin;
import io.harness.rest.RestResponse;
import io.harness.rule.FunctionalTestRule;
import io.harness.rule.LifecycleRule;
import io.harness.testframework.framework.CommandLibraryServiceExecutor;
import io.harness.testframework.framework.DelegateExecutor;
import io.harness.testframework.framework.Setup;
import io.harness.testframework.framework.utils.FileUtils;
import io.harness.testframework.graphql.GraphQLTestMixin;
import io.harness.testframework.restutils.PipelineRestUtils;
import io.harness.testframework.restutils.WorkflowRestUtils;
import io.restassured.RestAssured;
import io.restassured.mapper.ObjectMapperType;
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
import software.wings.beans.ExecutionCredential.ExecutionType;
import software.wings.beans.SSHExecutionCredential;
import software.wings.beans.User;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.security.UserGroup;
import software.wings.dl.WingsPersistence;
import software.wings.graphql.datafetcher.DataLoaderRegistryHelper;
import software.wings.security.UserPermissionInfo;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.analysis.AnalysisContext.AnalysisContextKeys;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.UserService;
import software.wings.service.intfc.WorkflowExecutionService;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.core.GenericType;

@Slf4j
public abstract class AbstractFunctionalTest extends CategoryTest implements GraphQLTestMixin, MultilineStringMixin {
  protected static final String ADMIN_USER = "admin@harness.io";

  protected static String bearerToken;
  protected static User adminUser;

  @Rule public LifecycleRule lifecycleRule = new LifecycleRule();
  @Rule public FunctionalTestRule rule = new FunctionalTestRule(lifecycleRule.getClosingFactory());
  @Inject DataLoaderRegistryHelper dataLoaderRegistryHelper;
  @Inject AuthHandler authHandler;
  @Inject private WingsPersistence wingsPersistence;
  @Inject CommandLibraryServiceExecutor commandLibraryServiceExecutor;

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
  @Inject private UserService userService;

  @Getter static Account account;

  @Before
  public void testSetup() throws IOException {
    account = accountSetupService.ensureAccount();
    adminUser = Setup.loginUser(ADMIN_USER, "admin");
    bearerToken = adminUser.getToken();
    delegateExecutor.ensureDelegate(account, bearerToken, AbstractFunctionalTest.class);
    if (needCommandLibraryService()) {
      commandLibraryServiceExecutor.ensureCommandLibraryService(AbstractFunctionalTest.class, alpn, alpnJar);
    }
    logger.info("Basic setup completed");
  }

  @AfterClass
  public static void cleanup() {
    FileUtils.deleteModifiedConfig(AbstractFunctionalTest.class);
    logger.info("All tests exit");
  }

  public void resetCache(String accountId) {
    //    Awaitility.await()
    //        .atMost(120, TimeUnit.SECONDS)
    //        .pollInterval(5, TimeUnit.SECONDS)
    //        .until(()
    //            -> Setup.portal()
    //            .auth()
    //            .oauth2(bearerToken)
    //            .put("/users/reset-cache")
    //            .jsonPath()
    //            .equals(ExecutionStatus.SUCCESS.name()));

    RestResponse<Void> restResponse =
        Setup.portal()
            .auth()
            .oauth2(bearerToken)
            .queryParam("accountId", accountId)
            //            .body(null, ObjectMapperType.GSON)
            .put("/users/reset-cache")
            .as(new GenericType<RestResponse<Void>>() {}.getType(), ObjectMapperType.GSON);
    System.out.println(restResponse);
  }

  public static Void updateApiKey(String accountId, String bearerToken) {
    RestResponse<Void> restResponse =
        Setup.portal()
            .auth()
            .oauth2(bearerToken)
            .queryParam("accountId", accountId)
            //            .body(null, ObjectMapperType.GSON)
            .put("/users/reset-cache")
            .as(new GenericType<RestResponse<Void>>() {}.getType(), ObjectMapperType.GSON);
    return restResponse.getResource();
  }

  public WorkflowExecution runWorkflow(
      String bearerToken, String appId, String envId, String orchestrationId, List<Artifact> artifactList) {
    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setWorkflowType(WorkflowType.ORCHESTRATION);
    executionArgs.setExecutionCredential(
        SSHExecutionCredential.Builder.aSSHExecutionCredential().withExecutionType(ExecutionType.SSH).build());
    executionArgs.setOrchestrationId(orchestrationId);
    executionArgs.setArtifacts(artifactList);

    return getWorkflowExecution(bearerToken, appId, envId, executionArgs);
  }

  public AnalysisContext runWorkflowWithVerification(
      String bearerToken, String appId, String envId, String orchestrationId, List<Artifact> artifactList) {
    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setWorkflowType(WorkflowType.ORCHESTRATION);
    executionArgs.setExecutionCredential(
        SSHExecutionCredential.Builder.aSSHExecutionCredential().withExecutionType(ExecutionType.SSH).build());
    executionArgs.setOrchestrationId(orchestrationId);
    executionArgs.setArtifacts(artifactList);

    return getWorkflowExecutionWithVerification(bearerToken, appId, envId, executionArgs);
  }

  public WorkflowExecution getWorkflowExecution(
      String bearerToken, String appId, String envId, ExecutionArgs executionArgs) {
    WorkflowExecution original = WorkflowRestUtils.startWorkflow(bearerToken, appId, envId, executionArgs);

    Awaitility.await().atMost(15, TimeUnit.MINUTES).pollInterval(5, TimeUnit.SECONDS).until(() -> {
      final WorkflowExecution workflowExecution =
          workflowExecutionService.getWorkflowExecution(appId, original.getUuid());
      return workflowExecution != null && ExecutionStatus.isFinalStatus(workflowExecution.getStatus());
    });

    return workflowExecutionService.getWorkflowExecution(appId, original.getUuid());
  }

  private AnalysisContext getWorkflowExecutionWithVerification(
      String bearerToken, String appId, String envId, ExecutionArgs executionArgs) {
    WorkflowExecution original = WorkflowRestUtils.startWorkflow(bearerToken, appId, envId, executionArgs);

    final AnalysisContext[] analysisContext = new AnalysisContext[1];
    Awaitility.await().atMost(15, TimeUnit.MINUTES).pollInterval(5, TimeUnit.SECONDS).until(() -> {
      analysisContext[0] = wingsPersistence.createQuery(AnalysisContext.class, excludeAuthority)
                               .filter(AnalysisContextKeys.workflowExecutionId, original.getUuid())
                               .get();
      return analysisContext[0] != null;
    });

    return analysisContext[0];
  }

  public WorkflowExecution runWorkflow(String bearerToken, String appId, String envId, ExecutionArgs executionArgs) {
    return getWorkflowExecution(bearerToken, appId, envId, executionArgs);
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

  @Override
  public ExecutionInput getExecutionInput(String query, String accountId) {
    User user = User.Builder.anUser().uuid("user1Id").build();
    UserGroup userGroup = authHandler.buildDefaultAdminUserGroup(accountId, user);
    UserPermissionInfo userPermissionInfo =
        authHandler.evaluateUserPermissionInfo(accountId, Arrays.asList(userGroup), user);
    return ExecutionInput.newExecutionInput()
        .query(query)
        .dataLoaderRegistry(getDataLoaderRegistry())
        .context(GraphQLContext.newContext().of("accountId", accountId, "permissions", userPermissionInfo))
        .build();
  }

  protected boolean needCommandLibraryService() {
    return false;
  }
}
