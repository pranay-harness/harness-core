package software.wings.delegatetasks.spotinst.taskhandler;

import static com.google.api.client.util.Lists.newArrayList;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static io.harness.spotinst.model.SpotInstConstants.CAPACITY;
import static io.harness.spotinst.model.SpotInstConstants.CAPACITY_MAXIMUM_CONFIG_ELEMENT;
import static io.harness.spotinst.model.SpotInstConstants.CAPACITY_MINIMUM_CONFIG_ELEMENT;
import static io.harness.spotinst.model.SpotInstConstants.CAPACITY_TARGET_CONFIG_ELEMENT;
import static io.harness.spotinst.model.SpotInstConstants.CAPACITY_UNIT_CONFIG_ELEMENT;
import static io.harness.spotinst.model.SpotInstConstants.COMPUTE;
import static io.harness.spotinst.model.SpotInstConstants.ELASTI_GROUP_CREATED_AT;
import static io.harness.spotinst.model.SpotInstConstants.ELASTI_GROUP_ID;
import static io.harness.spotinst.model.SpotInstConstants.ELASTI_GROUP_IMAGE_CONFIG;
import static io.harness.spotinst.model.SpotInstConstants.ELASTI_GROUP_UPDATED_AT;
import static io.harness.spotinst.model.SpotInstConstants.ELASTI_GROUP_USER_DATA_CONFIG;
import static io.harness.spotinst.model.SpotInstConstants.GROUP_CONFIG_ELEMENT;
import static io.harness.spotinst.model.SpotInstConstants.LAUNCH_SPECIFICATION;
import static io.harness.spotinst.model.SpotInstConstants.LB_TYPE_TG;
import static io.harness.spotinst.model.SpotInstConstants.LOAD_BALANCERS_CONFIG;
import static io.harness.spotinst.model.SpotInstConstants.NAME_CONFIG_ELEMENT;
import static io.harness.spotinst.model.SpotInstConstants.SETUP_COMMAND_UNIT;
import static io.harness.spotinst.model.SpotInstConstants.STAGE_ELASTI_GROUP_NAME_SUFFIX;
import static io.harness.spotinst.model.SpotInstConstants.UNIT_INSTANCE;
import static io.harness.spotinst.model.SpotInstConstants.elastiGroupsToKeep;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static software.wings.beans.Log.LogLevel.INFO;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Singleton;

