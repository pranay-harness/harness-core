package migrations.all;

import static io.harness.rule.OwnerRule.ANSHUL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static software.wings.api.DeploymentType.SSH;
import static software.wings.beans.Account.Builder.anAccount;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.GcpKubernetesInfrastructureMapping.Builder.aGcpKubernetesInfrastructureMapping;
import static software.wings.service.impl.workflow.WorkflowServiceTestHelper.constructEcsWorkflow;
import static software.wings.service.impl.workflow.WorkflowServiceTestHelper.constructHelmRollbackWorkflowWithProperties;
import static software.wings.service.impl.workflow.WorkflowServiceTestHelper.constructHelmWorkflowWithProperties;
import static software.wings.sm.StateType.HELM_ROLLBACK;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.Environment;
import software.wings.beans.GraphNode;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowPhase;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.workflow.WorkflowServiceHelper;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.WorkflowService;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HelmStateTimeoutMigrationTest extends WingsBaseTest {
  private static final String steadyStateTimeout = "steadyStateTimeout";

  @Mock private InfrastructureMappingService infrastructureMappingService;
  @Mock private AppService appService;
  @Mock private AccountService accountService;
  @Mock private Application application;
  @Mock private Account account;
  @Mock private EnvironmentService environmentService;
  @Inject private WingsPersistence wingsPersistence;

  @InjectMocks @Inject private WorkflowServiceHelper workflowServiceHelper;
  @InjectMocks @Inject private WorkflowService workflowService;
  @InjectMocks @Inject private HelmStateTimeoutMigration helmStateTimeoutMigration;

  @Before
  public void setupMocks() {
    when(appService.get(APP_ID)).thenReturn(application);
    when(accountService.get(anyString())).thenReturn(account);
    when(environmentService.get(APP_ID, ENV_ID, false))
        .thenReturn(Environment.Builder.anEnvironment().uuid(ENV_ID).name(ENV_NAME).appId(APP_ID).build());
    when(appService.getAccountIdByAppId(APP_ID)).thenReturn(ACCOUNT_ID);
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID))
        .thenReturn(aGcpKubernetesInfrastructureMapping()
                        .withUuid(INFRA_MAPPING_ID)
                        .withServiceId(SERVICE_ID)
                        .withDeploymentType(SSH.name())
                        .withInfraMappingType(InfrastructureMappingType.GCP_KUBERNETES.name())
                        .withComputeProviderType(SettingVariableTypes.GCP.name())
                        .withAppId(APP_ID)
                        .build());
    wingsPersistence.save(anApplication().appId(APP_ID).uuid(APP_ID).accountId(ACCOUNT_ID).build());
    Account account = anAccount().withUuid(ACCOUNT_ID).withAccountName("ACCOUNT_NAME").build();
    wingsPersistence.save(account);
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testMigrationOfHelmWorkflowsWithTimeoutInMiliSeconds() {
    testMigrationOfHelmDeployWorkflow(600000, 10);
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testMigrationOfHelmWorkflowsWithTimeoutInMinutes() {
    testMigrationOfHelmDeployWorkflow(6000, 6000);
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testMigrationOfHelmWorkflowsWithInvalidTimeoutValue() {
    testMigrationOfHelmDeployWorkflow("6300", "6300");
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testMigrationOfHelmRollbackWorkflowsWithTimeoutInMiliSeconds() {
    testMigrationOfHelmRollbackWorkflow(600000, 10);
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testMigrationOfHelmRollbackWorkflowsWithTimeoutInMinutes() {
    testMigrationOfHelmRollbackWorkflow(6000, 6000);
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testMigrationOfWorkflowsWithNonHelmDeployState() {
    Workflow workflow = workflowService.createWorkflow(constructEcsWorkflow());
    assertThat(workflow).isNotNull();
    helmStateTimeoutMigration.migrate();

    GraphNode graphNode = fetchGraphNode(workflow);
    assertThat(graphNode.getProperties()).doesNotContainKeys(steadyStateTimeout);
  }

  private void testMigrationOfHelmDeployWorkflow(Object timeout, Object expectedTimeout) {
    Map<String, Object> properties = new HashMap<>();
    properties.put("steadyStateTimeout", timeout);

    Workflow workflow = workflowService.createWorkflow(constructHelmWorkflowWithProperties(properties));
    assertThat(workflow).isNotNull();
    helmStateTimeoutMigration.migrate();

    GraphNode graphNode = fetchGraphNode(workflow);
    assertThat(graphNode.getProperties()).containsKeys(steadyStateTimeout);
    assertThat(graphNode.getProperties().get(steadyStateTimeout)).isEqualTo(expectedTimeout);
  }

  private void testMigrationOfHelmRollbackWorkflow(Object timeout, Object expectedTimeout) {
    Map<String, Object> properties = new HashMap<>();
    properties.put("steadyStateTimeout", timeout);

    Workflow workflow = workflowService.createWorkflow(constructHelmRollbackWorkflowWithProperties(properties));
    assertThat(workflow).isNotNull();
    helmStateTimeoutMigration.migrate();

    GraphNode graphNode = fetchGraphNode(workflow);
    assertThat(graphNode.getProperties()).containsKeys(steadyStateTimeout);
    assertThat(graphNode.getProperties().get(steadyStateTimeout)).isEqualTo(expectedTimeout);

    graphNode = fetchRollbackGraphNode(workflow);
    assertThat(graphNode.getType()).isEqualTo(HELM_ROLLBACK.name());
    assertThat(graphNode.getProperties()).containsKeys(steadyStateTimeout);
    assertThat(graphNode.getProperties().get(steadyStateTimeout)).isEqualTo(expectedTimeout);
  }

  private GraphNode fetchGraphNode(Workflow workflow) {
    CanaryOrchestrationWorkflow canaryOrchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
    String nodeId =
        canaryOrchestrationWorkflow.getWorkflowPhases().get(0).getPhaseSteps().get(0).getSteps().get(0).getId();
    return workflowService.readGraphNode(workflow.getAppId(), workflow.getUuid(), nodeId);
  }

  private GraphNode fetchRollbackGraphNode(Workflow workflow) {
    workflow = workflowService.readWorkflow(workflow.getAppId(), workflow.getUuid());

    CanaryOrchestrationWorkflow canaryOrchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
    List<WorkflowPhase> workflowPhaseList =
        new ArrayList<>(canaryOrchestrationWorkflow.getRollbackWorkflowPhaseIdMap().values());

    return workflowPhaseList.get(0).getPhaseSteps().get(0).getSteps().get(0);
  }
}
