package software.wings.service.impl.instance;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER_SRE;
import static java.lang.String.format;
import static software.wings.beans.InfrastructureMappingType.AWS_AWS_LAMBDA;
import static software.wings.beans.InfrastructureMappingType.AWS_SSH;
import static software.wings.beans.InfrastructureMappingType.PHYSICAL_DATA_CENTER_SSH;
import static software.wings.beans.InfrastructureMappingType.PHYSICAL_DATA_CENTER_WINRM;
import static software.wings.sm.ExecutionStatus.FAILED;
import static software.wings.utils.Validator.notNullCheck;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.Reservation;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.data.structure.UUIDGenerator;
import io.harness.exception.WingsException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.annotation.Encryptable;
import software.wings.api.DeploymentEvent;
import software.wings.api.DeploymentSummary;
import software.wings.api.HostElement;
import software.wings.api.PhaseExecutionData;
import software.wings.api.PhaseStepExecutionData;
import software.wings.beans.Application;
import software.wings.beans.AwsAmiInfrastructureMapping;
import software.wings.beans.AwsConfig;
import software.wings.beans.CodeDeployInfrastructureMapping;
import software.wings.beans.ElementExecutionSummary;
import software.wings.beans.EmbeddedUser;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.PhaseStepType;
import software.wings.beans.SettingAttribute;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.infrastructure.Host;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.Instance.InstanceBuilder;
import software.wings.beans.infrastructure.instance.ManualSyncJob;
import software.wings.beans.infrastructure.instance.info.Ec2InstanceInfo;
import software.wings.beans.infrastructure.instance.info.InstanceInfo;
import software.wings.beans.infrastructure.instance.info.PhysicalHostInstanceInfo;
import software.wings.beans.infrastructure.instance.key.HostInstanceKey;
import software.wings.core.queue.Queue;
import software.wings.lock.AcquiredLock;
import software.wings.lock.PersistentLocker;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.instance.DeploymentService;
import software.wings.service.intfc.instance.InstanceService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionEvent;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.PhaseStepExecutionSummary;
import software.wings.sm.PipelineSummary;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.WorkflowStandardParams;
import software.wings.sm.states.PhaseStepSubWorkflow;
import software.wings.utils.Util;
import software.wings.utils.Validator;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * Both the normal instance and container instance are handled here.
 * Once it finds the deployment is of type container, it hands off the request to ContainerInstanceHelper.
 *
 * @author rktummala on 09/11/17
 */
@Singleton
public class InstanceHelper {
  private static final Logger logger = LoggerFactory.getLogger(InstanceHelper.class);

  // This queue is used to asynchronously process all the instance information that the workflow touched upon.
  @Inject private Queue<DeploymentEvent> deploymentEventQueue;
  @Inject private InfrastructureMappingService infraMappingService;
  @Inject private AppService appService;
  @Inject private InstanceUtil instanceUtil;
  @Inject private HostService hostService;
  @Inject private AwsHelperService awsHelperService;
  @Inject private SettingsService settingsService;
  @Inject private SecretManager secretManager;
  @Inject private InstanceService instanceService;
  @Inject private PersistentLocker persistentLocker;
  @Inject private InstanceHandlerFactory instanceHandlerFactory;
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private DeploymentService deploymentService;
  @Inject private ExecutorService executorService;