import com.amazonaws.services.elasticloadbalancingv2.model.Listener;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetGroup;
import io.harness.delegate.task.aws.AwsElbListener;
import io.harness.delegate.task.aws.LoadBalancerDetailsForBGDeployment;
import io.harness.delegate.task.spotinst.request.SpotInstSetupTaskParameters;
import io.harness.delegate.task.spotinst.request.SpotInstTaskParameters;
import io.harness.delegate.task.spotinst.response.SpotInstSetupTaskResponse;
import io.harness.delegate.task.spotinst.response.SpotInstSetupTaskResponse.SpotInstSetupTaskResponseBuilder;
import io.harness.delegate.task.spotinst.response.SpotInstTaskExecutionResponse;
import io.harness.exception.WingsException;
import io.harness.exception.WingsException.ReportTarget;
import io.harness.spotinst.model.ElastiGroup;
import io.harness.spotinst.model.ElastiGroupCapacity;
import io.harness.spotinst.model.ElastiGroupLoadBalancer;
import io.harness.spotinst.model.ElastiGroupLoadBalancerConfig;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.AwsConfig;
import software.wings.beans.SpotInstConfig;
import software.wings.beans.command.ExecutionLogCallback;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Singleton
@NoArgsConstructor
public class SpotInstSetupTaskHandler extends SpotInstTaskHandler {
  @Override
  protected SpotInstTaskExecutionResponse executeTaskInternal(SpotInstTaskParameters spotInstTaskParameters,
      SpotInstConfig spotInstConfig, AwsConfig awsConfig) throws Exception {
    if (!(spotInstTaskParameters instanceof SpotInstSetupTaskParameters)) {
      String message =
          format("Parameters of unrecognized class: [%s] found while executing setup step. Workflow execution: [%s]",
              spotInstTaskParameters.getClass().getSimpleName(), spotInstTaskParameters.getWorkflowExecutionId());
      logger.error(message);
      return SpotInstTaskExecutionResponse.builder().commandExecutionStatus(FAILURE).errorMessage(message).build();
    }

    String spotInstAccountId = spotInstConfig.getSpotInstAccountId();
    String spotInstToken = String.valueOf(spotInstConfig.getSpotInstToken());
    SpotInstSetupTaskParameters setupTaskParameters = (SpotInstSetupTaskParameters) spotInstTaskParameters;
    ExecutionLogCallback logCallback = getLogCallBack(spotInstTaskParameters, SETUP_COMMAND_UNIT);

    if (setupTaskParameters.isBlueGreen()) {
      // Handle Blue Green
      return executeTaskInternalForBlueGreen(
          setupTaskParameters, spotInstAccountId, spotInstToken, awsConfig, logCallback);
    }

    // Handle canary and basic
    String prefix = format("%s__", setupTaskParameters.getElastiGroupNamePrefix());
    int elastiGroupVersion = 1;
    logCallback.saveExecutionLog(format("Querying Spotinst for existing Elastigroups with prefix: [%s]", prefix));
    List<ElastiGroup> elastiGroups = spotInstHelperServiceDelegate.listAllElastiGroups(
        spotInstToken, spotInstAccountId, setupTaskParameters.getElastiGroupNamePrefix());
    if (isNotEmpty(elastiGroups)) {
      elastiGroupVersion =
          Integer.parseInt(elastiGroups.get(elastiGroups.size() - 1).getName().substring(prefix.length())) + 1;
    }
    String newElastiGroupName = format("%s%d", prefix, elastiGroupVersion);

    String finalJson = generateFinalJson(setupTaskParameters, newElastiGroupName);

    logCallback.saveExecutionLog(format("Sending request to create Elastigroup with name: [%s]", newElastiGroupName));
    ElastiGroup elastiGroup =
        spotInstHelperServiceDelegate.createElastiGroup(spotInstToken, spotInstAccountId, finalJson);
    String newElastiGroupId = elastiGroup.getId();
    logCallback.saveExecutionLog(format("Created Elastigroup with id: [%s]", newElastiGroupId));

    /**
     * Look at all the Elastigroups except the "LAST" elastigroup.
     * If they have running instances, we will downscale them to 0.
     */
    List<ElastiGroup> groupsWithoutInstances = newArrayList();
    List<ElastiGroup> groupToDownsizeDuringDeploy = emptyList();
    if (isNotEmpty(elastiGroups)) {
      groupToDownsizeDuringDeploy = singletonList(elastiGroups.get(elastiGroups.size() - 1));
      for (int i = 0; i < elastiGroups.size() - 1; i++) {
        ElastiGroup elastigroupCurrent = elastiGroups.get(i);
        ElastiGroupCapacity capacity = elastigroupCurrent.getCapacity();
        if (capacity == null) {
          groupsWithoutInstances.add(elastigroupCurrent);
          continue;
        }
        int target = capacity.getTarget();
        if (target == 0) {
          groupsWithoutInstances.add(elastigroupCurrent);
        } else {
          logCallback.saveExecutionLog(
              format("Downscaling old Elastigroup with id: [%s] to 0 instances.", elastigroupCurrent.getId()));
          ElastiGroup temp = ElastiGroup.builder()
                                 .id(elastigroupCurrent.getId())
                                 .name(elastigroupCurrent.getName())
                                 .capacity(ElastiGroupCapacity.builder().minimum(0).maximum(0).target(0).build())
                                 .build();
          spotInstHelperServiceDelegate.updateElastiGroupCapacity(
              spotInstToken, spotInstAccountId, elastigroupCurrent.getId(), temp);
        }
      }
    }

    int lastIdx = groupsWithoutInstances.size() - elastiGroupsToKeep;
    for (int i = 0; i < lastIdx; i++) {
      String nameToDelete = groupsWithoutInstances.get(i).getName();
      String idToDelete = groupsWithoutInstances.get(i).getId();
      logCallback.saveExecutionLog(
          format("Sending request to delete Elastigroup: [%s] with id: [%s]", nameToDelete, idToDelete));
      spotInstHelperServiceDelegate.deleteElastiGroup(spotInstToken, spotInstAccountId, idToDelete);
    }

    logCallback.saveExecutionLog("Completed setup for Spotinst", INFO, SUCCESS);
    return SpotInstTaskExecutionResponse.builder()
        .commandExecutionStatus(SUCCESS)
        .spotInstTaskResponse(SpotInstSetupTaskResponse.builder()
                                  .newElastiGroup(elastiGroup)
                                  .groupToBeDownsized(groupToDownsizeDuringDeploy)
                                  .build())
        .build();
  }

