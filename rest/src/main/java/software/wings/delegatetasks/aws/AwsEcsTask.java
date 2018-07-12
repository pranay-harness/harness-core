package software.wings.delegatetasks.aws;

import static software.wings.exception.WingsException.ExecutionContext.DELEGATE;
import static software.wings.sm.ExecutionStatus.SUCCESS;

import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;
import software.wings.exception.InvalidRequestException;
import software.wings.exception.WingsException;
import software.wings.service.impl.aws.model.AwsEcsListClustersResponse;
import software.wings.service.impl.aws.model.AwsEcsRequest;
import software.wings.service.impl.aws.model.AwsEcsRequest.AwsEcsRequestType;
import software.wings.service.impl.aws.model.AwsResponse;
import software.wings.service.intfc.aws.delegate.AwsEcsHelperServiceDelegate;
import software.wings.waitnotify.NotifyResponseData;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class AwsEcsTask extends AbstractDelegateRunnableTask {
  private static final Logger logger = LoggerFactory.getLogger(AwsEcsTask.class);
  @Inject private AwsEcsHelperServiceDelegate ecsHelperServiceDelegate;

  public AwsEcsTask(String delegateId, DelegateTask delegateTask, Consumer<NotifyResponseData> consumer,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  public AwsResponse run(Object[] parameters) {
    AwsEcsRequest request = (AwsEcsRequest) parameters[0];
    try {
      AwsEcsRequestType requestType = request.getRequestType();
      switch (requestType) {
        case LIST_CLUSTERS: {
          List<String> clusters = ecsHelperServiceDelegate.listClusters(
              request.getAwsConfig(), request.getEncryptionDetails(), request.getRegion());
          return AwsEcsListClustersResponse.builder().clusters(clusters).executionStatus(SUCCESS).build();
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