  /**
   * The phaseExecutionData is used to process the instance information that is used by the service and infra
   * dashboards. The instance processing happens asynchronously.
   */
  public void extractInstanceOrDeploymentInfoBaseOnType(String stateExecutionInstanceId,
      PhaseExecutionData phaseExecutionData, PhaseStepExecutionData phaseStepExecutionData,
      WorkflowStandardParams workflowStandardParams, String appId, WorkflowExecution workflowExecution,
      PhaseStepSubWorkflow phaseStepSubWorkflow, ExecutionContext context) {
    try {
      if (phaseExecutionData == null) {
        logger.error("phaseExecutionData is null for state execution {}", stateExecutionInstanceId);
        return;
      }

      if (phaseStepExecutionData == null) {
        logger.error("phaseStepExecutionData is null for state execution {}", stateExecutionInstanceId);
        return;
      }

      if (workflowStandardParams == null) {
        logger.warn("workflowStandardParams can't be null, skipping instance processing");
        return;
      }

      Artifact artifact = workflowStandardParams.getArtifactForService(phaseExecutionData.getServiceId());

      if (artifact == null) {
        logger.info("artifact is null for stateExecutionInstance:" + stateExecutionInstanceId);
      }

      if (phaseExecutionData.getInfraMappingId() == null) {
        logger.info("infraMappingId is null for appId:{}, WorkflowExecutionId:{}", appId, workflowExecution.getUuid());
        return;
      }

      InfrastructureMapping infrastructureMapping =
          infraMappingService.get(appId, phaseExecutionData.getInfraMappingId());

      if (PHYSICAL_DATA_CENTER_SSH.getName().equals(infrastructureMapping.getInfraMappingType())
          || PHYSICAL_DATA_CENTER_WINRM.getName().equals(infrastructureMapping.getInfraMappingType())
          || AWS_SSH.getName().equals(infrastructureMapping.getInfraMappingType())) {
        List<Instance> instanceList = Lists.newArrayList();
        PhaseStepExecutionSummary phaseStepExecutionSummary = phaseStepExecutionData.getPhaseStepExecutionSummary();

        if (phaseStepExecutionSummary == null) {
          logger.warn(
              "phaseStepExecutionSummary is null for InfraMappingType {}, appId: {}, WorkflowExecution<Name, Id> :<{},{}>",
              infrastructureMapping.getInfraMappingType(), appId, workflowExecution.getName(),
              workflowExecution.getWorkflowId());
          return;
        }

        if (checkIfAnyStepsFailed(phaseStepExecutionSummary)) {
          logger.info("Deploy Service Phase step failed, not capturing any instances");
          return;
        }

        if (phaseStepExecutionData.getElementStatusSummary() == null) {
          logger.warn(
              "elementStatusSummary is null for InfraMappingType {}, appId: {}, WorkflowExecution<Name, Id> :<{},{}>",
              infrastructureMapping.getInfraMappingType(), appId, workflowExecution.getName(),
              workflowExecution.getWorkflowId());
          return;
        }

        for (ElementExecutionSummary summary : phaseStepExecutionData.getElementStatusSummary()) {
          List<InstanceStatusSummary> instanceStatusSummaries = summary.getInstanceStatusSummaries();
          if (isEmpty(instanceStatusSummaries)) {
            logger.info("No instances to process");
            return;
          }

          for (InstanceStatusSummary instanceStatusSummary : instanceStatusSummaries) {
            if (ExecutionStatus.isPositiveStatus(instanceStatusSummary.getStatus())) {
              if (phaseStepSubWorkflow.isRollback()) {
                // this is same way, workflow get artifact for rollback
                // reference in CommandState.java
                Artifact rollbackArtifact = serviceResourceService.findPreviousArtifact(
                    appId, context.getWorkflowExecutionId(), instanceStatusSummary.getInstanceElement());
                if (rollbackArtifact != null) {
                  artifact = rollbackArtifact;
                }
              }
              Instance instance = buildInstanceUsingHostInfo(workflowExecution, artifact, instanceStatusSummary,
                  phaseExecutionData, phaseStepExecutionData, infrastructureMapping);
              if (instance != null) {
                instanceList.add(instance);
              }
            }
          }
        }
        instanceService.saveOrUpdate(instanceList);
      } else {
        Optional<InstanceHandler> instanceHandlerOptional = getInstanceHandler(infrastructureMapping);
        if (!instanceHandlerOptional.isPresent()) {
          if (!hasNoInstanceHandler(infrastructureMapping.getInfraMappingType())) {
            String msg = "Instance handler not found for infraMappingType: " + infrastructureMapping.getName();
            logger.error(msg);
            throw new WingsException(msg);
          } else {
            logger.info("Instance handler not supported for infra mapping type: {}",
                infrastructureMapping.getInfraMappingType());
            return;
          }
        }

        InstanceHandler instanceHandler = instanceHandlerOptional.get();
        List<DeploymentSummary> deploymentSummaries = instanceHandler.getDeploymentSummariesForEvent(phaseExecutionData,
            phaseStepExecutionData, workflowExecution, infrastructureMapping, stateExecutionInstanceId, artifact);

        if (isNotEmpty(deploymentSummaries)) {
          deploymentEventQueue.send(DeploymentEvent.builder()
                                        .deploymentSummaries(deploymentSummaries)
                                        .isRollback(phaseStepSubWorkflow.isRollback())
                                        .build());
        }
      }
    } catch (Exception ex) {
      // we deliberately don't throw back the exception since we don't want the workflow to be affected
      logger.error(
          "Error while updating instance change information for executionId [{}], ", workflowExecution.getUuid(), ex);
    }
  }

