package software.wings.sm.states;

import static software.wings.api.AwsClusterExecutionData.AwsClusterExecutionDataBuilder.anAwsClusterExecutionData;
import static software.wings.api.ClusterElement.ClusterElementBuilder.aClusterElement;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;
import static software.wings.sm.StateType.AWS_CLUSTER_SETUP;

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
import software.wings.beans.EcsInfrastructureMapping;
import software.wings.beans.ErrorCode;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.cloudprovider.aws.AwsClusterConfiguration;
import software.wings.cloudprovider.aws.AwsClusterService;
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
import software.wings.utils.EcsConvention;

import java.util.Arrays;

/**
 * Created by brett on 4/14/17
 */
public class AwsClusterSetup extends State {
  private static final Logger logger = LoggerFactory.getLogger(AwsClusterSetup.class);
  @Attributes(title = "Region") private String region;

  @Attributes(title = "Node Count") private int nodeCount;

  @Attributes(title = "Machine Type") private String machineType;

  @Inject @Transient private transient AwsClusterService awsClusterService;
  @Inject @Transient private transient SettingsService settingsService;
  @Inject @Transient private transient ServiceResourceService serviceResourceService;
  @Inject @Transient private transient InfrastructureMappingService infrastructureMappingService;

  /**
   * Instantiates a new state.
   */
  public AwsClusterSetup(String name) {
    super(name, AWS_CLUSTER_SETUP.name());
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
    if (infrastructureMapping == null || !(infrastructureMapping instanceof EcsInfrastructureMapping)) {
      throw new WingsException(ErrorCode.INVALID_REQUEST, "message", "Invalid infrastructure type");
    }

    SettingAttribute computeProviderSetting = settingsService.get(infrastructureMapping.getComputeProviderSettingId());
    String serviceName = serviceResourceService.get(app.getUuid(), serviceId).getName();

    if (StringUtils.isEmpty(region)) {
      region = "us-west1-a";
    }
    if (nodeCount <= 0) {
      nodeCount = 2;
    }
    if (StringUtils.isEmpty(machineType)) {
      machineType = "n1-standard-2";
    }
    String clusterName = "wings-" + EcsConvention.getTaskFamily(app.getName(), serviceName, env);
    String regionCluster = region + "/" + clusterName;
    AwsClusterConfiguration clusterConfiguration = new AwsClusterConfiguration();
    clusterConfiguration.setName(clusterName);
    clusterConfiguration.setSize(nodeCount);
    clusterConfiguration.setAvailabilityZones(Arrays.asList("us-east-1a", "us-east-1c", "us-east-1d", "us-east-1e"));
    clusterConfiguration.setVpcZoneIdentifiers("subnet-9725a6bd,subnet-42ddaf34,subnet-64d99b59,subnet-fbe268a3");
    clusterConfiguration.setAutoScalingGroupName("wins_demo_launchconfigAsg_v1");
    clusterConfiguration.setLauncherConfiguration("wins_demo_launchconfig_v1");

    awsClusterService.createCluster(computeProviderSetting, clusterConfiguration);

    ClusterElement clusterElement = aClusterElement()
                                        .withUuid(serviceId)
                                        .withName(regionCluster)
                                        .withDeploymentType(DeploymentType.ECS)
                                        .withInfraMappingId(phaseElement.getInfraMappingId())
                                        .build();

    return anExecutionResponse()
        .withExecutionStatus(ExecutionStatus.SUCCESS)
        .addContextElement(clusterElement)
        .addNotifyElement(clusterElement)
        .withStateExecutionData(anAwsClusterExecutionData()
                                    .withClusterName(clusterName)
                                    .withRegion(region)
                                    .withNodeCount(nodeCount)
                                    .withMachineType(machineType)
                                    .build())
        .build();
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  public String getRegion() {
    return region;
  }

  public void setRegion(String region) {
    this.region = region;
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

  public static final class AwsClusterSetupBuilder {
    private String name;
    private String region;
    private int nodeCount;
    private String machineType;

    private AwsClusterSetupBuilder() {}

    public static AwsClusterSetupBuilder anAwsClusterSetup() {
      return new AwsClusterSetupBuilder();
    }

    public AwsClusterSetupBuilder withName(String name) {
      this.name = name;
      return this;
    }

    public AwsClusterSetupBuilder withRegion(String region) {
      this.region = region;
      return this;
    }

    public AwsClusterSetupBuilder withNodeCount(int nodeCount) {
      this.nodeCount = nodeCount;
      return this;
    }

    public AwsClusterSetupBuilder withMachineType(String machineType) {
      this.machineType = machineType;
      return this;
    }

    public AwsClusterSetupBuilder but() {
      return anAwsClusterSetup().withName(name).withRegion(region).withNodeCount(nodeCount).withMachineType(
          machineType);
    }

    public AwsClusterSetup build() {
      AwsClusterSetup awsClusterSetup = new AwsClusterSetup(name);
      awsClusterSetup.setRegion(region);
      awsClusterSetup.setNodeCount(nodeCount);
      awsClusterSetup.setMachineType(machineType);
      return awsClusterSetup;
    }
  }
}
