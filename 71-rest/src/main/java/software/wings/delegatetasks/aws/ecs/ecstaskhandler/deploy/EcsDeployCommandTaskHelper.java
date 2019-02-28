package software.wings.delegatetasks.aws.ecs.ecstaskhandler.deploy;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static software.wings.beans.InstanceUnitType.PERCENTAGE;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.amazonaws.services.applicationautoscaling.model.DeregisterScalableTargetRequest;
import com.amazonaws.services.applicationautoscaling.model.DescribeScalableTargetsRequest;
import com.amazonaws.services.applicationautoscaling.model.DescribeScalableTargetsResult;
import com.amazonaws.services.applicationautoscaling.model.ScalableTarget;
import com.amazonaws.services.applicationautoscaling.model.ServiceNamespace;
import com.amazonaws.services.ecs.model.Service;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.exception.InvalidRequestException;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.ContainerServiceData;
import software.wings.beans.AwsConfig;
import software.wings.beans.command.EcsResizeParams;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.beans.container.AwsAutoScalarConfig;
import software.wings.cloudprovider.ContainerInfo;
import software.wings.cloudprovider.aws.AwsClusterService;
import software.wings.cloudprovider.aws.EcsContainerService;
import software.wings.delegatetasks.aws.ecs.ecstaskhandler.EcsCommandTaskHelper;
import software.wings.helpers.ext.ecs.response.EcsServiceDeployResponse;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.intfc.aws.delegate.AwsAppAutoScalingHelperServiceDelegate;
import software.wings.service.intfc.aws.delegate.AwsEcsHelperServiceDelegate;
import software.wings.utils.Misc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

@Singleton
public class EcsDeployCommandTaskHelper {
  @Transient private static final Logger logger = LoggerFactory.getLogger(EcsDeployCommandTaskHelper.class);
  @Inject private AwsClusterService awsClusterService;
  @Inject private AwsAppAutoScalingHelperServiceDelegate awsAppAutoScalingService;
  @Inject private AwsHelperService awsHelperService;
  @Inject private AwsEcsHelperServiceDelegate awsEcsHelperServiceDelegate;
  @Inject private EcsContainerService ecsContainerService;
  @Inject private EcsCommandTaskHelper ecsCommandTaskHelper;
  private static final String DELIMITER = "__";
  private static final String CONTAINER_NAME_PLACEHOLDER_REGEX = "\\$\\{CONTAINER_NAME}";

  public void deregisterAutoScalarsIfExists(ContextData contextData, ExecutionLogCallback executionLogCallback) {
    EcsResizeParams resizeParams = contextData.getResizeParams();
    if (resizeParams.isPreviousEcsAutoScalarsAlreadyRemoved()
        || isEmpty(resizeParams.getPreviousAwsAutoScalarConfigs())) {
      return;
    }

    resizeParams.getPreviousAwsAutoScalarConfigs().forEach(awsAutoScalarConfig -> {
      if (StringUtils.isNotBlank(awsAutoScalarConfig.getScalableTargetJson())) {
        ScalableTarget target =
            awsAppAutoScalingService.getScalableTargetFromJson(awsAutoScalarConfig.getScalableTargetJson());

        // Fetch target from AWS
        DescribeScalableTargetsResult result = awsAppAutoScalingService.listScalableTargets(resizeParams.getRegion(),
            contextData.getAwsConfig(), contextData.getEncryptedDataDetails(),
            new DescribeScalableTargetsRequest()
                .withResourceIds(target.getResourceId())
                .withServiceNamespace(ServiceNamespace.Ecs)
                .withScalableDimension(target.getScalableDimension()));

        // If Target exists, delete it
        if (isNotEmpty(result.getScalableTargets())) {
          executionLogCallback.saveExecutionLog("De-registering Scalable Target :" + target.getResourceId());
          awsAppAutoScalingService.deregisterScalableTarget(resizeParams.getRegion(), contextData.getAwsConfig(),
              contextData.getEncryptedDataDetails(),
              new DeregisterScalableTargetRequest()
                  .withResourceId(target.getResourceId())
                  .withScalableDimension(target.getScalableDimension())
                  .withServiceNamespace(ServiceNamespace.Ecs));
          executionLogCallback.saveExecutionLog(new StringBuilder()
                                                    .append("De-registered Scalable Target Successfully: ")
                                                    .append(target.getResourceId())
                                                    .append("\n")
                                                    .toString());
        }
      }
    });
  }

