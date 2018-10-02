package software.wings.delegatetasks.aws;

import static software.wings.sm.ExecutionStatus.SUCCESS;

import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.DelegateTaskResponse;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;
import software.wings.service.impl.aws.model.AwsElbListAppElbsResponse;
import software.wings.service.impl.aws.model.AwsElbListClassicElbsResponse;
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

public class AwsElbTask extends AbstractDelegateRunnableTask {
  private static final Logger logger = LoggerFactory.getLogger(AwsElbTask.class);
  @Inject private AwsElbHelperServiceDelegate elbHelperServiceDelegate;

  public AwsElbTask(String delegateId, DelegateTask delegateTask, Consumer<DelegateTaskResponse> consumer,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
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
          List<String> applicationLbs = elbHelperServiceDelegate.listApplicationLoadBalancers(
              request.getAwsConfig(), request.getEncryptionDetails(), request.getRegion());
          return AwsElbListAppElbsResponse.builder().appElbs(applicationLbs).executionStatus(SUCCESS).build();
        }
        case LIST_TARGET_GROUPS_FOR_ALBS: {
          Map<String, String> targetGroups =
              elbHelperServiceDelegate.listTargetGroupsForAlb(request.getAwsConfig(), request.getEncryptionDetails(),
                  request.getRegion(), ((AwsElbListTargetGroupsRequest) request).getLoadBalancerName());
          return AwsElbListTargetGroupsResponse.builder().targetGroups(targetGroups).executionStatus(SUCCESS).build();
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
}