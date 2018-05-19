package software.wings.service.impl.instance;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.DeploymentInfo;
import software.wings.api.PhaseExecutionData;
import software.wings.api.PhaseStepExecutionData;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.Service;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.Instance.InstanceBuilder;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.exception.HarnessException;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.TriggerService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.instance.InstanceService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.utils.Validator;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * @author rktummala on 01/30/18
 */
public abstract class InstanceHandler {
  protected static final Logger logger = LoggerFactory.getLogger(InstanceHandler.class);

  @Inject protected InstanceHelper instanceHelper;
  @Inject protected InstanceService instanceService;
  @Inject protected InfrastructureMappingService infraMappingService;
  @Inject protected SettingsService settingsService;
  @Inject protected SecretManager secretManager;
  @Inject protected TriggerService triggerService;
  @Inject protected AppService appService;
  @Inject protected EnvironmentService environmentService;
  @Inject protected WorkflowExecutionService workflowExecutionService;
  @Inject protected ServiceResourceService serviceResourceService;
  @Inject protected InstanceUtil instanceUtil;

  public static final String AUTO_SCALE = "AUTO_SCALE";

  public abstract void syncInstances(String appId, String infraMappingId) throws HarnessException;
  public abstract void handleNewDeployment(DeploymentInfo deploymentInfo) throws HarnessException;
  public abstract Optional<DeploymentInfo> getDeploymentInfo(PhaseExecutionData phaseExecutionData,
      PhaseStepExecutionData phaseStepExecutionData, WorkflowExecution workflowExecution,
      InfrastructureMapping infrastructureMapping, String stateExecutionInstanceId, Artifact artifact)
      throws HarnessException;

  protected List<Instance> getInstances(String appId, String infraMappingId) {
    PageRequest<Instance> pageRequest = new PageRequest<>();
    pageRequest.addFilter("infraMappingId", Operator.EQ, infraMappingId);
    pageRequest.addFilter("appId", Operator.EQ, appId);
    PageResponse<Instance> pageResponse = instanceService.list(pageRequest);
    return pageResponse.getResponse();
  }

  /**
   * This generates the deployment info from an instance deployed earlier.
   * This info is used while creating new instance when periodic sync identifies a new instance.
   * @param instance instance from a previous deployment
   * @param deploymentInfo deployment info to be constructed.
   * @return
   */
  protected DeploymentInfo generateDeploymentInfoFromInstance(Instance instance, DeploymentInfo deploymentInfo) {
    deploymentInfo.setAppId(instance.getAppId());
    deploymentInfo.setAccountId(instance.getAccountId());
    deploymentInfo.setInfraMappingId(instance.getInfraMappingId());
    deploymentInfo.setInfraMappingId(instance.getInfraMappingId());
    deploymentInfo.setWorkflowExecutionId(instance.getLastWorkflowExecutionId());
    deploymentInfo.setWorkflowExecutionName(instance.getLastWorkflowExecutionName());
    deploymentInfo.setWorkflowId(instance.getLastWorkflowExecutionId());

    deploymentInfo.setArtifactId(instance.getLastArtifactId());
    deploymentInfo.setArtifactName(instance.getLastArtifactName());
    deploymentInfo.setArtifactStreamId(instance.getLastArtifactStreamId());
    deploymentInfo.setArtifactSourceName(instance.getLastArtifactSourceName());
    deploymentInfo.setArtifactBuildNum(instance.getLastArtifactBuildNum());

    deploymentInfo.setPipelineExecutionId(instance.getLastPipelineExecutionId());
    deploymentInfo.setPipelineExecutionName(instance.getLastPipelineExecutionName());

    // Commented this out, so we can distinguish between autoscales instances and instances we deployed
    deploymentInfo.setDeployedById(AUTO_SCALE);
    deploymentInfo.setDeployedByName(AUTO_SCALE);
    deploymentInfo.setDeployedAt(System.currentTimeMillis());
    deploymentInfo.setArtifactBuildNum(instance.getLastArtifactBuildNum());

    return deploymentInfo;
  }

  protected InstanceBuilder buildInstanceBase(
      String instanceId, InfrastructureMapping infraMapping, DeploymentInfo deploymentInfo) {
    InstanceBuilder builder = this.buildInstanceBase(instanceId, infraMapping);
    if (deploymentInfo != null) {
      builder.lastDeployedAt(deploymentInfo.getDeployedAt())
          .lastDeployedById(deploymentInfo.getDeployedById())
          .lastDeployedByName(deploymentInfo.getDeployedByName())
          .lastWorkflowExecutionId(deploymentInfo.getWorkflowExecutionId())
          .lastWorkflowExecutionName(deploymentInfo.getWorkflowExecutionName())
          .lastArtifactId(deploymentInfo.getArtifactId())
          .lastArtifactName(deploymentInfo.getArtifactName())
          .lastArtifactStreamId(deploymentInfo.getArtifactStreamId())
          .lastArtifactSourceName(deploymentInfo.getArtifactSourceName())
          .lastArtifactBuildNum(deploymentInfo.getArtifactBuildNum())
          .lastPipelineExecutionId(deploymentInfo.getPipelineExecutionId())
          .lastPipelineExecutionName(deploymentInfo.getPipelineExecutionName());
    }

    return builder;
  }

  protected InstanceBuilder buildInstanceBase(String instanceUuid, InfrastructureMapping infraMapping) {
    String appId = infraMapping.getAppId();
    Application application = appService.get(appId);
    Validator.notNullCheck("Application is null for the given appId: " + appId, application);
    Environment environment = environmentService.get(appId, infraMapping.getEnvId(), false);
    Validator.notNullCheck("Environment is null for the given id: " + infraMapping.getEnvId(), environment);
    Service service = serviceResourceService.get(appId, infraMapping.getServiceId());
    Validator.notNullCheck("Service is null for the given id: " + infraMapping.getServiceId(), service);
    String infraMappingType = infraMapping.getInfraMappingType();

    if (instanceUuid == null) {
      instanceUuid = generateUuid();
    }

    InstanceBuilder builder = Instance.builder()
                                  .uuid(instanceUuid)
                                  .accountId(application.getAccountId())
                                  .appId(appId)
                                  .appName(application.getName())
                                  .envName(environment.getName())
                                  .envId(infraMapping.getEnvId())
                                  .envType(environment.getEnvironmentType())
                                  .computeProviderId(infraMapping.getComputeProviderSettingId())
                                  .computeProviderName(infraMapping.getComputeProviderName())
                                  .infraMappingId(infraMapping.getUuid())
                                  .infraMappingType(infraMappingType)
                                  .serviceId(infraMapping.getServiceId())
                                  .serviceName(service.getName());
    instanceUtil.setInstanceType(builder, infraMappingType);

    return builder;
  }

  protected Optional<Instance> getInstanceWithExecutionInfo(Collection<Instance> instances) {
    return instances.stream().filter(instance -> instance.getLastWorkflowExecutionId() != null).findFirst();
  }
}