  /**
   * Delete autoScalar targets and policies created for New service that is being downsized in rollback
   * @param contextData
   * @param executionLogCallback
   */
  public void deleteAutoScalarForNewService(ContextData contextData, ExecutionLogCallback executionLogCallback) {
    EcsResizeParams resizeParams = contextData.getResizeParams();
    AwsConfig awsConfig = contextData.getAwsConfig();
    String serviceName = resizeParams.getContainerServiceName();
    String resourceId = ecsCommandTaskHelper.getResourceIdForEcsService(serviceName, resizeParams.getClusterName());

    DescribeScalableTargetsResult targetsResult = awsAppAutoScalingService.listScalableTargets(resizeParams.getRegion(),
        awsConfig, contextData.getEncryptedDataDetails(),
        new DescribeScalableTargetsRequest()
            .withServiceNamespace(ServiceNamespace.Ecs)
            .withResourceIds(Arrays.asList(resourceId)));

    if (isEmpty(targetsResult.getScalableTargets())) {
      return;
    }

    targetsResult.getScalableTargets().forEach(scalableTarget -> {
      executionLogCallback.saveExecutionLog("De-registering AutoScalable Target : " + scalableTarget.getResourceId());
      awsAppAutoScalingService.deregisterScalableTarget(resizeParams.getRegion(), awsConfig,
          contextData.getEncryptedDataDetails(),
          new DeregisterScalableTargetRequest()
              .withServiceNamespace(ServiceNamespace.Ecs)
              .withResourceId(resourceId)
              .withScalableDimension(scalableTarget.getScalableDimension()));
      executionLogCallback.saveExecutionLog(
          "Successfully De-registered AutoScalable Target : " + scalableTarget.getResourceId());
    });
  }

  public void restoreAutoScalarConfigs(
      ContextData contextData, ContainerServiceData containerServiceData, ExecutionLogCallback executionLogCallback) {
    EcsResizeParams resizeParams = contextData.getResizeParams();

    // This action is required in rollback stage only
    if (!resizeParams.isRollback()) {
      return;
    }

    // Delete eauto scalar for newly created Service that will be downsized as part of rollback
    deleteAutoScalarForNewService(contextData, executionLogCallback);

    // This is for services those are being upsized in Rollbak
    List<AwsAutoScalarConfig> awsAutoScalarConfigs = resizeParams.getPreviousAwsAutoScalarConfigs();

    if (isEmpty(awsAutoScalarConfigs)) {
      executionLogCallback.saveExecutionLog("No Auto-scalar configs to restore");
      return;
    }

    AwsConfig awsConfig = contextData.getAwsConfig();

    awsAutoScalarConfigs.forEach(awsAutoScalarConfig -> {
      if (StringUtils.isNotBlank(awsAutoScalarConfig.getScalableTargetJson())) {
        ScalableTarget scalableTarget =
            awsAppAutoScalingService.getScalableTargetFromJson(awsAutoScalarConfig.getScalableTargetJson());

        DescribeScalableTargetsResult result = awsAppAutoScalingService.listScalableTargets(resizeParams.getRegion(),
            awsConfig, contextData.getEncryptedDataDetails(),
            new DescribeScalableTargetsRequest()
                .withResourceIds(Arrays.asList(scalableTarget.getResourceId()))
                .withScalableDimension(scalableTarget.getScalableDimension())
                .withServiceNamespace(ServiceNamespace.Ecs));

        if (isEmpty(result.getScalableTargets())) {
          ecsCommandTaskHelper.registerScalableTargetForEcsService(awsAppAutoScalingService, resizeParams.getRegion(),
              awsConfig, contextData.getEncryptedDataDetails(), executionLogCallback, scalableTarget);

          if (isNotEmpty(awsAutoScalarConfig.getScalingPolicyJson())) {
            for (String policyJson : awsAutoScalarConfig.getScalingPolicyJson()) {
              ecsCommandTaskHelper.upsertScalingPolicyIfRequired(policyJson, scalableTarget.getResourceId(),
                  scalableTarget.getScalableDimension(), resizeParams.getRegion(), awsConfig, awsAppAutoScalingService,
                  contextData.getEncryptedDataDetails(), executionLogCallback);
            }
          }
        }
      }
    });
  }

