package software.wings.delegatetasks.spotinst.taskhandler;

import static com.google.api.client.util.Lists.newArrayList;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static io.harness.spotinst.model.SpotInstConstants.ELASTI_GROUP_NAME_PLACEHOLDER;
import static io.harness.spotinst.model.SpotInstConstants.PROD_ELASTI_GROUP_NAME_SUFFIX;
import static io.harness.spotinst.model.SpotInstConstants.SETUP_COMMAND_UNIT;
import static io.harness.spotinst.model.SpotInstConstants.STAGE_ELASTI_GROUP_NAME_SUFFIX;
import static io.harness.spotinst.model.SpotInstConstants.TG_ARN_PLACEHOLDER;
import static io.harness.spotinst.model.SpotInstConstants.TG_NAME_PLACEHOLDER;
import static io.harness.spotinst.model.SpotInstConstants.elastiGroupsToKeep;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static software.wings.beans.Log.LogLevel.INFO;

import com.google.inject.Singleton;

import com.amazonaws.services.elasticloadbalancingv2.model.Listener;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetGroup;
import io.harness.delegate.task.aws.AwsElbListener;
import io.harness.delegate.task.spotinst.request.SpotInstSetupTaskParameters;
import io.harness.delegate.task.spotinst.request.SpotInstTaskParameters;
import io.harness.delegate.task.spotinst.response.SpotInstSetupTaskResponse;
import io.harness.delegate.task.spotinst.response.SpotInstSetupTaskResponse.SpotInstSetupTaskResponseBuilder;
import io.harness.delegate.task.spotinst.response.SpotInstTaskExecutionResponse;
import io.harness.exception.WingsException;
import io.harness.exception.WingsException.ReportTarget;
import io.harness.spotinst.model.ElastiGroup;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.AwsConfig;
import software.wings.beans.SpotInstConfig;
import software.wings.beans.command.ExecutionLogCallback;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

@Slf4j
@Singleton
@NoArgsConstructor
public class SpotInstSetupTaskHandler extends SpotInstTaskHandler {
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
    SpotInstSetupTaskResponseBuilder builder = SpotInstSetupTaskResponse.builder();
    String prefix = format("%s__", setupTaskParameters.getElastiGroupNamePrefix());
    int elastiGroupVersion = 1;
    logCallback.saveExecutionLog(format("Querying spot inst for existing elasti groups with prefix: [%s]", prefix));
    List<ElastiGroup> elastiGroups = spotInstHelperServiceDelegate.listAllElastiGroups(
        spotInstToken, spotInstAccountId, setupTaskParameters.getElastiGroupNamePrefix());
    if (isNotEmpty(elastiGroups)) {
      elastiGroupVersion =
          Integer.parseInt(elastiGroups.get(elastiGroups.size() - 1).getName().substring(prefix.length())) + 1;
    }
    String newElastiGroupName = format("%s%d", prefix, elastiGroupVersion);

    // Target Group associated with stageListener
    TargetGroup stageTargetGroup = getTargetGroup(awsConfig, setupTaskParameters.getAwsRegion(),
        setupTaskParameters.getLoadBalancerName(), setupTaskParameters.getStageListenerPort(),
        setupTaskParameters.getProdListenerPort(), logCallback, setupTaskParameters.getWorkflowExecutionId(), builder);

    String finalJson = setupTaskParameters.getElastiGroupJson()
                           .replace(ELASTI_GROUP_NAME_PLACEHOLDER, newElastiGroupName)
                           .replace(TG_NAME_PLACEHOLDER, stageTargetGroup.getTargetGroupName())
                           .replace(TG_ARN_PLACEHOLDER, stageTargetGroup.getTargetGroupArn());

    logCallback.saveExecutionLog(format("Sending request to create elasti group with name: [%s]", newElastiGroupName));
    ElastiGroup elastiGroup =
        spotInstHelperServiceDelegate.createElastiGroup(spotInstToken, spotInstAccountId, finalJson);
    String newElastiGroupId = elastiGroup.getId();
    logCallback.saveExecutionLog(format("Created elasti group with id: [%s]", newElastiGroupId));

    List<ElastiGroup> groupsWithInstances = newArrayList();
    List<ElastiGroup> groupsWithoutInstances = newArrayList();
    if (isNotEmpty(elastiGroups)) {
      elastiGroups.forEach(group -> {
        if (group.getCapacity().getTarget() > 0) {
          groupsWithInstances.add(group);
        } else {
          groupsWithoutInstances.add(group);
        }
      });
    }

    int lastIdx = groupsWithoutInstances.size() - elastiGroupsToKeep;
    for (int i = 0; i < lastIdx; i++) {
      String nameToDelete = groupsWithoutInstances.get(i).getName();
      String idToDelete = groupsWithoutInstances.get(i).getId();
      logCallback.saveExecutionLog(
          format("Sending request to delete elasti group: [%s] with id: [%s]", nameToDelete, idToDelete));
      spotInstHelperServiceDelegate.deleteElastiGroup(spotInstToken, spotInstAccountId, idToDelete);
    }

