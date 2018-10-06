package migrations.all;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static software.wings.api.DeploymentType.SSH;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.GcpKubernetesInfrastructureMapping.Builder.aGcpKubernetesInfrastructureMapping;
import static software.wings.service.impl.workflow.WorkflowServiceTestHelper.constructEcsWorkflow;
import static software.wings.service.impl.workflow.WorkflowServiceTestHelper.constructHelmWorkflowWithProperties;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;

import com.google.inject.Inject;

import org.junit.Before;
import org.junit.Test;
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
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.workflow.WorkflowServiceHelper;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.WorkflowService;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.HashMap;
import java.util.Map;

public class HelmReleaseNameSuffixMigrationTest extends WingsBaseTest {
  private static final String HELM_RELEASE_NAME_PREFIX_KEY = "helmReleaseNamePrefix";
  private static final String HELM_RELEASE_NAME_PREFIX_DEFAULT_VALUE = "${app.name}-${service.name}-${env.name}";
  private static final String HELM_RELEASE_NAME_SUFFIX_VALUE = "-harness-${infra.helm.shortId}";

  @Mock private InfrastructureMappingService infrastructureMappingService;
  @Mock private AppService appService;
  @Mock private AccountService accountService;
  @Mock private Application application;
  @Mock private Account account;
  @Mock private EnvironmentService environmentService;
  @Inject private WingsPersistence wingsPersistence;

  @InjectMocks @Inject private WorkflowServiceHelper workflowServiceHelper;
  @InjectMocks @Inject private WorkflowService workflowService;
  @InjectMocks @Inject private HelmReleaseNameSuffixMigration helmReleaseNameSuffixMigration;

  @Before
  public void setupMocks() {
    when(appService.get(APP_ID)).thenReturn(application);
    when(accountService.get(anyString())).thenReturn(account);
    when(environmentService.get(APP_ID, ENV_ID, false))
        .thenReturn(Environment.Builder.anEnvironment().withUuid(ENV_ID).withName(ENV_NAME).withAppId(APP_ID).build());
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
    wingsPersistence.save(anApplication().withUuid(APP_ID).build());
  }

  @Test
  public void testMigrationOfWorkflowsWithHelmReleaseName() {
    Map<String, Object> properties = new HashMap<>();
    properties.put(HELM_RELEASE_NAME_PREFIX_KEY, "abc");

    Workflow workflow = workflowService.createWorkflow(constructHelmWorkflowWithProperties(properties));
    assertThat(workflow).isNotNull();
    helmReleaseNameSuffixMigration.migrate();

    GraphNode graphNode = fetchGraphNode(workflow);
    assertThat(graphNode.getProperties()).containsKeys(HELM_RELEASE_NAME_PREFIX_KEY);
    assertThat(graphNode.getProperties().get(HELM_RELEASE_NAME_PREFIX_KEY))
        .isEqualTo("abc" + HELM_RELEASE_NAME_SUFFIX_VALUE);
  }

  @Test
  public void testMigrationOfWorkflowsWithExpressionBasedHelmReleaseName() {
    Map<String, Object> properties = new HashMap<>();
    properties.put(HELM_RELEASE_NAME_PREFIX_KEY, HELM_RELEASE_NAME_PREFIX_DEFAULT_VALUE);

    Workflow workflow = workflowService.createWorkflow(constructHelmWorkflowWithProperties(properties));
    assertThat(workflow).isNotNull();
    helmReleaseNameSuffixMigration.migrate();

    GraphNode graphNode = fetchGraphNode(workflow);
    assertThat(graphNode.getProperties()).containsKeys(HELM_RELEASE_NAME_PREFIX_KEY);
    assertThat(graphNode.getProperties().get(HELM_RELEASE_NAME_PREFIX_KEY))
        .isEqualTo(HELM_RELEASE_NAME_PREFIX_DEFAULT_VALUE + HELM_RELEASE_NAME_SUFFIX_VALUE);
  }

  @Test
  public void testMigrationOfWorkflowsWithNonHelmDeployState() {
    Workflow workflow = workflowService.createWorkflow(constructEcsWorkflow());
    assertThat(workflow).isNotNull();
    helmReleaseNameSuffixMigration.migrate();

    GraphNode graphNode = fetchGraphNode(workflow);
    assertThat(graphNode.getProperties()).doesNotContainKeys(HELM_RELEASE_NAME_PREFIX_KEY);
  }

  @Test
  public void testMigrationOfWorkflowsWithExistingSuffixHelmReleaseName() {
    Map<String, Object> properties = new HashMap<>();
    String releaseName = "abc-" + HELM_RELEASE_NAME_SUFFIX_VALUE;
    properties.put(HELM_RELEASE_NAME_PREFIX_KEY, releaseName);

    Workflow workflow = workflowService.createWorkflow(constructHelmWorkflowWithProperties(properties));
    assertThat(workflow).isNotNull();
    helmReleaseNameSuffixMigration.migrate();

    GraphNode graphNode = fetchGraphNode(workflow);
    assertThat(graphNode.getProperties()).containsKeys(HELM_RELEASE_NAME_PREFIX_KEY);
    assertThat(graphNode.getProperties().get(HELM_RELEASE_NAME_PREFIX_KEY)).isEqualTo(releaseName);
  }

  @Test
  public void testMigrationOfWorkflowsWithNoHelmReleaseName() {
    Map<String, Object> properties = new HashMap<>();

    Workflow workflow = workflowService.createWorkflow(constructHelmWorkflowWithProperties(properties));
    assertThat(workflow).isNotNull();
    helmReleaseNameSuffixMigration.migrate();

    GraphNode graphNode = fetchGraphNode(workflow);
    assertThat(graphNode.getProperties()).doesNotContainKeys(HELM_RELEASE_NAME_PREFIX_KEY);
  }

  private GraphNode fetchGraphNode(Workflow workflow) {
    CanaryOrchestrationWorkflow canaryOrchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
    String nodeId =
        canaryOrchestrationWorkflow.getWorkflowPhases().get(0).getPhaseSteps().get(0).getSteps().get(0).getId();
    return workflowService.readGraphNode(workflow.getAppId(), workflow.getUuid(), nodeId);
  }
}
