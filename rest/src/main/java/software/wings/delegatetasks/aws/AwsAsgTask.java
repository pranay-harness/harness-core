package software.wings.delegatetasks.aws;

import static software.wings.exception.WingsException.ExecutionContext.DELEGATE;
import static software.wings.sm.ExecutionStatus.SUCCESS;

import com.google.inject.Inject;

import com.amazonaws.services.ec2.model.Instance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;
import software.wings.exception.InvalidRequestException;
import software.wings.exception.WingsException;
import software.wings.service.impl.aws.model.AwsAsgListAllNamesResponse;
import software.wings.service.impl.aws.model.AwsAsgListInstancesRequest;
import software.wings.service.impl.aws.model.AwsAsgListInstancesResponse;
import software.wings.service.impl.aws.model.AwsAsgRequest;
import software.wings.service.impl.aws.model.AwsAsgRequest.AwsAsgRequestType;
import software.wings.service.impl.aws.model.AwsResponse;
import software.wings.service.intfc.aws.delegate.AwsAsgHelperServiceDelegate;
import software.wings.waitnotify.NotifyResponseData;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class AwsAsgTask extends AbstractDelegateRunnableTask {
  private static final Logger logger = LoggerFactory.getLogger(AwsAsgTask.class);
  @Inject private AwsAsgHelperServiceDelegate awsAsgHelperServiceDelegate;

  public AwsAsgTask(String delegateId, DelegateTask delegateTask, Consumer<NotifyResponseData> consumer,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  public AwsResponse run(Object[] parameters) {
    AwsAsgRequest request = (AwsAsgRequest) parameters[0];
    try {
      AwsAsgRequestType requestType = request.getRequestType();
      switch (requestType) {
        case LIST_ALL_ASG_NAMES: {
          List<String> aSgNames = awsAsgHelperServiceDelegate.listAutoScalingGroupNames(
              request.getAwsConfig(), request.getEncryptionDetails(), request.getRegion());
          return AwsAsgListAllNamesResponse.builder().aSgNames(aSgNames).executionStatus(SUCCESS).build();
        }
        case LIST_ASG_INSTANCES: {
          List<Instance> aSgInstances = awsAsgHelperServiceDelegate.listAutoScalingGroupInstances(
              request.getAwsConfig(), request.getEncryptionDetails(), request.getRegion(),
              ((AwsAsgListInstancesRequest) request).getAutoScalingGroupName());
          return AwsAsgListInstancesResponse.builder().instances(aSgInstances).executionStatus(SUCCESS).build();
        }
        default: {
          throw new InvalidRequestException("Invalid request type [" + requestType + "]", WingsException.USER);
        }
      }
    } catch (WingsException ex) {
      ex.logProcessedMessages(DELEGATE, logger);
      throw ex;
    } catch (Exception ex) {
      throw new InvalidRequestException(ex.getMessage(), WingsException.USER);
    }
  }
}