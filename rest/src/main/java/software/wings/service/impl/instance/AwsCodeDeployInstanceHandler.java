package software.wings.service.impl.instance;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.inject.Inject;

import software.wings.annotation.Encryptable;
import software.wings.api.AwsCodeDeployDeploymentInfo;
import software.wings.api.CommandStepExecutionSummary;
import software.wings.api.DeploymentInfo;
import software.wings.api.PhaseExecutionData;
import software.wings.beans.AwsConfig;
import software.wings.beans.CodeDeployInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.Instance.InstanceBuilder;
import software.wings.beans.infrastructure.instance.info.CodeDeployInstanceInfo;
import software.wings.beans.infrastructure.instance.info.Ec2InstanceInfo;
import software.wings.beans.infrastructure.instance.info.InstanceInfo;
import software.wings.cloudprovider.aws.AwsCodeDeployService;
import software.wings.common.Constants;
import software.wings.exception.HarnessException;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.sm.PhaseStepExecutionSummary;
import software.wings.sm.StepExecutionSummary;
import software.wings.utils.Validator;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author rktummala on 01/30/18
 */
public class AwsCodeDeployInstanceHandler extends AwsInstanceHandler {
  @Inject private InfrastructureMappingService infraMappingService;
  @Inject private AwsCodeDeployService awsCodeDeployService;

  @Override
  public Optional<DeploymentInfo> getDeploymentInfo(PhaseExecutionData phaseExecutionData,
      WorkflowExecution workflowExecution, InfrastructureMapping infrastructureMapping, String stateExecutionInstanceId,
      Artifact artifact) throws HarnessException {
    String codeDeployDeploymentId = getCodeDeployDeploymentId(phaseExecutionData, workflowExecution);

    if (codeDeployDeploymentId == null) {
      logger.warn("Phase step execution summary null for Deploy for workflow:{} Can't create deployment event",
          workflowExecution.getName());
      return Optional.empty();
    }
    return Optional.of(AwsCodeDeployDeploymentInfo.builder().deploymentId(codeDeployDeploymentId).build());
  }

  private String getCodeDeployDeploymentId(PhaseExecutionData phaseExecutionData, WorkflowExecution workflowExecution)
      throws HarnessException {
    PhaseStepExecutionSummary phaseStepExecutionSummary =
        instanceHelper.getDeployPhaseStep(phaseExecutionData, Constants.DEPLOY_SERVICE);
    if (phaseStepExecutionSummary != null) {
      Optional<StepExecutionSummary> stepExecutionSummaryOptional =
          phaseStepExecutionSummary.getStepExecutionSummaryList()
              .stream()
              .filter(stepExecutionSummary -> stepExecutionSummary instanceof CommandStepExecutionSummary)
              .findFirst();

      if (stepExecutionSummaryOptional.isPresent()) {
        StepExecutionSummary stepExecutionSummary = stepExecutionSummaryOptional.get();

        CommandStepExecutionSummary commandStepExecutionSummary = (CommandStepExecutionSummary) stepExecutionSummary;
        return commandStepExecutionSummary.getCodeDeployDeploymentId();

      } else {
        throw new HarnessException("Command step execution summary null for workflow: " + workflowExecution.getName());
      }

    } else {
      return null;
    }
  }

  @Override
  public void syncInstances(String appId, String infraMappingId) throws HarnessException {
    syncInstancesInternal(appId, infraMappingId, null);
  }

