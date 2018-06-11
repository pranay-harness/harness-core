package software.wings.service.impl.instance;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.DeploymentInfo;
import software.wings.api.DeploymentSummary;
import software.wings.api.PhaseExecutionData;
import software.wings.api.PhaseStepExecutionData;
import software.wings.beans.Application;
import software.wings.beans.EmbeddedUser;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.Service;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.Instance.InstanceBuilder;
import software.wings.beans.infrastructure.instance.InstanceType;
import software.wings.beans.infrastructure.instance.key.deployment.DeploymentKey;
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
import software.wings.service.intfc.instance.DeploymentService;
import software.wings.service.intfc.instance.InstanceService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.PipelineSummary;
import software.wings.utils.Validator;

import java.util.ArrayList;
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
  @Inject protected DeploymentService deploymentService;

  public static final String AUTO_SCALE = "AUTO_SCALE";

  public abstract void syncInstances(String appId, String infraMappingId) throws HarnessException;
  public abstract void handleNewDeployment(List<DeploymentSummary> deploymentSummaries, boolean rollback)
      throws HarnessException;

  public abstract Optional<List<DeploymentInfo>> getDeploymentInfo(PhaseExecutionData phaseExecutionData,
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

  public abstract DeploymentKey generateDeploymentKey(DeploymentInfo deploymentInfo);

  public List<DeploymentSummary> getDeploymentSummariesForEvent(PhaseExecutionData phaseExecutionData,
      PhaseStepExecutionData phaseStepExecutionData, WorkflowExecution workflowExecution,
      InfrastructureMapping infrastructureMapping, String stateExecutionInstanceId, Artifact artifact)
      throws HarnessException {
    Optional<List<DeploymentInfo>> deploymentInfoOptional = getDeploymentInfo(phaseExecutionData,
        phaseStepExecutionData, workflowExecution, infrastructureMapping, stateExecutionInstanceId, artifact);

    List<DeploymentSummary> deploymentSummaries = new ArrayList<>();

    if (deploymentInfoOptional.isPresent()) {
      for (DeploymentInfo deploymentInfo : deploymentInfoOptional.get()) {
        DeploymentSummary deploymentSummary = setValuesToDeploymentSummary(stateExecutionInstanceId, workflowExecution,
            phaseExecutionData, infrastructureMapping, artifact, deploymentInfo, generateDeploymentKey(deploymentInfo));

        deploymentSummaries.add(deploymentSummary);
      }
    }

    return deploymentSummaries;
  }

  private DeploymentSummary setValuesToDeploymentSummary(String stateExecutionInstanceId,
      WorkflowExecution workflowExecution, PhaseExecutionData phaseExecutionData,
      InfrastructureMapping infrastructureMapping, Artifact artifact, DeploymentInfo deploymentInfo,
      DeploymentKey deploymentKey) {
    PipelineSummary pipelineSummary = workflowExecution.getPipelineSummary();
    Application application = appService.get(workflowExecution.getAppId());
    Validator.notNullCheck("Application", application);
    EmbeddedUser triggeredBy = workflowExecution.getTriggeredBy();
    Validator.notNullCheck("triggeredBy", triggeredBy);
    String infraMappingType = infrastructureMapping.getInfraMappingType();

    String workflowName = instanceUtil.getWorkflowName(workflowExecution.getName());
    Validator.notNullCheck("WorkflowName", workflowName);

    InstanceType instanceType = instanceUtil.getInstanceType(infraMappingType);
    Validator.notNullCheck("InstanceType", instanceType);

    DeploymentSummary deploymentSummary = DeploymentSummary.builder().build();
    deploymentSummary.setAppId(workflowExecution.getAppId());
    deploymentSummary.setAccountId(application.getAccountId());
    deploymentSummary.setInfraMappingId(phaseExecutionData.getInfraMappingId());
    deploymentSummary.setStateExecutionInstanceId(stateExecutionInstanceId);
    deploymentSummary.setWorkflowExecutionId(workflowExecution.getUuid());
    deploymentSummary.setWorkflowExecutionName(workflowExecution.getName());
    deploymentSummary.setWorkflowId(workflowExecution.getWorkflowId());

    if (artifact != null) {
      deploymentSummary.setArtifactId(artifact.getUuid());
      deploymentSummary.setArtifactName(artifact.getDisplayName());
      deploymentSummary.setArtifactStreamId(artifact.getArtifactStreamId());
      deploymentSummary.setArtifactSourceName(artifact.getArtifactSourceName());
      deploymentSummary.setArtifactBuildNum(artifact.getBuildNo());
    }

    if (pipelineSummary != null) {
      deploymentSummary.setPipelineExecutionId(pipelineSummary.getPipelineId());
      deploymentSummary.setPipelineExecutionName(pipelineSummary.getPipelineName());
    }

    deploymentSummary.setDeployedById(triggeredBy.getUuid());
    deploymentSummary.setDeployedByName(triggeredBy.getName());
    deploymentSummary.setDeployedAt(
        phaseExecutionData.getEndTs() == null ? System.currentTimeMillis() : phaseExecutionData.getEndTs());

    deploymentSummary.setDeploymentInfo(deploymentInfo);
    setDeploymentKey(deploymentSummary, deploymentKey);
    return deploymentSummary;
  }

  protected abstract void setDeploymentKey(DeploymentSummary deploymentSummary, DeploymentKey deploymentKey);

  /**
   * This generates the deployment info from an instance deployed earlier.
   * This info is used while creating new instance when periodic sync identifies a new instance.
   * @param instance instance from a previous deployment
   * @param deploymentSummary deployment info to be constructed.
   * @return
   */
  protected DeploymentSummary generateDeploymentSummaryFromInstance(
      Instance instance, DeploymentSummary deploymentSummary) {
    deploymentSummary.setAppId(instance.getAppId());
    deploymentSummary.setAccountId(instance.getAccountId());
    deploymentSummary.setInfraMappingId(instance.getInfraMappingId());
    deploymentSummary.setInfraMappingId(instance.getInfraMappingId());
    deploymentSummary.setWorkflowExecutionId(instance.getLastWorkflowExecutionId());
    deploymentSummary.setWorkflowExecutionName(instance.getLastWorkflowExecutionName());
    deploymentSummary.setWorkflowId(instance.getLastWorkflowExecutionId());

    deploymentSummary.setArtifactId(instance.getLastArtifactId());
    deploymentSummary.setArtifactName(instance.getLastArtifactName());
    deploymentSummary.setArtifactStreamId(instance.getLastArtifactStreamId());
    deploymentSummary.setArtifactSourceName(instance.getLastArtifactSourceName());
    deploymentSummary.setArtifactBuildNum(instance.getLastArtifactBuildNum());

    deploymentSummary.setPipelineExecutionId(instance.getLastPipelineExecutionId());
    deploymentSummary.setPipelineExecutionName(instance.getLastPipelineExecutionName());

    // Commented this out, so we can distinguish between autoscales instances and instances we deployed
    deploymentSummary.setDeployedById(AUTO_SCALE);
    deploymentSummary.setDeployedByName(AUTO_SCALE);
    deploymentSummary.setDeployedAt(System.currentTimeMillis());
    deploymentSummary.setArtifactBuildNum(instance.getLastArtifactBuildNum());

    return deploymentSummary;
  }

  protected DeploymentSummary getDeploymentSummaryForInstanceCreation(
      DeploymentSummary newDeploymentSummary, boolean rollback) {
    DeploymentSummary deploymentSummary;
    if (rollback) {
      deploymentSummary = getDeploymentSummaryForRollback(newDeploymentSummary);
    } else {
      deploymentSummary = newDeploymentSummary;
    }

    return deploymentSummary;
  }

  protected DeploymentSummary getDeploymentSummaryForRollback(DeploymentSummary deploymentSummary) {
    Optional<DeploymentSummary> summaryOptional = deploymentService.get(deploymentSummary);
    if (summaryOptional != null && summaryOptional.isPresent()) {
      DeploymentSummary deploymentSummaryFromDB = summaryOptional.get();
      // Copy Artifact Information for rollback version for previous deployment summary
      deploymentSummary.setArtifactBuildNum(deploymentSummaryFromDB.getArtifactBuildNum());
      deploymentSummary.setArtifactName(deploymentSummaryFromDB.getArtifactName());
      deploymentSummary.setArtifactId(deploymentSummaryFromDB.getArtifactId());
      deploymentSummary.setArtifactSourceName(deploymentSummaryFromDB.getArtifactSourceName());
      deploymentSummary.setArtifactStreamId(deploymentSummaryFromDB.getArtifactStreamId());
    } else {
      logger.info("Unable to find DeploymentSummary while rolling back " + deploymentSummary);
    }
    return deploymentSummary;
  }

  protected InstanceBuilder buildInstanceBase(
      String instanceId, InfrastructureMapping infraMapping, DeploymentSummary deploymentSummary) {
    InstanceBuilder builder = this.buildInstanceBase(instanceId, infraMapping);
    if (deploymentSummary != null) {
      builder.lastDeployedAt(deploymentSummary.getDeployedAt())
          .lastDeployedById(deploymentSummary.getDeployedById())
          .lastDeployedByName(deploymentSummary.getDeployedByName())
          .lastWorkflowExecutionId(deploymentSummary.getWorkflowExecutionId())
          .lastWorkflowExecutionName(deploymentSummary.getWorkflowExecutionName())
          .lastArtifactId(deploymentSummary.getArtifactId())
          .lastArtifactName(deploymentSummary.getArtifactName())
          .lastArtifactStreamId(deploymentSummary.getArtifactStreamId())
          .lastArtifactSourceName(deploymentSummary.getArtifactSourceName())
          .lastArtifactBuildNum(deploymentSummary.getArtifactBuildNum())
          .lastPipelineExecutionId(deploymentSummary.getPipelineExecutionId())
          .lastPipelineExecutionName(deploymentSummary.getPipelineExecutionName());
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
