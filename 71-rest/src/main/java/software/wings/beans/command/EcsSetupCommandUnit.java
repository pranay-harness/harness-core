package software.wings.beans.command;

import static io.harness.exception.WingsException.USER;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.amazonaws.services.ecs.model.CreateServiceRequest;
import com.amazonaws.services.ecs.model.CreateServiceResult;
import com.amazonaws.services.ecs.model.DeleteServiceRequest;
import com.amazonaws.services.ecs.model.Service;
import com.amazonaws.services.ecs.model.TaskDefinition;
import com.amazonaws.services.ecs.model.UpdateServiceRequest;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.WingsException;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.DeploymentType;
import software.wings.beans.AwsConfig;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.ContainerSetupCommandUnitExecutionData.ContainerSetupCommandUnitExecutionDataBuilder;
import software.wings.beans.container.EcsContainerTask;
import software.wings.beans.container.EcsServiceSpecification;
import software.wings.cloudprovider.aws.AwsClusterService;
import software.wings.cloudprovider.aws.EcsContainerService;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.AwsHelperService;
import software.wings.utils.EcsConvention;
import software.wings.utils.Misc;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created by brett on 11/18/17
 */
public class EcsSetupCommandUnit extends ContainerSetupCommandUnit {
  @Transient private static final Logger logger = LoggerFactory.getLogger(EcsSetupCommandUnit.class);
  public static final String ERROR = "Error: ";

  @Inject @Transient private transient AwsClusterService awsClusterService;
  @Inject @Transient private transient EcsContainerService ecsContainerService;
  @Inject @Transient private transient AwsHelperService awsHelperService;
  @Inject @Transient private transient EcsCommandUnitHelper ecsCommandUnitHelper;
  static final String CONTAINER_NAME_PLACEHOLDER_REGEX = "\\$\\{CONTAINER_NAME}";

  public EcsSetupCommandUnit() {
    super(CommandUnitType.ECS_SETUP);
    setDeploymentType(DeploymentType.ECS.name());
  }

  @Override
  protected CommandExecutionStatus executeInternal(CommandExecutionContext context,
      SettingAttribute cloudProviderSetting, List<EncryptedDataDetail> encryptedDataDetails,
      ContainerSetupParams containerSetupParams, Map<String, String> serviceVariables,
      Map<String, String> safeDisplayServiceVariables, ExecutionLogCallback executionLogCallback) {
    ContainerSetupCommandUnitExecutionDataBuilder commandExecutionDataBuilder =
        ContainerSetupCommandUnitExecutionData.builder();
    try {
      EcsSetupParams setupParams = (EcsSetupParams) containerSetupParams;

      // Rollback Setup happens for ECS Daemon service, as for daemon scheduling strategy actual deployment happens in
      // setup state. in this case, we dont need to launch actual tasks, as ECS will schedule 1 task per instance in
      // cluster so only thing we need to do is, create taskDefinition and service.
      if (setupParams.isRollback()) {
        handleRollback(
            setupParams, cloudProviderSetting, commandExecutionDataBuilder, encryptedDataDetails, executionLogCallback);
      } else {
        String dockerImageName = setupParams.getImageDetails().getName() + ":" + setupParams.getImageDetails().getTag();
        String containerName = EcsConvention.getContainerName(dockerImageName);
        String domainName = setupParams.getImageDetails().getDomainName();

        EcsContainerTask ecsContainerTask = (EcsContainerTask) setupParams.getContainerTask();
        ecsContainerTask = createEcsContainerTaskIfNull(ecsContainerTask);

        executionLogCallback.saveExecutionLog("Cluster Name: " + setupParams.getClusterName(), LogLevel.INFO);
        executionLogCallback.saveExecutionLog("Docker Image Name: " + dockerImageName, LogLevel.INFO);
        executionLogCallback.saveExecutionLog("Container Name: " + containerName, LogLevel.INFO);

        // create Task definition and register it with AWS
        TaskDefinition taskDefinition = ecsCommandUnitHelper.createTaskDefinition(ecsContainerTask, containerName,
            dockerImageName, setupParams, cloudProviderSetting, serviceVariables, safeDisplayServiceVariables,
            encryptedDataDetails, executionLogCallback, domainName, awsClusterService);

        if (setupParams.getEcsServiceSpecification() != null
            && StringUtils.isNotBlank(setupParams.getEcsServiceSpecification().getServiceSpecJson())) {
          EcsServiceSpecification specification = setupParams.getEcsServiceSpecification();
          specification.setServiceSpecJson(
              specification.getServiceSpecJson().replaceAll(CONTAINER_NAME_PLACEHOLDER_REGEX, containerName));
        }

        // For REPLICA STRATEGY
        if (!setupParams.isDaemonSchedulingStrategy()) {
          String containerServiceName =
              EcsConvention.getServiceName(setupParams.getTaskFamily(), taskDefinition.getRevision());

          Map<String, Integer> activeServiceCounts = awsClusterService.getActiveServiceCounts(setupParams.getRegion(),
              cloudProviderSetting, encryptedDataDetails, setupParams.getClusterName(), containerServiceName);

          commandExecutionDataBuilder.containerServiceName(containerServiceName)
              .activeServiceCounts(integerMapToListOfStringArray(activeServiceCounts));

          CreateServiceRequest createServiceRequest =
              ecsCommandUnitHelper.getCreateServiceRequest(cloudProviderSetting, encryptedDataDetails, setupParams,
                  taskDefinition, containerServiceName, awsClusterService, executionLogCallback, logger);

          executionLogCallback.saveExecutionLog(
              format("Creating ECS service %s in cluster %s ", containerServiceName, setupParams.getClusterName()),
              LogLevel.INFO);

          // create and register service with aws
          awsClusterService.createService(
              setupParams.getRegion(), cloudProviderSetting, encryptedDataDetails, createServiceRequest);

          try {
            // This should not halt workflow execution.
            ecsCommandUnitHelper.downsizeOldOrUnhealthy(cloudProviderSetting, setupParams, containerServiceName,
                encryptedDataDetails, awsClusterService, awsHelperService, ecsContainerService, executionLogCallback);
          } catch (Exception e) {
            logger.warn("Cleaning up of old or unhealthy instances failed while setting up ECS service: ", e);
          }

          ecsCommandUnitHelper.cleanup(cloudProviderSetting, setupParams.getRegion(), containerServiceName,
              setupParams.getClusterName(), encryptedDataDetails, awsClusterService, executionLogCallback);

        } else {
          // For DAEMON Scheduling Strategy
          handleDaemonServiceRequest(setupParams, taskDefinition, executionLogCallback, cloudProviderSetting,
              encryptedDataDetails, commandExecutionDataBuilder);
        }

        // Log load-balancer details
        if (setupParams.isUseLoadBalancer()) {
          executionLogCallback.saveExecutionLog(
              "Load Balancer Name: " + setupParams.getLoadBalancerName(), LogLevel.INFO);
          executionLogCallback.saveExecutionLog("Target Group ARN: " + setupParams.getTargetGroupArn(), LogLevel.INFO);
          if (isNotBlank(setupParams.getRoleArn())) {
            executionLogCallback.saveExecutionLog("Role ARN: " + setupParams.getRoleArn(), LogLevel.INFO);
          }
        }
      }
      return CommandExecutionStatus.SUCCESS;
    } catch (Exception ex) {
      logger.error(Misc.getMessage(ex), ex);
      Misc.logAllMessages(ex, executionLogCallback);
      return CommandExecutionStatus.FAILURE;
    } finally {
      context.setCommandExecutionData(commandExecutionDataBuilder.build());
    }
  }