  private boolean hasNoInstanceHandler(String infraMappingType) {
    if (AWS_AWS_LAMBDA.getName().equals(infraMappingType)) {
      return true;
    }
    return false;
  }

  private boolean checkIfAnyStepsFailed(PhaseStepExecutionSummary phaseStepExecutionSummary) {
    return phaseStepExecutionSummary.getStepExecutionSummaryList().stream().anyMatch(
        stepExecutionSummary -> stepExecutionSummary.getStatus() == FAILED);
  }

  public Instance buildInstanceUsingHostInfo(WorkflowExecution workflowExecution, Artifact artifact,
      InstanceStatusSummary instanceStatusSummary, PhaseExecutionData phaseExecutionData,
      PhaseStepExecutionData phaseStepExecutionData, InfrastructureMapping infraMapping) {
    HostElement host = instanceStatusSummary.getInstanceElement().getHost();
    Validator.notNullCheck("Host is null for workflow execution:" + workflowExecution.getWorkflowId(), host);

    InstanceBuilder builder = buildInstanceBase(
        workflowExecution, artifact, phaseExecutionData, phaseStepExecutionData, infraMapping.getInfraMappingType());
    String hostUuid = host.getUuid();

    String region = null;
    if (infraMapping instanceof AwsAmiInfrastructureMapping) {
      region = ((AwsAmiInfrastructureMapping) infraMapping).getRegion();
    } else if (infraMapping instanceof CodeDeployInfrastructureMapping) {
      region = ((CodeDeployInfrastructureMapping) infraMapping).getRegion();
    }

    if (hostUuid == null) {
      if (host.getEc2Instance() != null) {
        setInstanceInfoAndKey(builder, host.getEc2Instance(), phaseExecutionData.getInfraMappingId());
      } else if (host.getInstanceId() != null && region != null) {
        // TODO:: Avoid sequential fetch for Instance
        SettingAttribute cloudProviderSetting = settingsService.get(infraMapping.getComputeProviderSettingId());
        List<EncryptedDataDetail> encryptionDetails = secretManager.getEncryptionDetails(
            (Encryptable) cloudProviderSetting.getValue(), workflowExecution.getAppId(), workflowExecution.getUuid());
        AwsConfig awsConfig = (AwsConfig) cloudProviderSetting.getValue();
        com.amazonaws.services.ec2.model.Instance instance =
            awsHelperService
                .describeEc2Instances(awsConfig, encryptionDetails, region,
                    new DescribeInstancesRequest().withInstanceIds(host.getInstanceId()))
                .getReservations()
                .stream()
                .findFirst()
                .orElse(new Reservation().withInstances(new ArrayList<>()))
                .getInstances()
                .stream()
                .findFirst()
                .orElse(null);
        if (instance != null) {
          setInstanceInfoAndKey(builder, instance, phaseExecutionData.getInfraMappingId());
        } else {
          logger.warn(
              "Cannot build host based instance info since instanceId is not found in AWS workflowId:{}, instanceId:{}",
              workflowExecution.getUuid(), host.getInstanceId());
          return null;
        }
      } else {
        logger.warn(
            "Cannot build host based instance info since both hostId and ec2Instance are null for workflow execution {}",
            workflowExecution.getUuid());
        return null;
      }
    } else {
      Host hostInfo = hostService.get(workflowExecution.getAppId(), workflowExecution.getEnvId(), hostUuid);
      Validator.notNullCheck("Host is null for workflow execution:" + workflowExecution.getWorkflowId(), hostInfo);
      setInstanceInfoAndKey(
          builder, hostInfo, infraMapping.getInfraMappingType(), phaseExecutionData.getInfraMappingId());
    }
    return builder.build();
  }

  public void setInstanceInfoAndKey(
      InstanceBuilder builder, com.amazonaws.services.ec2.model.Instance ec2Instance, String infraMappingId) {
    String privateDnsNameWithSuffix = ec2Instance.getPrivateDnsName();
    String privateDnsName = getPrivateDnsName(privateDnsNameWithSuffix);
    HostInstanceKey hostInstanceKey =
        HostInstanceKey.builder().hostName(privateDnsName).infraMappingId(infraMappingId).build();
    builder.hostInstanceKey(hostInstanceKey);

    InstanceInfo instanceInfo = Ec2InstanceInfo.builder()
                                    .ec2Instance(ec2Instance)
                                    .hostName(privateDnsName)
                                    .hostPublicDns(ec2Instance.getPublicDnsName())
                                    .build();

    builder.instanceInfo(instanceInfo);
  }