  public void createAutoScalarConfigIfServiceReachedMaxSize(
      ContextData contextData, ContainerServiceData containerServiceData, ExecutionLogCallback executionLogCallback) {
    EcsResizeParams resizeParams = contextData.getResizeParams();

    int maxDesiredCount =
        resizeParams.isUseFixedInstances() ? resizeParams.getFixedInstances() : resizeParams.getMaxInstances();

    // If Rollback or this is not final resize stage, just return.
    // We create AutoScalar after new service has been completely upsized.
    if (resizeParams.isRollback() || maxDesiredCount > containerServiceData.getDesiredCount()) {
      return;
    }

    if (resizeParams.getAwsAutoScalarConfigForNewService() == null
        || isEmpty(resizeParams.getAwsAutoScalarConfigForNewService())) {
      executionLogCallback.saveExecutionLog("No Autoscalar config provided.");
      return;
    }

    String resourceId =
        ecsCommandTaskHelper.getResourceIdForEcsService(containerServiceData.getName(), resizeParams.getClusterName());

    AwsConfig awsConfig = contextData.getAwsConfig();
    executionLogCallback.saveExecutionLog(
        "\nRegistering Scalable Target for Service : " + containerServiceData.getName());
    resizeParams.getAwsAutoScalarConfigForNewService().forEach(awsAutoScalarConfig -> {
      if (StringUtils.isNotBlank(awsAutoScalarConfig.getScalableTargetJson())) {
        ScalableTarget scalableTarget =
            awsAppAutoScalingService.getScalableTargetFromJson(awsAutoScalarConfig.getScalableTargetJson());
        scalableTarget.withResourceId(resourceId);
        ecsCommandTaskHelper.registerScalableTargetForEcsService(awsAppAutoScalingService, resizeParams.getRegion(),
            awsConfig, contextData.getEncryptedDataDetails(), executionLogCallback, scalableTarget);

        if (isNotEmpty(awsAutoScalarConfig.getScalingPolicyJson())) {
          executionLogCallback.saveExecutionLog(
              "Creating Auto Scaling Policies for Service: " + containerServiceData.getName());
          for (String policyJson : awsAutoScalarConfig.getScalingPolicyJson()) {
            ecsCommandTaskHelper.upsertScalingPolicyIfRequired(policyJson, resourceId,
                scalableTarget.getScalableDimension(), resizeParams.getRegion(), awsConfig, awsAppAutoScalingService,
                contextData.getEncryptedDataDetails(), executionLogCallback);
          }
        }
      }
    });
  }

  public boolean getDeployingToHundredPercent(EcsResizeParams resizeParams) {
    boolean deployingToHundredPercent;
    if (!resizeParams.isRollback()) {
      Preconditions.checkNotNull(resizeParams.getInstanceCount());
      deployingToHundredPercent = resizeParams.getInstanceUnitType() == PERCENTAGE
          ? resizeParams.getInstanceCount() >= 100
          : (resizeParams.isUseFixedInstances() && resizeParams.getInstanceCount() >= resizeParams.getFixedInstances())
              || (!resizeParams.isUseFixedInstances()
                     && resizeParams.getInstanceCount() >= resizeParams.getMaxInstances());
    } else {
      deployingToHundredPercent = false;
    }

    return deployingToHundredPercent;
  }

