package software.wings.delegatetasks.aws;

import static io.harness.beans.ExecutionStatus.SUCCESS;
import static java.util.stream.Collectors.toList;

import com.google.inject.Inject;

import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.aws.AwsElbListener;
import io.harness.delegate.task.aws.AwsLoadBalancerDetails;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import software.wings.beans.DelegateTaskPackage;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;
import software.wings.service.impl.aws.model.AwsElbListAppElbsResponse;
import software.wings.service.impl.aws.model.AwsElbListClassicElbsResponse;
import software.wings.service.impl.aws.model.AwsElbListListenerRequest;
import software.wings.service.impl.aws.model.AwsElbListListenerResponse;
import software.wings.service.impl.aws.model.AwsElbListTargetGroupsRequest;
import software.wings.service.impl.aws.model.AwsElbListTargetGroupsResponse;
import software.wings.service.impl.aws.model.AwsElbRequest;
import software.wings.service.impl.aws.model.AwsElbRequest.AwsElbRequestType;
import software.wings.service.impl.aws.model.AwsResponse;
import software.wings.service.intfc.aws.delegate.AwsElbHelperServiceDelegate;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Slf4j
public class AwsElbTask extends AbstractDelegateRunnableTask {
  @Inject private AwsElbHelperServiceDelegate elbHelperServiceDelegate;

  public AwsElbTask(
      DelegateTaskPackage delegateTaskPackage, Consumer<DelegateTaskResponse> consumer, Supplier<Boolean> preExecute) {
    super(delegateTaskPackage, consumer, preExecute);
  }

  @Override
  public AwsResponse run(TaskParameters parameters) {
    throw new NotImplementedException("not implemented");
  }

  @Override
  public AwsResponse run(Object[] parameters) {
    AwsElbRequest request = (AwsElbRequest) parameters[0];
    try {
      AwsElbRequestType requestType = request.getRequestType();
      switch (requestType) {
        case LIST_CLASSIC_ELBS: {
          List<String> classicLbs = elbHelperServiceDelegate.listClassicLoadBalancers(
              request.getAwsConfig(), request.getEncryptionDetails(), request.getRegion());
          return AwsElbListClassicElbsResponse.builder().classicElbs(classicLbs).executionStatus(SUCCESS).build();
        }
        case LIST_APPLICATION_LBS: {
          List<AwsLoadBalancerDetails> applicationLbs = elbHelperServiceDelegate.listApplicationLoadBalancerDetails(
              request.getAwsConfig(), request.getEncryptionDetails(), request.getRegion());
          return AwsElbListAppElbsResponse.builder().appElbs(applicationLbs).executionStatus(SUCCESS).build();
        }
        case LIST_NETWORK_LBS: {
          List<AwsLoadBalancerDetails> applicationLbs = elbHelperServiceDelegate.listNetworkLoadBalancerDetails(
              request.getAwsConfig(), request.getEncryptionDetails(), request.getRegion());
          return AwsElbListAppElbsResponse.builder().appElbs(applicationLbs).executionStatus(SUCCESS).build();
        }
        case LIST_ELB_LBS: {
          List<AwsLoadBalancerDetails> applicationLbs = elbHelperServiceDelegate.listElasticLoadBalancerDetails(
              request.getAwsConfig(), request.getEncryptionDetails(), request.getRegion());
          return AwsElbListAppElbsResponse.builder().appElbs(applicationLbs).executionStatus(SUCCESS).build();
        }
        case LIST_TARGET_GROUPS_FOR_ALBS: {
          Map<String, String> targetGroups =
              elbHelperServiceDelegate.listTargetGroupsForAlb(request.getAwsConfig(), request.getEncryptionDetails(),
                  request.getRegion(), ((AwsElbListTargetGroupsRequest) request).getLoadBalancerName());
          return AwsElbListTargetGroupsResponse.builder().targetGroups(targetGroups).executionStatus(SUCCESS).build();
        }
        case LIST_LISTENER_FOR_ELB: {
          List<AwsElbListener> listeners = elbHelperServiceDelegate.getElbListenersForLoadBalaner(
              request.getAwsConfig(), request.getEncryptionDetails(), request.getRegion(),
              ((AwsElbListListenerRequest) request).getLoadBalancerName());
          return AwsElbListListenerResponse.builder().awsElbListeners(listeners).executionStatus(SUCCESS).build();
        }
        default: {
          throw new InvalidRequestException("Invalid request type [" + requestType + "]", WingsException.USER);
        }
      }
    } catch (WingsException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new InvalidRequestException(exception.getMessage(), WingsException.USER);
    }
  }

  private List<String> generateLoadBalancerNamesList(List<AwsLoadBalancerDetails> details) {
    return details.stream().map(AwsLoadBalancerDetails::getName).collect(toList());
  }
}