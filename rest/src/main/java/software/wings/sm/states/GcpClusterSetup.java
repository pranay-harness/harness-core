package software.wings.sm.states;

import static software.wings.api.ClusterElement.ClusterElementBuilder.aClusterElement;
import static software.wings.api.GcpClusterExecutionData.GcpClusterExecutionDataBuilder.aGcpClusterExecutionData;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;
import static software.wings.sm.StateType.GCP_CLUSTER_SETUP;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.ClusterElement;
import software.wings.api.DeploymentType;
import software.wings.api.PhaseElement;
import software.wings.beans.Application;
import software.wings.beans.ErrorCode;
import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.cloudprovider.gke.GkeClusterService;
import software.wings.common.Constants;
import software.wings.exception.WingsException;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.sm.WorkflowStandardParams;
import software.wings.utils.KubernetesConvention;

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
    if (infrastructureMapping == null || !(infrastructureMapping instanceof GcpKubernetesInfrastructureMapping)) {
      throw new WingsException(ErrorCode.INVALID_REQUEST, "message", "Invalid infrastructure type");
    }

    SettingAttribute computeProviderSetting = settingsService.get(infrastructureMapping.getComputeProviderSettingId());
    String serviceName = serviceResourceService.get(app.getUuid(), serviceId).getName();

    if (StringUtils.isEmpty(zone)) {
      zone = "us-west1-a";
    }
    if (nodeCount <= 0) {
      nodeCount = 2;
    }
    if (StringUtils.isEmpty(machineType)) {
      machineType = "n1-standard-2";
    }
    String clusterName = "harness-"
        + KubernetesConvention.getKubernetesServiceName(
              KubernetesConvention.getReplicationControllerNamePrefix(app.getName(), serviceName, env));
    String zoneCluster = zone + "/" + clusterName;
    gkeClusterService.createCluster(computeProviderSetting, zoneCluster,
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

  public static final class GcpClusterSetupBuilder {
    private String name;
    private String zone;
    private int nodeCount;
    private String machineType;

    private GcpClusterSetupBuilder() {}

    public static GcpClusterSetupBuilder aGcpClusterSetup() {
      return new GcpClusterSetupBuilder();
    }

    public GcpClusterSetupBuilder withName(String name) {
      this.name = name;
      return this;
    }

    public GcpClusterSetupBuilder withZone(String zone) {
      this.zone = zone;
      return this;
    }

    public GcpClusterSetupBuilder withNodeCount(int nodeCount) {
      this.nodeCount = nodeCount;
      return this;
    }

    public GcpClusterSetupBuilder withMachineType(String machineType) {
      this.machineType = machineType;
      return this;
    }

    public GcpClusterSetupBuilder but() {
      return aGcpClusterSetup().withName(name).withZone(zone).withNodeCount(nodeCount).withMachineType(machineType);
    }

    public GcpClusterSetup build() {
      GcpClusterSetup gcpClusterSetup = new GcpClusterSetup(name);
      gcpClusterSetup.setZone(zone);
      gcpClusterSetup.setNodeCount(nodeCount);
      gcpClusterSetup.setMachineType(machineType);
      return gcpClusterSetup;
    }
  }
}
