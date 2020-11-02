package io.harness.functional;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.persistence.HQuery.excludeAuthority;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.domain.Sort.Direction;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;
import static software.wings.sm.StateExecutionInstance.StateExecutionInstanceKeys;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import graphql.ExecutionInput;
import graphql.GraphQL;
import graphql.GraphQLContext;
import io.harness.CategoryTest;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.WorkflowType;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.execution.PlanExecution;
import io.harness.execution.PlanExecution.PlanExecutionKeys;
import io.harness.interrupts.Interrupt;
import io.harness.interrupts.Interrupt.InterruptKeys;
import io.harness.multiline.MultilineStringMixin;
import io.harness.rest.RestResponse;
import io.harness.rule.FunctionalTestRule;
import io.harness.rule.LifecycleRule;
import io.harness.testframework.framework.CommandLibraryServiceExecutor;
import io.harness.testframework.framework.DelegateExecutor;
import io.harness.testframework.framework.Setup;
import io.harness.testframework.framework.utils.FileUtils;
import io.harness.testframework.graphql.GraphQLTestMixin;
import io.harness.testframework.restutils.ArtifactStreamRestUtils;
import io.harness.testframework.restutils.PipelineRestUtils;
import io.harness.testframework.restutils.WorkflowRestUtils;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.dataloader.DataLoaderRegistry;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import software.wings.api.DeploymentType;
import software.wings.api.PhaseStepExecutionData;
import software.wings.beans.Account;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.ExecutionCredential;
import software.wings.beans.ExecutionCredential.ExecutionType;
import software.wings.beans.FeatureName;
import software.wings.beans.SSHExecutionCredential;
import software.wings.beans.User;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.Instance.InstanceKeys;
import software.wings.beans.security.UserGroup;
import software.wings.dl.WingsPersistence;
import software.wings.graphql.datafetcher.DataLoaderRegistryHelper;
import software.wings.security.UserPermissionInfo;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.analysis.AnalysisContext.AnalysisContextKeys;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.InfrastructureDefinitionService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.UserService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.instance.InstanceService;
import software.wings.service.intfc.instance.ServerlessInstanceService;
import software.wings.sm.PhaseStepExecutionSummary;
import software.wings.sm.StateExecutionInstance;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
  @Inject @Named("orchestrationMongoTemplate") private MongoTemplate mongoTemplate;
  @Inject CommandLibraryServiceExecutor commandLibraryServiceExecutor;
  @Inject FeatureFlagService featureFlagService;
  @Inject private InstanceService instanceService;
  @Inject InfrastructureMappingService infrastructureMappingService;
  @Inject ServerlessInstanceService serverlessInstanceService;
  @Inject InfrastructureDefinitionService infrastructureDefinitionService;

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

  @Inject private DelegateExecutor delegateExecutor;
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
      commandLibraryServiceExecutor.ensureCommandLibraryService(
          AbstractFunctionalTest.class, FunctionalTestRule.alpn, FunctionalTestRule.alpnJar);
    }
    log.info("Basic setup completed");
  }

  @AfterClass
  public static void cleanup() {
    FileUtils.deleteModifiedConfig(AbstractFunctionalTest.class);
    log.info("All tests exit");
  }

  public void resetCache(String accountId) {
    RestResponse<Void> restResponse =
        Setup.portal()
            .auth()
            .oauth2(bearerToken)
            .queryParam("accountId", accountId)
            //            .body(null, ObjectMapperType.GSON)
            .put("/users/reset-cache")
            .as(new GenericType<RestResponse<Void>>() {}.getType(), ObjectMapperType.GSON);
    log.info(restResponse.toString());
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

  public PlanExecution executePlan(String bearerToken, String accountId, String appId, String planType) {
    PlanExecution original = startPlanExecution(bearerToken, accountId, appId, planType);

    final String finalStatusEnding = "ED";
    Awaitility.await().atMost(5, TimeUnit.MINUTES).pollInterval(10, TimeUnit.SECONDS).until(() -> {
      final PlanExecution planExecution = getPlanExecution(original.getUuid());
      return planExecution != null && planExecution.getStatus().name().endsWith(finalStatusEnding);
    });

    return getPlanExecution(original.getUuid());
  }

  protected void logStateExecutionInstanceErrors(WorkflowExecution workflowExecution) {
    if (workflowExecution != null && workflowExecution.getStatus() != ExecutionStatus.FAILED) {
      log.info("Workflow execution didn't failed, skipping this step");
      return;
    }

    List<StateExecutionInstance> stateExecutionInstances =
        wingsPersistence.createQuery(StateExecutionInstance.class)
            .filter(StateExecutionInstanceKeys.accountId, workflowExecution.getAccountId())
            .filter(StateExecutionInstanceKeys.executionUuid, workflowExecution.getUuid())
            .filter(StateExecutionInstanceKeys.status, ExecutionStatus.FAILED)
            .asList();

    if (isEmpty(stateExecutionInstances)) {
      log.info("No FAILED state execution instances found for workflow {}", workflowExecution.getUuid());
      return;
    }

    stateExecutionInstances.stream()
        .map(stateExecutionInstance -> stateExecutionInstance.getStateExecutionMap().values())
        .flatMap(Collection::stream)
        .filter(stateExecutionData
            -> stateExecutionData.getStatus() == ExecutionStatus.FAILED
                || stateExecutionData.getStatus() == ExecutionStatus.ERROR)
        .forEach(stateExecutionData -> {
          log.info("Analyzing failed state: {}", stateExecutionData.getStateName());
          if (isNotEmpty(stateExecutionData.getErrorMsg())) {
            log.info(
                "State: {} failed with error: {}", stateExecutionData.getStateName(), stateExecutionData.getErrorMsg());
          } else {
            log.info("No error message found for state: {}, checking phase execution summary...",
                stateExecutionData.getStateName());
          }
          if (stateExecutionData instanceof PhaseStepExecutionData) {
            PhaseStepExecutionData phaseStepExecutionData = (PhaseStepExecutionData) stateExecutionData;
            PhaseStepExecutionSummary phaseStepExecutionSummary = phaseStepExecutionData.getPhaseStepExecutionSummary();
            if (phaseStepExecutionSummary != null) {
              phaseStepExecutionSummary.getStepExecutionSummaryList()
                  .stream()
                  .filter(stepExecutionSummary
                      -> stepExecutionSummary.getStatus() == ExecutionStatus.ERROR
                          || stepExecutionSummary.getStatus() == ExecutionStatus.FAILED)
                  .forEach(stepExecutionSummary
                      -> log.info("Phase step execution failed at state: {} and step name: {} with message: {}",
                          stateExecutionData.getStateName(), stepExecutionSummary.getStepName(),
                          stepExecutionSummary.getMessage()));
            }
          } else {
            log.info(
                "No phase step execution summary found for state: {}. ¯\\_(ツ)_/¯", stateExecutionData.getStateName());
          }

          log.info("Analysis completed for failed state: {}", stateExecutionData.getStateName());
        });
  }

  private PlanExecution startPlanExecution(String bearerToken, String accountId, String appId, String planType) {
    GenericType<RestResponse<PlanExecution>> returnType = new GenericType<RestResponse<PlanExecution>>() {};

    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("accountId", accountId);
    queryParams.put("appId", appId);

    RestResponse<PlanExecution> response = Setup.portal()
                                               .auth()
                                               .oauth2(bearerToken)
                                               .queryParams(queryParams)
                                               .contentType(ContentType.JSON)
                                               .get("/execute2/" + planType)
                                               .as(returnType.getType());

    return response.getResource();
  }

  public PlanExecution getPlanExecution(String uuid) {
    Query query = query(where(PlanExecutionKeys.uuid).is(uuid));
    query.fields().include(PlanExecutionKeys.status);
    return mongoTemplate.findOne(query, PlanExecution.class);
  }

  public List<NodeExecution> getNodeExecutions(String planExecutionId) {
    Query query = query(where(NodeExecutionKeys.planExecutionId).is(planExecutionId))
                      .with(Sort.by(Direction.DESC, NodeExecutionKeys.createdAt));
    return mongoTemplate.find(query, NodeExecution.class);
  }

  public List<Interrupt> getPlanInterrupts(String planExecutionId) {
    return mongoTemplate.find(query(where(InterruptKeys.planExecutionId).is(planExecutionId)), Interrupt.class);
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

  protected void logFeatureFlagsEnabled(String accountId) {
    for (FeatureName featureName : FeatureName.values()) {
      if (featureFlagService.isEnabled(featureName, accountId)) {
        log.info("[ENABLED_FEATURE_FLAG]: {}", featureName);
      }
    }
  }

  protected void assertExecution(Workflow savedWorkflow, String appId, String envId) {
    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setWorkflowType(savedWorkflow.getWorkflowType());

    String artifactId = ArtifactStreamRestUtils.getArtifactStreamId(
        bearerToken, appId, savedWorkflow.getEnvId(), savedWorkflow.getServiceId());
    Artifact artifact = new Artifact();
    artifact.setUuid(artifactId);

    executionArgs.setArtifacts(Arrays.asList(artifact));
    executionArgs.setOrchestrationId(savedWorkflow.getUuid());
    executionArgs.setExecutionCredential(SSHExecutionCredential.Builder.aSSHExecutionCredential()
                                             .withExecutionType(ExecutionCredential.ExecutionType.SSH)
                                             .build());

    log.info("Invoking workflow execution");

    WorkflowExecution workflowExecution = runWorkflow(bearerToken, appId, envId, executionArgs);
    logStateExecutionInstanceErrors(workflowExecution);
    assertThat(workflowExecution).isNotNull();
    log.info("Waiting for execution to finish");
    assertInstanceCount(workflowExecution.getStatus(), appId, workflowExecution.getInfraMappingIds().get(0),
        workflowExecution.getInfraDefinitionIds().get(0));

    log.info("ECs Execution status: " + workflowExecution.getStatus());
    assertThat(workflowExecution.getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
  }

  protected void assertInstanceCount(
      ExecutionStatus workflowExecutionStatus, String appId, String infraMappingId, String infraDefinitionId) {
    if (workflowExecutionStatus != ExecutionStatus.SUCCESS) {
      return;
    }
    DeploymentType deploymentType = infrastructureDefinitionService.get(appId, infraDefinitionId).getDeploymentType();
    assertThat(getActiveInstancesConditional(appId, infraMappingId, deploymentType)).isGreaterThanOrEqualTo(1);
  }

  private long getActiveInstancesConditional(String appId, String infraMappingId, DeploymentType deploymentType) {
    Awaitility.await().atMost(5, TimeUnit.MINUTES).pollInterval(5, TimeUnit.SECONDS).until(() -> {
      if (deploymentType == DeploymentType.AWS_LAMBDA) {
        return serverlessInstanceService.list(infraMappingId, appId).size() >= 1;
      } else {
        return instanceService.getInstanceCount(appId, infraMappingId) >= 1;
      }
    });
    if (deploymentType == DeploymentType.AWS_LAMBDA) {
      return serverlessInstanceService.list(infraMappingId, appId).size();
    } else {
      return instanceService.getInstanceCount(appId, infraMappingId);
    }
  }

  protected List<Instance> getActiveInstancesConditional(String appId, String serviceId, String infraMappingId) {
    Awaitility.await()
        .atMost(3, TimeUnit.MINUTES)
        .pollInterval(5, TimeUnit.SECONDS)
        .until(() -> getActiveInstances(appId, serviceId, infraMappingId).size() > 0);
    return getActiveInstances(appId, serviceId, infraMappingId);
  }

  private List<Instance> getActiveInstances(String appId, String serviceId, String infraMappingId) {
    return wingsPersistence.createQuery(Instance.class)
        .filter(InstanceKeys.appId, appId)
        .filter(InstanceKeys.infraMappingId, infraMappingId)
        .filter(InstanceKeys.serviceId, serviceId)
        .filter(InstanceKeys.isDeleted, Boolean.FALSE)
        .asList();
  }
}
