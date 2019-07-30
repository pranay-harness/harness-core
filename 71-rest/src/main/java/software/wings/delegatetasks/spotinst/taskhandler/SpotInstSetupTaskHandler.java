package software.wings.delegatetasks.spotinst.taskhandler;

import static com.google.api.client.util.Lists.newArrayList;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static io.harness.spotinst.model.SpotInstConstants.ELASTI_GROUP_NAME_PLACEHOLDER;
import static io.harness.spotinst.model.SpotInstConstants.TG_ARN_PLACEHOLDER;
import static io.harness.spotinst.model.SpotInstConstants.TG_NAME_PLACEHOLDER;
import static io.harness.spotinst.model.SpotInstConstants.elastiGroupsToKeep;
import static java.lang.String.format;
import static java.util.Collections.emptyList;

import com.google.inject.Singleton;

import com.amazonaws.services.elasticloadbalancingv2.model.Listener;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetGroup;
import io.harness.delegate.task.aws.AwsElbListener;
import io.harness.delegate.task.spotinst.request.SpotInstSetupTaskParameters;
import io.harness.delegate.task.spotinst.request.SpotInstTaskParameters;
import io.harness.delegate.task.spotinst.response.SpotInstSetupTaskResponse;
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
      ExecutionLogCallback logCallback, SpotInstConfig spotInstConfig, AwsConfig awsConfig) throws Exception {
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
    TargetGroup stageTargetGroup =
        getTargetGroup(awsConfig, spotInstTaskParameters.getAwsRegion(), setupTaskParameters.getLoadBalancerName(),
            setupTaskParameters.getTargetListenerPort(), logCallback, setupTaskParameters.getWorkflowExecutionId());
    String finalJson = setupTaskParameters.getElastiGroupJson()
                           .replace(ELASTI_GROUP_NAME_PLACEHOLDER, newElastiGroupName)
                           .replace(TG_NAME_PLACEHOLDER, stageTargetGroup.getTargetGroupName())
                           .replace(TG_ARN_PLACEHOLDER, stageTargetGroup.getTargetGroupArn());

    logCallback.saveExecutionLog(format("Sending request to create elasti group with name: [%s]", newElastiGroupName));
    ElastiGroup elastiGroup =
        spotInstHelperServiceDelegate.createElastiGroup(spotInstToken, spotInstAccountId, finalJson);
    String newElastiGroupId = elastiGroup.getId();
    logCallback.saveExecutionLog(format("Create elasti group with id: [%s]", newElastiGroupId));

    List<ElastiGroup> groupsWithInstances = newArrayList();
    List<ElastiGroup> groupsWithoutInstances = newArrayList();
    if (isNotEmpty(elastiGroups)) {
      elastiGroups.forEach(group -> {
        if (group.getElastiGroupCapacity().getTarget() > 0) {
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
                                  .groupsToBeDownsized(groupsWithInstances)
                                  .build())
        .build();
  }

  private TargetGroup getTargetGroup(AwsConfig awsConfig, String region, String loadBalancerName, int stageListenerPort,
      ExecutionLogCallback logCallback, String workflowExecutionId) {
    // Aws Config is already decrypted
    List<AwsElbListener> listeners =
        awsElbHelperServiceDelegate.getElbListenersForLoadBalaner(awsConfig, emptyList(), region, loadBalancerName);
    Optional<AwsElbListener> optionalListener =
        listeners.stream().filter(listener -> stageListenerPort == listener.getPort()).findFirst();
    if (!optionalListener.isPresent()) {
      String message =
          format("Did not find any listener on port: [%d] for load balancer: [%s]. Workflow execution: [%s]",
              stageListenerPort, loadBalancerName, workflowExecutionId);
      logger.error(message);
      logCallback.saveExecutionLog(message);
      throw new WingsException(message, EnumSet.of(ReportTarget.UNIVERSAL));
    }
    AwsElbListener awsElbListener = optionalListener.get();
    Listener listener =
        awsElbHelperServiceDelegate.getElbListener(awsConfig, emptyList(), region, awsElbListener.getListenerArn());
    String targetGroupArn = awsElbHelperServiceDelegate.getTargetGroupForDefaultAction(listener, logCallback);
    Optional<TargetGroup> targetGroup =
        awsElbHelperServiceDelegate.getTargetGroup(awsConfig, emptyList(), region, targetGroupArn);
    if (!targetGroup.isPresent()) {
      String message = format(
          "Did not find any target group with arn: [%s].Workflow execution: [%s]", targetGroupArn, workflowExecutionId);
      logger.error(message);
      logCallback.saveExecutionLog(message);
      throw new WingsException(message, EnumSet.of(ReportTarget.UNIVERSAL));
    }
    return targetGroup.get();
  }
}