  @VisibleForTesting
  SpotInstTaskExecutionResponse executeTaskInternalForBlueGreen(SpotInstSetupTaskParameters setupTaskParameters,
      String spotInstAccountId, String spotInstToken, AwsConfig awsConfig, ExecutionLogCallback logCallback)
      throws Exception {
    SpotInstSetupTaskResponseBuilder builder = SpotInstSetupTaskResponse.builder();
    List<LoadBalancerDetailsForBGDeployment> lbDetailList =
        fetchAllLoadBalancerDetails(setupTaskParameters, awsConfig, logCallback);
    builder.lbDetailsForBGDeployments(lbDetailList);
    // Update lbDetails with fetched details, as they have more data field in
    setupTaskParameters.setAwsLoadBalancerConfigs(lbDetailList);

    // Generate STAGE elastiGroup name
    String stageElastiGroupName =
        format("%s__%s", setupTaskParameters.getElastiGroupNamePrefix(), STAGE_ELASTI_GROUP_NAME_SUFFIX);

    // Generate final json by substituting name, capacity and LBConfig
    String finalJson = generateFinalJson(setupTaskParameters, stageElastiGroupName);

    // Check if existing elastigroup with exists with same stage name
    logCallback.saveExecutionLog(format("Querying to find Elastigroup with name: [%s]", stageElastiGroupName));
    Optional<ElastiGroup> stageOptionalElastiGroup =
        spotInstHelperServiceDelegate.getElastiGroupByName(spotInstToken, spotInstAccountId, stageElastiGroupName);
    ElastiGroup stageElastiGroup;
    if (stageOptionalElastiGroup.isPresent()) {
      stageElastiGroup = stageOptionalElastiGroup.get();
      logCallback.saveExecutionLog(
          format("Found stage Elastigroup with id: [%s]. Deleting it. ", stageElastiGroup.getId()));
      spotInstHelperServiceDelegate.deleteElastiGroup(spotInstToken, spotInstAccountId, stageElastiGroup.getId());
    }

    // Create new elastiGroup
    logCallback.saveExecutionLog(
        format("Sending request to create new Elastigroup with name: [%s]", stageElastiGroupName));
    stageElastiGroup = spotInstHelperServiceDelegate.createElastiGroup(spotInstToken, spotInstAccountId, finalJson);
    String stageElastiGroupId = stageElastiGroup.getId();
    logCallback.saveExecutionLog(
        format("Created Elastigroup with name: [%s] and id: [%s]", stageElastiGroupName, stageElastiGroupId));
    builder.newElastiGroup(stageElastiGroup);

    // Prod ELasti Groups
    String prodElastiGroupName = setupTaskParameters.getElastiGroupNamePrefix();
    logCallback.saveExecutionLog(format("Querying Spotinst for Elastigroup with name: [%s]", prodElastiGroupName));
    Optional<ElastiGroup> prodOptionalElastiGroup =
        spotInstHelperServiceDelegate.getElastiGroupByName(spotInstToken, spotInstAccountId, prodElastiGroupName);
    List<ElastiGroup> prodElastiGroupList;
    if (prodOptionalElastiGroup.isPresent()) {
      ElastiGroup prodElastiGroup = prodOptionalElastiGroup.get();
      logCallback.saveExecutionLog(format("Found existing Prod Elastigroup with name: [%s] and id: [%s]",
          prodElastiGroup.getName(), prodElastiGroup.getId()));
      prodElastiGroupList = singletonList(prodElastiGroup);
    } else {
      prodElastiGroupList = emptyList();
    }
    builder.groupToBeDownsized(prodElastiGroupList);
    logCallback.saveExecutionLog("Completed Blue green setup for Spotinst", INFO, SUCCESS);
    return SpotInstTaskExecutionResponse.builder()
        .commandExecutionStatus(SUCCESS)
        .spotInstTaskResponse(builder.build())
        .build();
  }

