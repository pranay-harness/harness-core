package software.wings.service.impl.instance;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.validation.Validator.notNullCheck;
import static java.util.Collections.singletonList;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.inject.Inject;

import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import software.wings.annotation.EncryptableSetting;
import software.wings.api.AwsCodeDeployDeploymentInfo;
import software.wings.api.CommandStepExecutionSummary;
import software.wings.api.DeploymentInfo;
import software.wings.api.DeploymentSummary;
import software.wings.api.PhaseExecutionData;
import software.wings.api.PhaseStepExecutionData;
import software.wings.api.ondemandrollback.OnDemandRollbackInfo;
import software.wings.beans.AwsConfig;
import software.wings.beans.CodeDeployInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.command.CodeDeployParams;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.Instance.InstanceBuilder;
import software.wings.beans.infrastructure.instance.info.CodeDeployInstanceInfo;
import software.wings.beans.infrastructure.instance.info.Ec2InstanceInfo;
import software.wings.beans.infrastructure.instance.info.InstanceInfo;
import software.wings.beans.infrastructure.instance.key.deployment.AwsCodeDeployDeploymentKey;
import software.wings.beans.infrastructure.instance.key.deployment.DeploymentKey;
import software.wings.service.intfc.aws.manager.AwsCodeDeployHelperServiceManager;
import software.wings.sm.PhaseStepExecutionSummary;
import software.wings.sm.StepExecutionSummary;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author rktummala on 01/30/18
 */
@Slf4j
public class AwsCodeDeployInstanceHandler extends AwsInstanceHandler {
  @Inject private AwsCodeDeployHelperServiceManager awsCodeDeployHelperServiceManager;

  @Override
  public Optional<List<DeploymentInfo>> getDeploymentInfo(PhaseExecutionData phaseExecutionData,
      PhaseStepExecutionData phaseStepExecutionData, WorkflowExecution workflowExecution,
      InfrastructureMapping infrastructureMapping, String stateExecutionInstanceId, Artifact artifact) {
    PhaseStepExecutionSummary phaseStepExecutionSummary = phaseStepExecutionData.getPhaseStepExecutionSummary();

    if (phaseStepExecutionSummary != null) {
      Optional<StepExecutionSummary> stepExecutionSummaryOptional =
          phaseStepExecutionSummary.getStepExecutionSummaryList()
              .stream()
              .filter(stepExecutionSummary -> stepExecutionSummary instanceof CommandStepExecutionSummary)
              .findFirst();

      if (stepExecutionSummaryOptional.isPresent()) {
        StepExecutionSummary stepExecutionSummary = stepExecutionSummaryOptional.get();

        CommandStepExecutionSummary commandStepExecutionSummary = (CommandStepExecutionSummary) stepExecutionSummary;
        CodeDeployParams codeDeployParams = commandStepExecutionSummary.getCodeDeployParams();
        if (codeDeployParams == null) {
          logger.warn("Phase step execution summary null for Deploy for workflow:{} Can't create deployment event",
              workflowExecution.normalizedName());
          return Optional.empty();
        }

        return Optional.of(singletonList(AwsCodeDeployDeploymentInfo.builder()
                                             .deploymentGroupName(codeDeployParams.getDeploymentGroupName())
                                             .key(codeDeployParams.getKey())
                                             .applicationName(codeDeployParams.getApplicationName())
                                             .deploymentId(commandStepExecutionSummary.getCodeDeployDeploymentId())
                                             .build()));

      } else {
        throw WingsException.builder()
            .message("Command step execution summary null for workflow: " + workflowExecution.normalizedName())
            .build();
      }
    } else {
      return Optional.empty();
    }
  }

  @Override
  public void syncInstances(String appId, String infraMappingId) {
    syncInstancesInternal(appId, infraMappingId, null, false);
  }