  private void handleRollback(EcsSetupParams setupParams, SettingAttribute cloudProviderSetting,
      ContainerSetupCommandUnitExecutionDataBuilder commandExecutionDataBuilder,
      List<EncryptedDataDetail> encryptedDataDetails, ExecutionLogCallback executionLogCallback) {
    if (setupParams.isDaemonSchedulingStrategy()) {
      try {
        // For Daemon service, we cache service spec json for existing service before we did actual deployment,
        // as deployment is being rolled back, update service with same service spec to restore it to original state
        if (EmptyPredicate.isNotEmpty(setupParams.getPreviousEcsServiceSnapshotJson())) {
          Service previousServiceSnapshot =
              ecsCommandUnitHelper.getAwsServiceFromJson(setupParams.getPreviousEcsServiceSnapshotJson(), logger);
          UpdateServiceRequest updateServiceRequest =
              new UpdateServiceRequest()
                  .withService(previousServiceSnapshot.getServiceName())
                  .withCluster(setupParams.getClusterName())
                  .withNetworkConfiguration(previousServiceSnapshot.getNetworkConfiguration())
                  .withTaskDefinition(previousServiceSnapshot.getTaskDefinition())
                  .withDeploymentConfiguration(previousServiceSnapshot.getDeploymentConfiguration())
                  .withHealthCheckGracePeriodSeconds(previousServiceSnapshot.getHealthCheckGracePeriodSeconds());

          awsHelperService.updateService(setupParams.getRegion(), (AwsConfig) cloudProviderSetting.getValue(),
              encryptedDataDetails, updateServiceRequest);
          ecsContainerService.waitForDaemonServiceToReachSteadyState(setupParams.getRegion(), cloudProviderSetting,
              encryptedDataDetails, setupParams.getClusterName(), previousServiceSnapshot.getServiceName(),
              setupParams.getServiceSteadyStateTimeout(), executionLogCallback);
          commandExecutionDataBuilder.containerServiceName(previousServiceSnapshot.getServiceName())
              .ecsTaskDefintion(previousServiceSnapshot.getTaskDefinition())
              .ecsServiceArn(previousServiceSnapshot.getServiceArn());
        } else {
          // For Daemon service, if first launch of the service fails, we delete that service, as there is no way to set
          // 0 tasks for such service. If we dont delete it, ECS will keep trying scheduling tasks on all instances with
          // cluster
          DeleteServiceRequest deleteServiceRequest = new DeleteServiceRequest()
                                                          .withService(setupParams.getEcsServiceArn())
                                                          .withCluster(setupParams.getClusterName());
          executionLogCallback.saveExecutionLog(new StringBuilder(32)
                                                    .append("Deleting Service")
                                                    .append(setupParams.getTaskFamily())
                                                    .append(" as first launch of service failed: ")
                                                    .toString());
          awsHelperService.deleteService(setupParams.getRegion(), (AwsConfig) cloudProviderSetting.getValue(),
              encryptedDataDetails, deleteServiceRequest);
        }
      } catch (Exception e) {
        String errorMsg = "Failed while handling rollback";
        logger.error(errorMsg, e);
        throw new WingsException(errorMsg, e, USER).addParam("message", errorMsg);
      }
    }
  }