  @VisibleForTesting
  String generateFinalJson(SpotInstSetupTaskParameters setupTaskParameters, String newElastiGroupName) {
    String elastiGroupJson = setupTaskParameters.getElastiGroupJson();
    java.lang.reflect.Type mapType = new TypeToken<Map<String, Object>>() {}.getType();
    Gson gson = new Gson();

    // Map<"group": {...entire config...}>, this is elastiGroupConfig json that spotinst exposes
    Map<String, Object> jsonConfigMap = gson.fromJson(elastiGroupJson, mapType);

    Map<String, Object> elastiGroupConfigMap = (Map<String, Object>) jsonConfigMap.get(GROUP_CONFIG_ELEMENT);

    removeUnsupportedFieldsForCreatingNewGroup(elastiGroupConfigMap);
    updateName(elastiGroupConfigMap, newElastiGroupName);
    updateInitialCapacity(elastiGroupConfigMap);
    updateWithLoadBalancerAndImageConfig(setupTaskParameters.getAwsLoadBalancerConfigs(), elastiGroupConfigMap,
        setupTaskParameters.getImage(), setupTaskParameters.getUserData(), setupTaskParameters.isBlueGreen());
    return gson.toJson(jsonConfigMap);
  }

  @VisibleForTesting
  void removeUnsupportedFieldsForCreatingNewGroup(Map<String, Object> elastiGroupConfigMap) {
    if (elastiGroupConfigMap.containsKey(ELASTI_GROUP_ID)) {
      elastiGroupConfigMap.remove(ELASTI_GROUP_ID);
    }

    if (elastiGroupConfigMap.containsKey(ELASTI_GROUP_CREATED_AT)) {
      elastiGroupConfigMap.remove(ELASTI_GROUP_CREATED_AT);
    }

    if (elastiGroupConfigMap.containsKey(ELASTI_GROUP_UPDATED_AT)) {
      elastiGroupConfigMap.remove(ELASTI_GROUP_UPDATED_AT);
    }
  }

  private void updateName(Map<String, Object> elastiGroupConfigMap, String stageElastiGroupName) {
    elastiGroupConfigMap.put(NAME_CONFIG_ELEMENT, stageElastiGroupName);
  }

  private void updateInitialCapacity(Map<String, Object> elastiGroupConfigMap) {
    Map<String, Object> capacityConfig = (Map<String, Object>) elastiGroupConfigMap.get(CAPACITY);

    capacityConfig.put(CAPACITY_MINIMUM_CONFIG_ELEMENT, 0);
    capacityConfig.put(CAPACITY_TARGET_CONFIG_ELEMENT, 0);
    capacityConfig.put(CAPACITY_MAXIMUM_CONFIG_ELEMENT, 0);

    if (!capacityConfig.containsKey(CAPACITY_UNIT_CONFIG_ELEMENT)) {
      capacityConfig.put(CAPACITY_UNIT_CONFIG_ELEMENT, UNIT_INSTANCE);
    }
  }

