package software.wings.delegatetasks.aws;

import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;
import software.wings.exception.InvalidRequestException;
import software.wings.exception.WingsException;
import software.wings.service.impl.aws.model.AwsLambdaExecuteFunctionRequest;
import software.wings.service.impl.aws.model.AwsLambdaRequest;
import software.wings.service.impl.aws.model.AwsLambdaRequest.AwsLambdaRequestType;
import software.wings.service.impl.aws.model.AwsResponse;
import software.wings.service.intfc.aws.delegate.AwsLambdaHelperServiceDelegate;
import software.wings.waitnotify.NotifyResponseData;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class AwsLambdaTask extends AbstractDelegateRunnableTask {
  private static final Logger logger = LoggerFactory.getLogger(AwsLambdaTask.class);
  @Inject private AwsLambdaHelperServiceDelegate awsLambdaHelperServiceDelegate;

  public AwsLambdaTask(String delegateId, DelegateTask delegateTask, Consumer<NotifyResponseData> consumer,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  public AwsResponse run(Object[] parameters) {
    AwsLambdaRequest request = (AwsLambdaRequest) parameters[0];
    try {
      AwsLambdaRequestType requestType = request.getRequestType();
      switch (requestType) {
        case EXECUTE_LAMBDA_FUNCTION: {
          return awsLambdaHelperServiceDelegate.executeFunction((AwsLambdaExecuteFunctionRequest) request);
        }
        default: {
          throw new InvalidRequestException("Invalid request type [" + requestType + "]", WingsException.USER);
        }
      }
    } catch (WingsException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new InvalidRequestException(ex.getMessage(), ex, WingsException.USER);
    }
  }
}