  private String getPrivateDnsName(String privateDnsNameWithSuffix) {
    // e.g. null, "", "   "
    if (StringUtils.isEmpty(privateDnsNameWithSuffix) || StringUtils.isBlank(privateDnsNameWithSuffix)) {
      return StringUtils.EMPTY;
    }

    // "ip-172-31-11-6.ec2.internal", we return ip-172-31-11-6
    if (privateDnsNameWithSuffix.indexOf('.') != -1) {
      return privateDnsNameWithSuffix.substring(0, privateDnsNameWithSuffix.indexOf('.'));
    }

    return privateDnsNameWithSuffix;
  }

  public InstanceBuilder buildInstanceBase(WorkflowExecution workflowExecution, Artifact artifact,
      PhaseExecutionData phaseExecutionData, PhaseStepExecutionData phaseStepExecutionData, String infraMappingType) {
    PipelineSummary pipelineSummary = workflowExecution.getPipelineSummary();
    Application application = appService.get(workflowExecution.getAppId());
    Validator.notNullCheck("Application", application);
    EmbeddedUser triggeredBy = workflowExecution.getTriggeredBy();
    Validator.notNullCheck("triggeredBy", triggeredBy);

    InstanceBuilder builder =
        Instance.builder()
            .accountId(application.getAccountId())
            .appId(workflowExecution.getAppId())
            .appName(workflowExecution.getAppName())
            .envName(workflowExecution.getEnvName())
            .envId(workflowExecution.getEnvId())
            .envType(workflowExecution.getEnvType())
            .computeProviderId(phaseExecutionData.getComputeProviderId())
            .computeProviderName(phaseExecutionData.getComputeProviderName())
            .infraMappingId(phaseExecutionData.getInfraMappingId())
            .infraMappingType(infraMappingType)
            .lastDeployedAt(phaseStepExecutionData.getEndTs() != null ? phaseStepExecutionData.getEndTs()
                                                                      : System.currentTimeMillis())
            .lastDeployedById(triggeredBy.getUuid())
            .lastDeployedByName(triggeredBy.getName())
            .serviceId(phaseExecutionData.getServiceId())
            .serviceName(phaseExecutionData.getServiceName())
            .lastWorkflowExecutionId(workflowExecution.getUuid());
    if (artifact != null) {
      builder.lastArtifactId(artifact.getUuid())
          .lastArtifactName(artifact.getDisplayName())
          .lastArtifactStreamId(artifact.getArtifactStreamId())
          .lastArtifactSourceName(artifact.getArtifactSourceName())
          .lastArtifactBuildNum(artifact.getBuildNo());
    }

    if (pipelineSummary != null) {
      builder.lastPipelineExecutionId(pipelineSummary.getPipelineId())
          .lastPipelineExecutionName(pipelineSummary.getPipelineName());
    }

    String workflowName = instanceUtil.getWorkflowName(workflowExecution.getName());
    Validator.notNullCheck("WorkflowName", workflowName);
    builder.lastWorkflowExecutionName(workflowName);

    instanceUtil.setInstanceType(builder, infraMappingType);

    return builder;
  }

  private void setInstanceInfoAndKey(
      InstanceBuilder builder, Host host, String infraMappingType, String infraMappingId) {
    InstanceInfo instanceInfo;
    HostInstanceKey hostInstanceKey =
        HostInstanceKey.builder().hostName(host.getHostName()).infraMappingId(infraMappingId).build();
    builder.hostInstanceKey(hostInstanceKey);

    if (PHYSICAL_DATA_CENTER_SSH.getName().equals(infraMappingType)
        || PHYSICAL_DATA_CENTER_WINRM.getName().equals(infraMappingType)) {
      instanceInfo = PhysicalHostInstanceInfo.builder()
                         .hostPublicDns(host.getPublicDns())
                         .hostId(host.getUuid())
                         .hostName(host.getHostName())
                         .build();
    } else {
      instanceInfo = Ec2InstanceInfo.builder()
                         .ec2Instance(host.getEc2Instance())
                         .hostId(host.getUuid())
                         .hostName(host.getHostName())
                         .hostPublicDns(host.getPublicDns())
                         .build();
    }

    builder.instanceInfo(instanceInfo);
  }