  public ContainerServiceData getNewInstanceData(ContextData contextData) {
    Optional<Integer> previousDesiredCount = getServiceDesiredCount(contextData);

    String containerServiceName = contextData.getResizeParams().getContainerServiceName();
    if (!previousDesiredCount.isPresent()) {
      throw new InvalidRequestException("Service setup not done, service name: " + containerServiceName);
    }

    int previousCount = previousDesiredCount.get();
    int desiredCount = getNewInstancesDesiredCount(contextData);

    if (desiredCount < previousCount) {
      String msg = "Desired instance count must be greater than or equal to the current instance count: {current: "
          + previousCount + ", desired: " + desiredCount + "}";
      logger.error(msg);
      throw new InvalidRequestException(msg);
    }

    return ContainerServiceData.builder()
        .name(containerServiceName)
        .image(contextData.getResizeParams().getImage())
        .previousCount(previousCount)
        .desiredCount(desiredCount)
        .previousTraffic(0)
        .desiredTraffic(0)
        .build();
  }

  public Optional<Integer> getServiceDesiredCount(ContextData contextData) {
    EcsResizeParams resizeParams = contextData.getResizeParams();
    Optional<Service> service = awsClusterService
                                    .getServices(resizeParams.getRegion(), contextData.getSettingAttribute(),
                                        contextData.getEncryptedDataDetails(), resizeParams.getClusterName())
                                    .stream()
                                    .filter(svc -> svc.getServiceName().equals(resizeParams.getContainerServiceName()))
                                    .findFirst();
    return service.map(Service::getDesiredCount);
  }

  public int getNewInstancesDesiredCount(ContextData contextData) {
    EcsResizeParams resizeParams = contextData.getResizeParams();
    Preconditions.checkNotNull(resizeParams.getInstanceCount());
    int instanceCount = resizeParams.getInstanceCount();
    int totalTargetInstances =
        resizeParams.isUseFixedInstances() ? resizeParams.getFixedInstances() : resizeParams.getMaxInstances();
    return resizeParams.getInstanceUnitType() == PERCENTAGE
        ? (int) Math.round(Math.min(instanceCount, 100) * totalTargetInstances / 100.0)
        : Math.min(instanceCount, totalTargetInstances);
  }

  public int getDownsizeByAmount(
      ContextData contextData, int totalOtherInstances, int upsizeDesiredCount, int upsizePreviousCount) {
    EcsResizeParams resizeParams = contextData.getResizeParams();
    Integer downsizeDesiredCount = resizeParams.getDownsizeInstanceCount();
    if (downsizeDesiredCount != null) {
      int downsizeInstanceCount = resizeParams.getDownsizeInstanceCount();
      int totalTargetInstances =
          resizeParams.isUseFixedInstances() ? resizeParams.getFixedInstances() : resizeParams.getMaxInstances();
      int downsizeToCount = resizeParams.getDownsizeInstanceUnitType() == PERCENTAGE
          ? (int) Math.round(Math.min(downsizeInstanceCount, 100) * totalTargetInstances / 100.0)
          : Math.min(downsizeInstanceCount, totalTargetInstances);
      return Math.max(totalOtherInstances - downsizeToCount, 0);
    } else {
      return contextData.isDeployingToHundredPercent() ? totalOtherInstances
                                                       : Math.max(upsizeDesiredCount - upsizePreviousCount, 0);
    }
  }