  private void updateWithLoadBalancerAndImageConfig(List<LoadBalancerDetailsForBGDeployment> lbDetailList,
      Map<String, Object> elastiGroupConfigMap, String image, String userData, boolean blueGreen) {
    Map<String, Object> computeConfigMap = (Map<String, Object>) elastiGroupConfigMap.get(COMPUTE);
    Map<String, Object> launchSpecificationMap = (Map<String, Object>) computeConfigMap.get(LAUNCH_SPECIFICATION);

    if (blueGreen) {
      launchSpecificationMap.put(LOAD_BALANCERS_CONFIG,
          ElastiGroupLoadBalancerConfig.builder().loadBalancers(generateLBConfigs(lbDetailList)).build());
    }
    launchSpecificationMap.put(ELASTI_GROUP_IMAGE_CONFIG, image);
    if (isNotEmpty(userData)) {
      launchSpecificationMap.put(ELASTI_GROUP_USER_DATA_CONFIG, userData);
    }
  }

  private List<ElastiGroupLoadBalancer> generateLBConfigs(List<LoadBalancerDetailsForBGDeployment> lbDetailList) {
    List<ElastiGroupLoadBalancer> elastiGroupLoadBalancers = new ArrayList<>();
    lbDetailList.forEach(loadBalancerdetail
        -> elastiGroupLoadBalancers.add(ElastiGroupLoadBalancer.builder()
                                            .arn(loadBalancerdetail.getStageTargetGroupArn())
                                            .name(loadBalancerdetail.getStageTargetGroupName())
                                            .type(LB_TYPE_TG)
                                            .build()));
    return elastiGroupLoadBalancers;
  }

  private List<LoadBalancerDetailsForBGDeployment> fetchAllLoadBalancerDetails(
      SpotInstSetupTaskParameters setupTaskParameters, AwsConfig awsConfig, ExecutionLogCallback logCallback) {
    List<LoadBalancerDetailsForBGDeployment> awsLoadBalancerConfigs = setupTaskParameters.getAwsLoadBalancerConfigs();
    List<LoadBalancerDetailsForBGDeployment> lbDetailsWithArnValues = new ArrayList<>();
    try {
      for (LoadBalancerDetailsForBGDeployment awsLoadBalancerConfig : awsLoadBalancerConfigs) {
        logCallback.saveExecutionLog(
            format("Querying aws to get the stage target group details for load balancer: [%s]",
                awsLoadBalancerConfig.getLoadBalancerName()));

        // Target Group associated with StageListener
        int stageListenerPort = Integer.parseInt(awsLoadBalancerConfig.getStageListenerPort());
        int prodListenerPort = Integer.parseInt(awsLoadBalancerConfig.getProdListenerPort());

        LoadBalancerDetailsForBGDeployment loadBalancerDetailsForBGDeployment = getListenerResponseDetails(awsConfig,
            setupTaskParameters.getAwsRegion(), awsLoadBalancerConfig.getLoadBalancerName(), stageListenerPort,
            prodListenerPort, logCallback, setupTaskParameters.getWorkflowExecutionId());

        lbDetailsWithArnValues.add(loadBalancerDetailsForBGDeployment);

        logCallback.saveExecutionLog(format("Using TargetGroup: [%s], ARN: [%s] with new Elastigroup",
            loadBalancerDetailsForBGDeployment.getStageTargetGroupName(),
            loadBalancerDetailsForBGDeployment.getStageTargetGroupArn()));
      }
    } catch (Exception e) {
      throw new WingsException("Failed while fetching TargetGroup Details", e);
    }

    return lbDetailsWithArnValues;
  }

