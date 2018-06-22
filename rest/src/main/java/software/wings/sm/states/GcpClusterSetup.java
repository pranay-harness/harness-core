package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.api.ClusterElement.ClusterElementBuilder.aClusterElement;
import static software.wings.api.GcpClusterExecutionData.GcpClusterExecutionDataBuilder.aGcpClusterExecutionData;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;
import static software.wings.sm.StateType.GCP_CLUSTER_SETUP;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.annotation.Encryptable;
import software.wings.api.ClusterElement;
import software.wings.api.DeploymentType;
import software.wings.api.PhaseElement;
import software.wings.beans.Application;
import software.wings.beans.FeatureName;
import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.beans.container.ContainerTask;
import software.wings.beans.container.KubernetesContainerTask;
import software.wings.cloudprovider.gke.GkeClusterService;
import software.wings.common.Constants;
import software.wings.exception.InvalidRequestException;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.sm.WorkflowStandardParams;
import software.wings.utils.KubernetesConvention;

import java.util.List;
/**
 * Created by brett on 4/14/17
 */
public class GcpClusterSetup extends State {
  private static final Logger logger = LoggerFactory.getLogger(GcpClusterSetup.class);
  @Attributes(title = "Zone") private String zone;

  @Attributes(title = "Node Count") private int nodeCount;

  @Attributes(title = "Machine Type") private String machineType;

  @Inject @Transient private transient GkeClusterService gkeClusterService;
  @Inject @Transient private transient SettingsService settingsService;
  @Inject @Transient private transient ServiceResourceService serviceResourceService;
  @Inject @Transient private transient InfrastructureMappingService infrastructureMappingService;
  @Inject @Transient private transient FeatureFlagService featureFlagService;

  @Inject @Transient private transient SecretManager secretManager;

  /**
   * Instantiates a new state.
   */
  public GcpClusterSetup(String name) {
    super(name, GCP_CLUSTER_SETUP.name());
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    String serviceId = phaseElement.getServiceElement().getUuid();

    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    Application app = workflowStandardParams.getApp();
    String env = workflowStandardParams.getEnv().getName();

    InfrastructureMapping infrastructureMapping =
        infrastructureMappingService.get(app.getUuid(), phaseElement.getInfraMappingId());
    if (!(infrastructureMapping instanceof GcpKubernetesInfrastructureMapping)) {
      throw new InvalidRequestException("Invalid infrastructure type");
    }
    GcpKubernetesInfrastructureMapping gcpInfraMapping = (GcpKubernetesInfrastructureMapping) infrastructureMapping;
    SettingAttribute computeProviderSetting = settingsService.get(gcpInfraMapping.getComputeProviderSettingId());
    List<EncryptedDataDetail> encryptionDetails = secretManager.getEncryptionDetails(
        (Encryptable) computeProviderSetting.getValue(), context.getAppId(), context.getWorkflowExecutionId());
    String serviceName = serviceResourceService.get(app.getUuid(), serviceId).getName();
    boolean isStatefulSet = false;
    ContainerTask containerTask = serviceResourceService.getContainerTaskByDeploymentType(
        app.getUuid(), serviceId, infrastructureMapping.getDeploymentType());
    if (containerTask instanceof KubernetesContainerTask) {
      KubernetesContainerTask kubernetesContainerTask = (KubernetesContainerTask) containerTask;
      isStatefulSet = kubernetesContainerTask.checkStatefulSet();
    }
    if (isEmpty(zone)) {
      zone = "us-west1-a";
    }
    if (nodeCount <= 0) {
      nodeCount = 2;
    }
    if (isEmpty(machineType)) {
      machineType = "n1-standard-2";
    }

    boolean useDashInHostName = featureFlagService.isEnabled(FeatureName.USE_DASH_IN_HOSTNAME, app.getAccountId());
    String clusterName = "harness-"
        + KubernetesConvention.getKubernetesServiceName(KubernetesConvention.getControllerNamePrefix(
              app.getName(), serviceName, env, isStatefulSet, useDashInHostName));
    String zoneCluster = zone + "/" + clusterName;

    gkeClusterService.createCluster(computeProviderSetting, encryptionDetails, zoneCluster,
        gcpInfraMapping.getNamespace(),
        ImmutableMap.<String, String>builder()
            .put("nodeCount", Integer.toString(nodeCount))
            .put("machineType", machineType)
            .put("masterUser", "admin")
            .put("masterPwd", "admin")
            .build());

    ClusterElement clusterElement = aClusterElement()
                                        .withUuid(serviceId)
                                        .withName(zoneCluster)
                                        .withDeploymentType(DeploymentType.KUBERNETES)
                                        .withInfraMappingId(phaseElement.getInfraMappingId())
                                        .build();

    return anExecutionResponse()
        .withExecutionStatus(ExecutionStatus.SUCCESS)
        .addContextElement(clusterElement)
        .addNotifyElement(clusterElement)
        .withStateExecutionData(aGcpClusterExecutionData()
                                    .withClusterName(clusterName)
                                    .withZone(zone)
                                    .withNodeCount(nodeCount)
                                    .withMachineType(machineType)
                                    .build())
        .build();
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  public String getZone() {
    return zone;
  }

  public void setZone(String zone) {
    this.zone = zone;
  }

  public int getNodeCount() {
    return nodeCount;
  }

  public void setNodeCount(int nodeCount) {
    this.nodeCount = nodeCount;
  }

  public String getMachineType() {
    return machineType;
  }

  public void setMachineType(String machineType) {
    this.machineType = machineType;
  }
}
