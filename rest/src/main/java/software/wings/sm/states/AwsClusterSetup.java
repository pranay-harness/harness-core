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
import software.wings.beans.AwsInfrastructureMapping.AwsRegionDataProvider;
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
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;
import software.wings.utils.EcsConvention;

import java.util.Arrays;

/**
 * Created by brett on 4/14/17
 */
public class AwsClusterSetup extends State {
  private static final Logger logger = LoggerFactory.getLogger(AwsClusterSetup.class);
  @Attributes(title = "Region")
  @DefaultValue("us-east-1")
  @EnumData(enumDataProvider = AwsRegionDataProvider.class)
  private String region;

  @Attributes(title = "Node Count") private int nodeCount;

  @Attributes(title = "Availability Zones") private String availabilityZones;

  @Attributes(title = "VPC Zone Identifiers") private String vpcZoneIdentifiers;

  @Attributes(title = "Auto Scaling Group Name") private String autoScalingGroupName;

  @Attributes(title = "Launcher Configuration") private String launcherConfig;

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
    AwsClusterConfiguration clusterConfiguration = new AwsClusterConfiguration();

    if (StringUtils.isNotEmpty(availabilityZones)) {
      clusterConfiguration.setAvailabilityZones(Arrays.asList(availabilityZones.split(",")));
    }
    if (StringUtils.isNotEmpty(vpcZoneIdentifiers)) {
      clusterConfiguration.setVpcZoneIdentifiers(vpcZoneIdentifiers);
    }
    if (StringUtils.isNotEmpty(autoScalingGroupName)) {
      clusterConfiguration.setAutoScalingGroupName(autoScalingGroupName);
    }
    if (StringUtils.isNotEmpty(launcherConfig)) {
      clusterConfiguration.setLauncherConfiguration(launcherConfig);
    }
    if (StringUtils.isNotEmpty(machineType)) {
      // TODO:: specify machine type for the cluster
    }
    if (StringUtils.isEmpty(region)) {
      region = "us-west-1";
    }
    if (nodeCount <= 0) {
      nodeCount = 2;
    }
    String clusterName = "harness-" + EcsConvention.getTaskFamily(app.getName(), serviceName, env);
    String regionCluster = region + "/" + clusterName;
    clusterConfiguration.setName(clusterName);
    clusterConfiguration.setSize(nodeCount);

    awsClusterService.createCluster(region, computeProviderSetting, clusterConfiguration);

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

  public String getAvailabilityZones() {
    return availabilityZones;
  }

  public void setAvailabilityZones(String availabilityZones) {
    this.availabilityZones = availabilityZones;
  }

  public String getVpcZoneIdentifiers() {
    return vpcZoneIdentifiers;
  }

  public void setVpcZoneIdentifiers(String vpcZoneIdentifiers) {
    this.vpcZoneIdentifiers = vpcZoneIdentifiers;
  }

  public String getAutoScalingGroupName() {
    return autoScalingGroupName;
  }

  public void setAutoScalingGroupName(String autoScalingGroupName) {
    this.autoScalingGroupName = autoScalingGroupName;
  }

  public String getLauncherConfig() {
    return launcherConfig;
  }

  public void setLauncherConfig(String launcherConfig) {
    this.launcherConfig = launcherConfig;
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
    private String availabilityZones;
    private String vpcZoneIdentifiers;
    private String autoScalingGroupName;
    private String launcherConfig;
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

    public AwsClusterSetupBuilder withAvailabilityZones(String availabilityZones) {
      this.availabilityZones = availabilityZones;
      return this;
    }

    public AwsClusterSetupBuilder withVpcZoneIdentifiers(String vpcZoneIdentifiers) {
      this.vpcZoneIdentifiers = vpcZoneIdentifiers;
      return this;
    }

    public AwsClusterSetupBuilder withAutoScalingGroupName(String autoScalingGroupName) {
      this.autoScalingGroupName = autoScalingGroupName;
      return this;
    }

    public AwsClusterSetupBuilder withLauncherConfig(String launcherConfig) {
      this.launcherConfig = launcherConfig;
      return this;
    }

    public AwsClusterSetupBuilder withMachineType(String machineType) {
      this.machineType = machineType;
      return this;
    }

    public AwsClusterSetup build() {
      AwsClusterSetup awsClusterSetup = new AwsClusterSetup(name);
      awsClusterSetup.setRegion(region);
      awsClusterSetup.setNodeCount(nodeCount);
      awsClusterSetup.setAvailabilityZones(availabilityZones);
      awsClusterSetup.setVpcZoneIdentifiers(vpcZoneIdentifiers);
      awsClusterSetup.setAutoScalingGroupName(autoScalingGroupName);
      awsClusterSetup.setLauncherConfig(launcherConfig);
      awsClusterSetup.setMachineType(machineType);
      return awsClusterSetup;
    }
  }
}