  @SuppressFBWarnings("NP_LOAD_OF_KNOWN_NULL_VALUE")
  public void processDeploymentEvent(DeploymentEvent deploymentEvent) {
    List<DeploymentSummary> deploymentSummaries = deploymentEvent.getDeploymentSummaries();

    if (isEmpty(deploymentSummaries)) {
      logger.error("Deployment Summaries can not be empty or null");
      return;
    }

    if (!deploymentEvent.isRollback()) {
      deploymentSummaries.forEach(deploymentSummary -> saveDeploymentSummary(deploymentSummary, false));
    }

    processDeploymentSummaries(deploymentSummaries, deploymentEvent.isRollback());
  }

  private DeploymentSummary saveDeploymentSummary(DeploymentSummary deploymentSummary, boolean rollback) {
    if (rollback) {
      return deploymentSummary;
    }

    return deploymentService.save(deploymentSummary);
  }

  private void processDeploymentSummaries(List<DeploymentSummary> deploymentSummaries, boolean isRollback) {
    if (isEmpty(deploymentSummaries)) {
      return;
    }

    String infraMappingId = deploymentSummaries.iterator().next().getInfraMappingId();
    String appId = deploymentSummaries.iterator().next().getAppId();
    String workflowExecutionId = deploymentSummaries.iterator().next().getWorkflowExecutionId();
    try (AcquiredLock lock = persistentLocker.waitToAcquireLock(
             InfrastructureMapping.class, infraMappingId, Duration.ofSeconds(200), Duration.ofSeconds(220))) {
      logger.info("Handling deployment event for executionId [{}], infraMappingId [{}] of appId [{}]",
          workflowExecutionId, infraMappingId, appId);

      InfrastructureMapping infraMapping = infraMappingService.get(appId, infraMappingId);
      Validator.notNullCheck("Infra mapping is null for the given id: " + infraMappingId, infraMapping);

      InfrastructureMappingType infrastructureMappingType =
          Util.getEnumFromString(InfrastructureMappingType.class, infraMapping.getInfraMappingType());
      if (isSupported(infrastructureMappingType)) {
        InstanceHandler instanceHandler = instanceHandlerFactory.getInstanceHandler(infrastructureMappingType);
        instanceHandler.handleNewDeployment(deploymentSummaries, isRollback);
        logger.info("Handled deployment event for executionId [{}], infraMappingId [{}] of appId [{}] successfully",
            workflowExecutionId, infraMappingId, appId);
      } else {
        logger.info("Skipping deployment event for executionId [{}], infraMappingId [{}] of appId [{}]",
            workflowExecutionId, infraMappingId, appId);
      }
    } catch (Exception ex) {
      // We have to catch all kinds of runtime exceptions, log it and move on, otherwise the queue impl keeps retrying
      // forever in case of exception
      logger.error("Exception while handling deployment event for executionId [{}], infraMappingId [{}] of appId [{}]",
          workflowExecutionId, infraMappingId, appId, ex);
    }
  }

  private Optional<InstanceHandler> getInstanceHandler(InfrastructureMapping infraMapping) {
    InfrastructureMappingType infrastructureMappingType =
        Util.getEnumFromString(InfrastructureMappingType.class, infraMapping.getInfraMappingType());
    if (isSupported(infrastructureMappingType)) {
      return Optional.of(instanceHandlerFactory.getInstanceHandler(infrastructureMappingType));
    }
    return Optional.empty();
  }

  private boolean isSupported(InfrastructureMappingType infrastructureMappingType) {
    if (AWS_AWS_LAMBDA.equals(infrastructureMappingType) || PHYSICAL_DATA_CENTER_SSH.equals(infrastructureMappingType)
        || PHYSICAL_DATA_CENTER_WINRM.equals(infrastructureMappingType)) {
      return false;
    }
    return true;
  }

  public boolean isDeployPhaseStep(PhaseStepType phaseStepType) {
    switch (phaseStepType) {
        //      case PROVISION_NODE:
        //      case DE_PROVISION_NODE:
      case DEPLOY_SERVICE:
      case CONTAINER_DEPLOY:
      case PCF_RESIZE:
      case DEPLOY_AWSCODEDEPLOY:
      case DEPLOY_AWS_LAMBDA:
      case AMI_DEPLOY_AUTOSCALING_GROUP:
      case HELM_DEPLOY:
        return true;
      default:
        return false;
    }
  }