  public List<ContainerServiceData> getOldInstanceData(ContextData contextData, ContainerServiceData newServiceData) {
    List<ContainerServiceData> oldInstanceData = new ArrayList<>();
    Map<String, Integer> previousCounts = getActiveServiceCounts(contextData);
    previousCounts.remove(newServiceData.getName());
    Map<String, String> previousImages = getActiveServiceImages(contextData);
    previousImages.remove(newServiceData.getName());

    int downsizeCount =
        getDownsizeByAmount(contextData, previousCounts.values().stream().mapToInt(Integer::intValue).sum(),
            newServiceData.getDesiredCount(), newServiceData.getPreviousCount());

    for (Entry<String, Integer> entry : previousCounts.entrySet()) {
      String serviceName = entry.getKey();
      String previousImage = previousImages.get(serviceName);
      int previousCount = entry.getValue();
      int desiredCount = Math.max(previousCount - downsizeCount, 0);

      oldInstanceData.add(ContainerServiceData.builder()
                              .name(serviceName)
                              .image(previousImage)
                              .previousCount(previousCount)
                              .desiredCount(desiredCount)
                              .build());
      downsizeCount -= previousCount - desiredCount;
    }
    return oldInstanceData;
  }

  public Map<String, Integer> getActiveServiceCounts(ContextData contextData) {
    EcsResizeParams resizeParams = contextData.getResizeParams();
    return awsClusterService.getActiveServiceCounts(resizeParams.getRegion(), contextData.getSettingAttribute(),
        contextData.getEncryptedDataDetails(), resizeParams.getClusterName(), resizeParams.getContainerServiceName());
  }

  public Map<String, String> getActiveServiceImages(ContextData contextData) {
    EcsResizeParams resizeParams = contextData.getResizeParams();
    String imagePrefix = substringBefore(resizeParams.getImage(), ":");
    return awsClusterService.getActiveServiceImages(resizeParams.getRegion(), contextData.getSettingAttribute(),
        contextData.getEncryptedDataDetails(), resizeParams.getClusterName(), resizeParams.getContainerServiceName(),
        imagePrefix);
  }

  public Map<String, Integer> listOfStringArrayToMap(List<String[]> listOfStringArray) {
    return Optional.ofNullable(listOfStringArray)
        .orElse(new ArrayList<>())
        .stream()
        .collect(Collectors.toMap(item -> item[0], item -> Integer.valueOf(item[1])));
  }

  public void setDesiredToOriginal(
      List<ContainerServiceData> newInstanceDataList, Map<String, Integer> originalServiceCounts) {
    for (ContainerServiceData containerServiceData : newInstanceDataList) {
      containerServiceData.setDesiredCount(
          Optional.ofNullable(originalServiceCounts.get(containerServiceData.getName())).orElse(0));
    }
  }

  public void logContainerInfos(List<ContainerInfo> containerInfos, ExecutionLogCallback executionLogCallback) {
    try {
      if (isNotEmpty(containerInfos)) {
        containerInfos.sort(Comparator.comparing(ContainerInfo::isNewContainer).reversed());
        executionLogCallback.saveExecutionLog("\nContainer IDs:");
        containerInfos.forEach(info
            -> executionLogCallback.saveExecutionLog("  " + info.getHostName()
                + (isNotEmpty(info.getHostName()) && info.getHostName().equals(info.getIp()) ? ""
                                                                                             : " - " + info.getIp())
                + (isNotEmpty(info.getHostName()) && info.getHostName().equals(info.getContainerId())
                          ? ""
                          : " - " + info.getContainerId())
                + (info.isNewContainer() ? " (new)" : "")));
        executionLogCallback.saveExecutionLog("");
      }
    } catch (Exception e) {
      Misc.logAllMessages(e, executionLogCallback);
    }
  }

  public EcsServiceDeployResponse getEmptyEcsServiceDeployResponse() {
    EcsServiceDeployResponse ecsServiceDeployResponse = EcsServiceDeployResponse.builder().build();
    ecsServiceDeployResponse.setCommandExecutionStatus(CommandExecutionStatus.SUCCESS);
    ecsServiceDeployResponse.setOutput(StringUtils.EMPTY);
    return ecsServiceDeployResponse;
  }
}
