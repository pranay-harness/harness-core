package software.wings.delegatetasks.spotinst.taskhandler;

import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static java.lang.String.format;
import static java.lang.String.valueOf;

import com.google.inject.Singleton;

import com.amazonaws.services.ec2.model.Instance;
import io.harness.delegate.task.spotinst.request.SpotInstGetElastigroupJsonParameters;
import io.harness.delegate.task.spotinst.request.SpotInstListElastigroupInstancesParameters;
import io.harness.delegate.task.spotinst.request.SpotInstTaskParameters;
import io.harness.delegate.task.spotinst.response.SpotInstGetElastigroupJsonResponse;
import io.harness.delegate.task.spotinst.response.SpotInstListElastigroupInstancesResponse;
import io.harness.delegate.task.spotinst.response.SpotInstListElastigroupNamesResponse;
import io.harness.delegate.task.spotinst.response.SpotInstTaskExecutionResponse;
import io.harness.delegate.task.spotinst.response.SpotInstTaskResponse;
import io.harness.exception.InvalidRequestException;
import io.harness.spotinst.model.ElastiGroup;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.AwsConfig;
import software.wings.beans.SpotInstConfig;

import java.util.List;

@Singleton
@NoArgsConstructor
@Slf4j
public class SpotInstSyncTaskHandler extends SpotInstTaskHandler {
  @Override
  protected SpotInstTaskExecutionResponse executeTaskInternal(SpotInstTaskParameters spotInstTaskParameters,
      SpotInstConfig spotInstConfig, AwsConfig awsConfig) throws Exception {
    if (!spotInstTaskParameters.isSyncTask()) {
      throw new InvalidRequestException(format("Unrecognized object of class: [%s] while executing sync task",
          spotInstTaskParameters.getCommandType().name()));
    }

    String spotInstAccountId = spotInstConfig.getSpotInstAccountId();
    String spotInstToken = valueOf(spotInstConfig.getSpotInstToken());
    SpotInstTaskResponse syncTaskResponse;
    switch (spotInstTaskParameters.getCommandType()) {
      case SPOT_INST_LIST_ELASTI_GROUPS: {
        List<ElastiGroup> groups = spotInstHelperServiceDelegate.listAllElstiGroups(spotInstToken, spotInstAccountId);
        syncTaskResponse = SpotInstListElastigroupNamesResponse.builder().elastigroups(groups).build();
        break;
      }
      case SPOT_INST_GET_ELASTI_GROUP_JSON: {
        String elastigroupId = ((SpotInstGetElastigroupJsonParameters) spotInstTaskParameters).getElastigroupId();
        String json = spotInstHelperServiceDelegate.getElastigroupJson(spotInstToken, spotInstAccountId, elastigroupId);
        syncTaskResponse = SpotInstGetElastigroupJsonResponse.builder().elastigroupJson(json).build();
        break;
      }
      case SPOT_INST_LIST_ELASTI_GROUP_INSTANCES: {
        SpotInstListElastigroupInstancesParameters listInstancesParams =
            (SpotInstListElastigroupInstancesParameters) spotInstTaskParameters;
        List<Instance> instances = getAllEc2InstancesOfElastiGroup(awsConfig, listInstancesParams.getAwsRegion(),
            spotInstToken, spotInstAccountId, listInstancesParams.getElastigroupId());
        syncTaskResponse = SpotInstListElastigroupInstancesResponse.builder().elastigroupInstances(instances).build();
        break;
      }
      default: {
        throw new InvalidRequestException(format("Unrecognized object of class: [%s] while executing sync task",
            spotInstTaskParameters.getCommandType().name()));
      }
    }
    return SpotInstTaskExecutionResponse.builder()
        .spotInstTaskResponse(syncTaskResponse)
        .commandExecutionStatus(SUCCESS)
        .build();
  }
}