  private void handleDaemonServiceRequest(EcsSetupParams setupParams, TaskDefinition taskDefinition,
      ExecutionLogCallback executionLogCallback, SettingAttribute cloudProviderSetting,
      List<EncryptedDataDetail> encryptedDataDetails,
      ContainerSetupCommandUnitExecutionDataBuilder commandExecutionDataBuilder) {
    // Get existing service. In case of Daemon service, we do not use versioning. We update existing service with new
    // task definition and some service configs mentioned in service spec
    Optional<Service> existingServiceMetadataSnapshot = ecsCommandUnitHelper.getExistingServiceMetadataSnapshot(
        setupParams, cloudProviderSetting, encryptedDataDetails, setupParams.getTaskFamily(), awsHelperService);

    // We just use mapper to deserialize service Spec.We then use this object to get configs we want to updat e with
    // service
    CreateServiceRequest createServiceRequest =
        ecsCommandUnitHelper.getCreateServiceRequest(cloudProviderSetting, encryptedDataDetails, setupParams,
            taskDefinition, setupParams.getTaskFamily(), awsClusterService, executionLogCallback, logger);

    if (existingServiceMetadataSnapshot.isPresent()) {
      Service service = existingServiceMetadataSnapshot.get();
      // Store existing service json spec, so it can be used if rollback is required
      commandExecutionDataBuilder
          .previousEcsServiceSnapshotJson(ecsCommandUnitHelper.getJsonForAwsServiceConfig(service, logger))
          .containerServiceName(service.getServiceName())
          .ecsTaskDefintion(service.getTaskDefinition());
      commandExecutionDataBuilder.ecsServiceArn(service.getServiceArn());
      // Update existing service
      UpdateServiceRequest updateServiceRequest =
          new UpdateServiceRequest()
              .withService(createServiceRequest.getServiceName())
              .withCluster(setupParams.getClusterName())
              .withNetworkConfiguration(createServiceRequest.getNetworkConfiguration())
              .withTaskDefinition(createServiceRequest.getTaskDefinition())
              .withDeploymentConfiguration(createServiceRequest.getDeploymentConfiguration())
              .withHealthCheckGracePeriodSeconds(createServiceRequest.getHealthCheckGracePeriodSeconds());

      awsHelperService.updateService(setupParams.getRegion(), (AwsConfig) cloudProviderSetting.getValue(),
          encryptedDataDetails, updateServiceRequest);
    } else {
      CreateServiceResult createServiceResult = awsHelperService.createService(setupParams.getRegion(),
          (AwsConfig) cloudProviderSetting.getValue(), encryptedDataDetails, createServiceRequest);
      commandExecutionDataBuilder.ecsServiceArn(createServiceResult.getService().getServiceArn())
          .containerServiceName(createServiceResult.getService().getServiceName())
          .ecsTaskDefintion(createServiceResult.getService().getTaskDefinition());
    }

    // Wait for all tasks to be up and service to reach steady state
    ecsContainerService.waitForDaemonServiceToReachSteadyState(setupParams.getRegion(), cloudProviderSetting,
        encryptedDataDetails, setupParams.getClusterName(), createServiceRequest.getServiceName(),
        setupParams.getServiceSteadyStateTimeout(), executionLogCallback);
  }

  private EcsContainerTask createEcsContainerTaskIfNull(EcsContainerTask ecsContainerTask) {
    if (ecsContainerTask == null) {
      ecsContainerTask = new EcsContainerTask();
      software.wings.beans.container.ContainerDefinition containerDefinition =
          software.wings.beans.container.ContainerDefinition.builder()
              .memory(256)
              .cpu(1)
              .portMappings(emptyList())
              .build();
      ecsContainerTask.setContainerDefinitions(Lists.newArrayList(containerDefinition));
    }

    return ecsContainerTask;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @JsonTypeName("ECS_SETUP")
  public static class Yaml extends ContainerSetupCommandUnit.Yaml {
    public Yaml() {
      super(CommandUnitType.ECS_SETUP.name());
    }

    @Builder
    public Yaml(String name, String deploymentType) {
      super(name, CommandUnitType.ECS_SETUP.name(), deploymentType);
    }
  }
}
