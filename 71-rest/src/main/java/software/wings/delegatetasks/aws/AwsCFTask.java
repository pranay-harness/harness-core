package software.wings.delegatetasks.aws;

import static software.wings.sm.ExecutionStatus.SUCCESS;

import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import software.wings.beans.DelegateTask;
import software.wings.beans.DelegateTaskResponse;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;
import software.wings.service.impl.aws.model.AwsCFGetTemplateParamsRequest;
import software.wings.service.impl.aws.model.AwsCFGetTemplateParamsResponse;
import software.wings.service.impl.aws.model.AwsCFRequest;
import software.wings.service.impl.aws.model.AwsCFRequest.AwsCFRequestType;
import software.wings.service.impl.aws.model.AwsCFTemplateParamsData;
import software.wings.service.impl.aws.model.AwsResponse;
import software.wings.service.intfc.aws.delegate.AwsCFHelperServiceDelegate;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class AwsCFTask extends AbstractDelegateRunnableTask {
  @Inject private AwsCFHelperServiceDelegate awsCFHelperServiceDelegate;

  public AwsCFTask(String delegateId, DelegateTask delegateTask, Consumer<DelegateTaskResponse> consumer,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  public AwsResponse run(Object[] parameters) {
    AwsCFRequest request = (AwsCFRequest) parameters[0];
    try {
      AwsCFRequestType requestType = request.getRequestType();
      switch (requestType) {
        case GET_TEMPLATE_PARAMETERS: {
          AwsCFGetTemplateParamsRequest paramsRequest = (AwsCFGetTemplateParamsRequest) request;
          List<AwsCFTemplateParamsData> paramsData = awsCFHelperServiceDelegate.getParamsData(
              paramsRequest.getAwsConfig(), paramsRequest.getEncryptionDetails(), paramsRequest.getRegion(),
              paramsRequest.getData(), paramsRequest.getType());
          return AwsCFGetTemplateParamsResponse.builder().executionStatus(SUCCESS).parameters(paramsData).build();
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