  private void syncInstancesInternal(String appId, String infraMappingId, DeploymentInfo newDeploymentInfo)
      throws HarnessException {
    InfrastructureMapping infrastructureMapping = infraMappingService.get(appId, infraMappingId);
    Validator.notNullCheck("Infra mapping is null for id:" + infraMappingId, infrastructureMapping);
    if (!(infrastructureMapping instanceof CodeDeployInfrastructureMapping)) {
      String msg = "Incompatible infra mapping type. Expecting code deploy type. Found:"
          + infrastructureMapping.getInfraMappingType();
      logger.error(msg);
      throw new HarnessException(msg);
    }

    // key - ec2 instance id, value - instance
    Map<String, Instance> ec2InstanceIdInstanceMap = Maps.newHashMap();

    List<Instance> instancesInDB = getInstances(appId, infraMappingId);

    instancesInDB.forEach(instance -> {
      InstanceInfo instanceInfo = instance.getInstanceInfo();
      if (instanceInfo instanceof Ec2InstanceInfo) {
        Ec2InstanceInfo ec2InstanceInfo = (Ec2InstanceInfo) instanceInfo;
        com.amazonaws.services.ec2.model.Instance ec2Instance = ec2InstanceInfo.getEc2Instance();
        String ec2InstanceId = ec2Instance.getInstanceId();
        ec2InstanceIdInstanceMap.put(ec2InstanceId, instance);
      }
    });

    SettingAttribute cloudProviderSetting = settingsService.get(infrastructureMapping.getComputeProviderSettingId());
    AwsConfig awsConfig = (AwsConfig) cloudProviderSetting.getValue();

    List<EncryptedDataDetail> encryptedDataDetails =
        secretManager.getEncryptionDetails((Encryptable) cloudProviderSetting.getValue(), null, null);

    CodeDeployInfrastructureMapping codeDeployInfraMapping = (CodeDeployInfrastructureMapping) infrastructureMapping;
    String region = codeDeployInfraMapping.getRegion();

    if (newDeploymentInfo != null) {
      AwsCodeDeployDeploymentInfo awsCodeDeployDeploymentInfo = (AwsCodeDeployDeploymentInfo) newDeploymentInfo;

      // instancesInDBMap contains all instancesInDB for current appId and infraMapId
      Map<String, Instance> instancesInDBMap =
          instancesInDB.stream()
              .filter(instance -> instance != null)
              .collect(Collectors.toMap(instance -> getKeyFromInstance(instance), instance -> instance));

      // This will create filter for "instance-state-name" = "running"
      List<com.amazonaws.services.ec2.model.Instance> latestEc2Instances = awsCodeDeployService.listDeploymentInstances(
          region, cloudProviderSetting, encryptedDataDetails, awsCodeDeployDeploymentInfo.getDeploymentId());
      Map<String, com.amazonaws.services.ec2.model.Instance> latestEc2InstanceMap = latestEc2Instances.stream().collect(
          Collectors.toMap(ec2Instance -> ec2Instance.getInstanceId(), ec2Instance -> ec2Instance));

      SetView<String> instancesToBeUpdated =
          Sets.intersection(latestEc2InstanceMap.keySet(), instancesInDBMap.keySet());

      if (newDeploymentInfo != null) {
        instancesToBeUpdated.stream().forEach(ec2InstanceId -> {
          // change to codeDeployInstance builder
          Instance instance = instancesInDBMap.get(ec2InstanceId);
          String uuid = null;
          if (instance != null) {
            uuid = instance.getUuid();
          }
          com.amazonaws.services.ec2.model.Instance ec2Instance = latestEc2InstanceMap.get(ec2InstanceId);
          instance = buildInstanceUsingEc2Instance(uuid, ec2Instance, infrastructureMapping, newDeploymentInfo);
          instanceService.saveOrUpdate(instance);
        });
      }

      // Find the instances that were yet to be added to db
      SetView<String> instancesToBeAdded = Sets.difference(latestEc2InstanceMap.keySet(), instancesInDBMap.keySet());

      DeploymentInfo deploymentInfo;
      if (isNotEmpty(instancesToBeAdded)) {
        // newDeploymentInfo would be null in case of sync job.
        if (newDeploymentInfo == null && isNotEmpty(instancesInDB)) {
          Optional<Instance> instanceWithExecutionInfoOptional = getInstanceWithExecutionInfo(instancesInDB);
          if (!instanceWithExecutionInfoOptional.isPresent()) {
            logger.warn("Couldn't find an instance from a previous deployment for inframapping {}",
                infrastructureMapping.getUuid());
            return;
          }

          DeploymentInfo deploymentInfoFromPrevious = AwsCodeDeployDeploymentInfo.builder().build();
          generateDeploymentInfoFromInstance(instanceWithExecutionInfoOptional.get(), deploymentInfoFromPrevious);
          deploymentInfo = deploymentInfoFromPrevious;
        } else {
          deploymentInfo = newDeploymentInfo;
        }

        instancesToBeAdded.stream().forEach(ec2InstanceId -> {
          com.amazonaws.services.ec2.model.Instance ec2Instance = latestEc2InstanceMap.get(ec2InstanceId);
          // change to codeDeployInstance builder
          Instance instance = buildInstanceUsingEc2Instance(null, ec2Instance, infrastructureMapping, deploymentInfo);
          instanceService.saveOrUpdate(instance);
        });
      }
    }

    handleEc2InstanceSync(ec2InstanceIdInstanceMap, awsConfig, encryptedDataDetails, region);
  }

  private Instance buildInstanceUsingEc2Instance(String instanceUuid,
      com.amazonaws.services.ec2.model.Instance ec2Instance, InfrastructureMapping infraMapping,
      DeploymentInfo deploymentInfo) {
    InstanceBuilder builder = buildInstanceBase(instanceUuid, infraMapping, deploymentInfo);
    instanceHelper.setInstanceInfoAndKey(builder, ec2Instance, infraMapping.getUuid());
    return builder.build();
  }

  private String getKeyFromInstance(Instance instance) {
    String instanceInfoString;
    if (instance.getInstanceInfo() instanceof Ec2InstanceInfo) {
      Ec2InstanceInfo ec2InstanceInfo = (Ec2InstanceInfo) instance.getInstanceInfo();
      instanceInfoString = ec2InstanceInfo.getEc2Instance().getInstanceId();
    } else {
      CodeDeployInstanceInfo instanceInfo = (CodeDeployInstanceInfo) instance.getInstanceInfo();
      instanceInfoString = instanceInfo.getEc2Instance().getInstanceId();
    }

    return instanceInfoString;
  }

  @Override
  public void handleNewDeployment(DeploymentInfo deploymentInfo) throws HarnessException {
    syncInstancesInternal(deploymentInfo.getAppId(), deploymentInfo.getInfraMappingId(), deploymentInfo);
  }
}