  private void syncInstancesInternal(
      String appId, String infraMappingId, List<DeploymentSummary> newDeploymentSummaries, boolean rollback) {
    InfrastructureMapping infrastructureMapping = infraMappingService.get(appId, infraMappingId);
    notNullCheck("Infra mapping is null for id:" + infraMappingId, infrastructureMapping);
    if (!(infrastructureMapping instanceof CodeDeployInfrastructureMapping)) {
      String msg = "Incompatible infra mapping type. Expecting code deploy type. Found:"
          + infrastructureMapping.getInfraMappingType();
      logger.error(msg);
      throw WingsException.builder().message(msg).build();
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
        secretManager.getEncryptionDetails((EncryptableSetting) cloudProviderSetting.getValue(), null, null);

    CodeDeployInfrastructureMapping codeDeployInfraMapping = (CodeDeployInfrastructureMapping) infrastructureMapping;
    String region = codeDeployInfraMapping.getRegion();

    if (isNotEmpty(newDeploymentSummaries)) {
      newDeploymentSummaries.forEach(newDeploymentSummary -> {
        AwsCodeDeployDeploymentInfo awsCodeDeployDeploymentInfo =
            (AwsCodeDeployDeploymentInfo) newDeploymentSummary.getDeploymentInfo();

        // instancesInDBMap contains all instancesInDB for current appId and infraMapId
        Map<String, Instance> instancesInDBMap =
            instancesInDB.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(this ::getKeyFromInstance, instance -> instance));

        // This will create filter for "instance-state-name" = "running"
        List<com.amazonaws.services.ec2.model.Instance> latestEc2Instances =
            awsCodeDeployHelperServiceManager.listDeploymentInstances(awsConfig, encryptedDataDetails, region,
                awsCodeDeployDeploymentInfo.getDeploymentId(), codeDeployInfraMapping.getAppId());
        Map<String, com.amazonaws.services.ec2.model.Instance> latestEc2InstanceMap =
            latestEc2Instances.stream().collect(
                Collectors.toMap(com.amazonaws.services.ec2.model.Instance::getInstanceId, ec2Instance -> ec2Instance));

        SetView<String> instancesToBeUpdated =
            Sets.intersection(latestEc2InstanceMap.keySet(), instancesInDBMap.keySet());

        instancesToBeUpdated.forEach(ec2InstanceId -> {
          // change to codeDeployInstance builder
          Instance oldInstance = instancesInDBMap.get(ec2InstanceId);
          com.amazonaws.services.ec2.model.Instance ec2Instance = latestEc2InstanceMap.get(ec2InstanceId);
          Instance instance = buildInstanceUsingEc2Instance(null, ec2Instance, infrastructureMapping,
              getDeploymentSummaryForInstanceCreation(newDeploymentSummary, rollback));
          if (oldInstance != null) {
            instanceService.update(instance, oldInstance.getUuid());
          } else {
            logger.error("Instance doesn't exist for given ec2 instance id {}", ec2InstanceId);
          }
        });

        logger.info("Instances to be updated {}", instancesToBeUpdated.size());

        // Find the instances that were yet to be added to db
        SetView<String> instancesToBeAdded = Sets.difference(latestEc2InstanceMap.keySet(), instancesInDBMap.keySet());

        DeploymentSummary deploymentSummary;
        if (isNotEmpty(instancesToBeAdded)) {
          deploymentSummary = getDeploymentSummaryForInstanceCreation(newDeploymentSummary, rollback);

          instancesToBeAdded.forEach(ec2InstanceId -> {
            com.amazonaws.services.ec2.model.Instance ec2Instance = latestEc2InstanceMap.get(ec2InstanceId);
            // change to codeDeployInstance builder
            Instance instance =
                buildInstanceUsingEc2Instance(null, ec2Instance, infrastructureMapping, deploymentSummary);
            instanceService.save(instance);
          });

          logger.info("Instances to be added {}", instancesToBeAdded.size());
        }
      });
    }

    handleEc2InstanceSync(ec2InstanceIdInstanceMap, awsConfig, encryptedDataDetails, region);
  }

  private Instance buildInstanceUsingEc2Instance(String instanceUuid,
      com.amazonaws.services.ec2.model.Instance ec2Instance, InfrastructureMapping infraMapping,
      DeploymentSummary deploymentSummary) {
    InstanceBuilder builder = buildInstanceBase(instanceUuid, infraMapping, deploymentSummary);
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
  public void handleNewDeployment(List<DeploymentSummary> deploymentSummaries, boolean rollback,
      OnDemandRollbackInfo onDemandRollbackInfo) throws WingsException {
    syncInstancesInternal(deploymentSummaries.iterator().next().getAppId(),
        deploymentSummaries.iterator().next().getInfraMappingId(), deploymentSummaries, rollback);
  }

  @Override
  public DeploymentKey generateDeploymentKey(DeploymentInfo deploymentInfo) {
    return AwsCodeDeployDeploymentKey.builder().key(((AwsCodeDeployDeploymentInfo) deploymentInfo).getKey()).build();
  }

  @Override
  protected void setDeploymentKey(DeploymentSummary deploymentSummary, DeploymentKey deploymentKey) {
    if (deploymentKey instanceof AwsCodeDeployDeploymentKey) {
      deploymentSummary.setAwsCodeDeployDeploymentKey((AwsCodeDeployDeploymentKey) deploymentKey);
    } else {
      throw WingsException.builder()
          .message("Invalid deploymentKey passed for AwsCodeDeployDeploymentKey" + deploymentKey)
          .build();
    }
  }
}
