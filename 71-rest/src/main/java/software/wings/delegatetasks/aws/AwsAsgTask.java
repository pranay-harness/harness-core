package software.wings.delegatetasks.aws;

import static io.harness.beans.ExecutionStatus.SUCCESS;

import com.google.inject.Inject;

import com.amazonaws.services.ec2.model.Instance;
import io.harness.beans.DelegateTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import software.wings.beans.DelegateTaskResponse;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;
import software.wings.service.impl.aws.model.AwsAsgListAllNamesResponse;
import software.wings.service.impl.aws.model.AwsAsgListDesiredCapacitiesRequest;
import software.wings.service.impl.aws.model.AwsAsgListDesiredCapacitiesResponse;
import software.wings.service.impl.aws.model.AwsAsgListInstancesRequest;
import software.wings.service.impl.aws.model.AwsAsgListInstancesResponse;
import software.wings.service.impl.aws.model.AwsAsgRequest;
import software.wings.service.impl.aws.model.AwsAsgRequest.AwsAsgRequestType;
import software.wings.service.impl.aws.model.AwsResponse;
import software.wings.service.intfc.aws.delegate.AwsAsgHelperServiceDelegate;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Slf4j
public class AwsAsgTask extends AbstractDelegateRunnableTask {
  @Inject private AwsAsgHelperServiceDelegate awsAsgHelperServiceDelegate;

  public AwsAsgTask(String delegateId, DelegateTask delegateTask, Consumer<DelegateTaskResponse> consumer,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  public AwsResponse run(Object[] parameters) {
    throw new NotImplementedException("not implemented");
  }

  @Override
  public AwsResponse run(TaskParameters parameters) {
    AwsAsgRequest request = (AwsAsgRequest) parameters;
    try {
      AwsAsgRequestType requestType = request.getRequestType();
      switch (requestType) {
        case LIST_ALL_ASG_NAMES: {
          List<String> aSgNames = awsAsgHelperServiceDelegate.listAutoScalingGroupNames(
              request.getAwsConfig(), request.getEncryptionDetails(), request.getRegion());
          return AwsAsgListAllNamesResponse.builder().aSgNames(aSgNames).executionStatus(SUCCESS).build();
        }
        case LIST_ASG_INSTANCES: {
          AwsAsgListInstancesRequest awsAsgListInstancesRequest = (AwsAsgListInstancesRequest) parameters;
          List<Instance> aSgInstances = awsAsgHelperServiceDelegate.listAutoScalingGroupInstances(
              request.getAwsConfig(), request.getEncryptionDetails(), request.getRegion(),
              awsAsgListInstancesRequest.getAutoScalingGroupName());
          return AwsAsgListInstancesResponse.builder().instances(aSgInstances).executionStatus(SUCCESS).build();
        }
        case LIST_DESIRED_CAPACITIES: {
          AwsAsgListDesiredCapacitiesRequest awsAsgListDesiredCapacitiesRequest =
              (AwsAsgListDesiredCapacitiesRequest) parameters;
          Map<String, Integer> capacities =
              awsAsgHelperServiceDelegate.getDesiredCapacitiesOfAsgs(request.getAwsConfig(),
                  request.getEncryptionDetails(), request.getRegion(), awsAsgListDesiredCapacitiesRequest.getAsgs());
          return AwsAsgListDesiredCapacitiesResponse.builder().capacities(capacities).executionStatus(SUCCESS).build();
        }
        default: {
          throw new InvalidRequestException("Invalid request type [" + requestType + "]", WingsException.USER);
        }
      }
    } catch (WingsException exception) {
      throw exception;
    } catch (Exception ex) {
      throw new InvalidRequestException(ex.getMessage(), WingsException.USER);
    }
  }
}