    return SpotInstTaskExecutionResponse.builder()
        .commandExecutionStatus(SUCCESS)
        .spotInstTaskResponse(SpotInstSetupTaskResponse.builder()
                                  .newElastiGroup(elastiGroup)
                                  .groupToBeDownsized(groupsWithInstances)
                                  .build())
        .build();
  }

  private SpotInstTaskExecutionResponse executeTaskInternalForBlueGreen(SpotInstSetupTaskParameters setupTaskParameters,
      String spotInstAccountId, String spotInstToken, AwsConfig awsConfig, ExecutionLogCallback logCallback)
      throws Exception {
    SpotInstSetupTaskResponseBuilder builder = SpotInstSetupTaskResponse.builder();
    logCallback.saveExecutionLog(format("Querying aws to get the stage target group details for load balancer: [%s]",
        setupTaskParameters.getLoadBalancerName()));

    // Target Group associated with StageListener
    TargetGroup stageTargetGroup = getTargetGroup(awsConfig, setupTaskParameters.getAwsRegion(),
        setupTaskParameters.getLoadBalancerName(), setupTaskParameters.getStageListenerPort(),
        setupTaskParameters.getProdListenerPort(), logCallback, setupTaskParameters.getWorkflowExecutionId(), builder);

    // Target Group associated with ProdListener
    TargetGroup prodTargetGroup = getTargetGroupUsingListenerArn(awsConfig, setupTaskParameters.getAwsRegion(),
        builder.build().getProdListenerArn(), logCallback, setupTaskParameters.getWorkflowExecutionId());

    builder.prodTargetGroupArn(prodTargetGroup.getTargetGroupArn());
    builder.stageTargetGroupArn(stageTargetGroup.getTargetGroupArn());
    logCallback.saveExecutionLog(format("Found stage target group: [%s] with Arn: [%s]",
        stageTargetGroup.getTargetGroupName(), stageTargetGroup.getTargetGroupArn()));

    // Stage Elasti Groups
    String stageElastiGroupName =
        format("%s__%s", setupTaskParameters.getElastiGroupNamePrefix(), STAGE_ELASTI_GROUP_NAME_SUFFIX);
    String finalJson = setupTaskParameters.getElastiGroupJson()
                           .replace(ELASTI_GROUP_NAME_PLACEHOLDER, stageElastiGroupName)
                           .replace(TG_NAME_PLACEHOLDER, stageTargetGroup.getTargetGroupName())
                           .replace(TG_ARN_PLACEHOLDER, stageTargetGroup.getTargetGroupArn());
    logCallback.saveExecutionLog(format("Querying to find elasti group with name: [%s]", stageElastiGroupName));
    Optional<ElastiGroup> stageOptionalElastiGroup =
        spotInstHelperServiceDelegate.getElastiGroupByName(spotInstToken, spotInstAccountId, stageElastiGroupName);
    ElastiGroup stageElastiGroup;
    if (stageOptionalElastiGroup.isPresent()) {
      stageElastiGroup = stageOptionalElastiGroup.get();
      logCallback.saveExecutionLog(
          format("Found stage elasti group with id: [%s]. Deleting it. ", stageElastiGroup.getId()));
      spotInstHelperServiceDelegate.deleteElastiGroup(spotInstToken, spotInstAccountId, stageElastiGroup.getId());
    }
    logCallback.saveExecutionLog(
        format("Sending request to create new Elasti Group with name: [%s]", stageElastiGroupName));
    stageElastiGroup = spotInstHelperServiceDelegate.createElastiGroup(spotInstToken, spotInstAccountId, finalJson);
    String stageElastiGroupId = stageElastiGroup.getId();
    logCallback.saveExecutionLog(
        format("Created elasti group with name: [%s] and id: [%s]", stageElastiGroupName, stageElastiGroupId));
    builder.newElastiGroup(stageElastiGroup);

    // Prod ELasti Groups
    String prodElastiGroupName =
        format("%s__%s", setupTaskParameters.getElastiGroupNamePrefix(), PROD_ELASTI_GROUP_NAME_SUFFIX);
    logCallback.saveExecutionLog(format("Querying spot inst for elasti group with name: [%s]", prodElastiGroupName));
    Optional<ElastiGroup> prodOptionalElastiGroup =
        spotInstHelperServiceDelegate.getElastiGroupByName(spotInstToken, spotInstAccountId, prodElastiGroupName);
    List<ElastiGroup> prodElastiGroupList;
    if (prodOptionalElastiGroup.isPresent()) {
      ElastiGroup prodElastiGroup = prodOptionalElastiGroup.get();
      logCallback.saveExecutionLog(format("Found existing Prod Elasti group with name: [%s] and id: [%s]",
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

  private TargetGroup getTargetGroup(AwsConfig awsConfig, String region, String loadBalancerName, int stageListenerPort,
      int prodListenerPort, ExecutionLogCallback logCallback, String workflowExecutionId,
      SpotInstSetupTaskResponseBuilder builder) throws Exception {
    List<AwsElbListener> listeners =
        awsElbHelperServiceDelegate.getElbListenersForLoadBalaner(awsConfig, emptyList(), region, loadBalancerName);
    AwsElbListener prodListener =
        getListenerOnPort(listeners, prodListenerPort, loadBalancerName, workflowExecutionId, logCallback);
    builder.prodListenerArn(prodListener.getListenerArn());
    AwsElbListener stageListener =
        getListenerOnPort(listeners, stageListenerPort, loadBalancerName, workflowExecutionId, logCallback);
    builder.stageListenerArn(stageListener.getListenerArn());
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

  private TargetGroup getTargetGroupUsingListenerArn(AwsConfig awsConfig, String region, String listenerArn,
      ExecutionLogCallback logCallback, String workflowExecutionId) throws Exception {
    Listener listener = awsElbHelperServiceDelegate.getElbListener(awsConfig, emptyList(), region, listenerArn);
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