  private AwsElbListener getListenerOnPort(List<AwsElbListener> listeners, int port, String loadBalancerName,
      String workflowExecutionId, ExecutionLogCallback logCallback) {
    if (isEmpty(listeners)) {
      String message = format("Did not find any listeners for load balancer: [%s]. Workflow execution: [%s]",
          loadBalancerName, workflowExecutionId);
      logger.error(message);
      logCallback.saveExecutionLog(message);
      throw new WingsException(message, EnumSet.of(ReportTarget.UNIVERSAL));
    }
    Optional<AwsElbListener> optionalListener =
        listeners.stream().filter(listener -> port == listener.getPort()).findFirst();
    if (!optionalListener.isPresent()) {
      String message =
          format("Did not find any listeners on port: [%d] for load balancer: [%s]. Workflow execution: [%s]", port,
              loadBalancerName, workflowExecutionId);
      logger.error(message);
      logCallback.saveExecutionLog(message);
      throw new WingsException(message, EnumSet.of(ReportTarget.UNIVERSAL));
    }
    return optionalListener.get();
  }

  private LoadBalancerDetailsForBGDeployment getListenerResponseDetails(AwsConfig awsConfig, String region,
      String loadBalancerName, int stageListenerPort, int prodListenerPort, ExecutionLogCallback logCallback,
      String workflowExecutionId) throws Exception {
    List<AwsElbListener> listeners =
        awsElbHelperServiceDelegate.getElbListenersForLoadBalaner(awsConfig, emptyList(), region, loadBalancerName);

    AwsElbListener prodListener =
        getListenerOnPort(listeners, prodListenerPort, loadBalancerName, workflowExecutionId, logCallback);
    TargetGroup prodTargetGroup =
        fetchTargetGroupForListener(awsConfig, region, logCallback, workflowExecutionId, prodListener);

    AwsElbListener stageListener =
        getListenerOnPort(listeners, stageListenerPort, loadBalancerName, workflowExecutionId, logCallback);
    TargetGroup stageTargetGroup =
        fetchTargetGroupForListener(awsConfig, region, logCallback, workflowExecutionId, stageListener);

    return LoadBalancerDetailsForBGDeployment.builder()
        .loadBalancerArn(prodListener.getLoadBalancerArn())
        .loadBalancerName(loadBalancerName)
        .prodListenerArn(prodListener.getListenerArn())
        .prodTargetGroupArn(prodTargetGroup.getTargetGroupArn())
        .prodTargetGroupName(prodTargetGroup.getTargetGroupName())
        .stageListenerArn(stageListener.getListenerArn())
        .stageTargetGroupArn(stageTargetGroup.getTargetGroupArn())
        .stageTargetGroupName(stageTargetGroup.getTargetGroupName())
        .prodListenerPort(Integer.toString(prodListenerPort))
        .stageListenerPort(Integer.toString(stageListenerPort))
        .build();
  }

  private TargetGroup fetchTargetGroupForListener(AwsConfig awsConfig, String region, ExecutionLogCallback logCallback,
      String workflowExecutionId, AwsElbListener stageListener) {
    Listener listener =
        awsElbHelperServiceDelegate.getElbListener(awsConfig, emptyList(), region, stageListener.getListenerArn());
    String targetGroupArn = awsElbHelperServiceDelegate.getTargetGroupForDefaultAction(listener, logCallback);
    Optional<TargetGroup> targetGroup =
        awsElbHelperServiceDelegate.getTargetGroup(awsConfig, emptyList(), region, targetGroupArn);
    if (!targetGroup.isPresent()) {
      String message = format("Did not find any target group with arn: [%s]. Workflow execution: [%s]", targetGroupArn,
          workflowExecutionId);
      logger.error(message);
      logCallback.saveExecutionLog(message);
      throw new WingsException(message, EnumSet.of(ReportTarget.UNIVERSAL));
    }

    return targetGroup.get();
  }
}