  public void extractInstance(PhaseStepSubWorkflow phaseStepSubWorkflow, ExecutionEvent executionEvent,
      WorkflowExecution workflowExecution, ExecutionContext context, StateExecutionInstance stateExecutionInstance) {
    if (isDeployPhaseStep(phaseStepSubWorkflow.getPhaseStepType())
        && ExecutionStatus.isFinalStatus(executionEvent.getExecutionStatus())) {
      WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
      notNullCheck("params are null for workflow " + workflowExecution.getUuid(), workflowStandardParams);

      PhaseStepExecutionData phaseStepExecutionData =
          (PhaseStepExecutionData) stateExecutionInstance.getStateExecutionData();
      notNullCheck("phase step execution data is null for phase step " + phaseStepSubWorkflow.getId(),
          phaseStepExecutionData, USER_SRE);

      StateExecutionInstance phaseStateExecutionInstance = workflowExecutionService.getStateExecutionData(
          workflowExecution.getAppId(), stateExecutionInstance.getParentInstanceId());

      if (phaseStateExecutionInstance != null) {
        StateExecutionData stateExecutionData = phaseStateExecutionInstance.getStateExecutionData();
        notNullCheck("state execution data is null for " + phaseStepSubWorkflow.getParentId(), stateExecutionData);

        if (stateExecutionData instanceof PhaseExecutionData) {
          extractInstanceOrDeploymentInfoBaseOnType(context.getStateExecutionInstanceId(),
              (PhaseExecutionData) stateExecutionData, phaseStepExecutionData, workflowStandardParams,
              context.getAppId(), workflowExecution, phaseStepSubWorkflow, context);
        } else {
          logger.warn("Fetched execution data is not of type phase for phase step {}", phaseStepSubWorkflow.getId());
        }
      } else {
        logger.warn("Could not locate phase for phase step {}", phaseStepSubWorkflow.getId());
      }
    }
  }

  public String manualSync(String appId, String infraMappingId) {
    String syncJobId = UUIDGenerator.generateUuid();
    InfrastructureMapping infrastructureMapping = infraMappingService.get(appId, infraMappingId);
    instanceService.saveManualSyncJob(ManualSyncJob.builder().uuid(syncJobId).appId(appId).build());
    executorService.submit(() -> {
      try {
        syncNow(appId, infrastructureMapping);
      } finally {
        instanceService.deleteManualSyncJob(appId, syncJobId);
      }
    });
    return syncJobId;
  }

  public void syncNow(String appId, InfrastructureMapping infraMapping) {
    if (infraMapping == null) {
      return;
    }

    String infraMappingId = infraMapping.getUuid();
    InfrastructureMappingType infraMappingType =
        Util.getEnumFromString(InfrastructureMappingType.class, infraMapping.getInfraMappingType());
    try (AcquiredLock lock =
             persistentLocker.tryToAcquireLock(InfrastructureMapping.class, infraMappingId, Duration.ofSeconds(180))) {
      if (lock == null) {
        logger.warn("Couldn't acquire infra lock for infraMappingId [{}] of appId [{}]", infraMappingId, appId);
        return;
      }

      try {
        InstanceHandler instanceHandler = instanceHandlerFactory.getInstanceHandler(infraMappingType);
        if (instanceHandler == null) {
          logger.warn("Instance handler null for infraMappingId [{}] of appId [{}]", infraMappingId, appId);
          return;
        }
        logger.info("Instance sync started for infraMapping [{}]", infraMappingId);
        instanceHandler.syncInstances(appId, infraMappingId);
        instanceService.updateSyncSuccess(appId, infraMapping.getServiceId(), infraMapping.getEnvId(), infraMappingId,
            infraMapping.getName(), System.currentTimeMillis());
        logger.info("Instance sync completed for infraMapping [{}]", infraMappingId);
      } catch (Exception ex) {
        logger.warn(format("Instance sync failed for infraMappingId [%s]", infraMappingId), ex);
        String errorMsg;
        if (ex instanceof WingsException) {
          errorMsg = ex.getMessage();
        } else {
          errorMsg = ex.getMessage() != null ? ex.getMessage() : "Unknown error";
        }
        instanceService.updateSyncFailure(appId, infraMapping.getServiceId(), infraMapping.getEnvId(), infraMappingId,
            infraMapping.getName(), System.currentTimeMillis(), errorMsg);
      }
    }
  }

  public List<Boolean> getManualSyncJobsStatus(Set<String> manualSyncJobIdSet) {
    return instanceService.getManualSyncJobsStatus(manualSyncJobIdSet);
  }
}
