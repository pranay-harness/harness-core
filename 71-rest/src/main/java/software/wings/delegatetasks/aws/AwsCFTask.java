package software.wings.delegatetasks.aws;

import static io.harness.beans.ExecutionStatus.SUCCESS;

import com.google.inject.Inject;

import groovy.util.logging.Slf4j;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import org.apache.commons.lang3.NotImplementedException;
import software.wings.beans.DelegateTaskPackage;
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

@Slf4j
public class AwsCFTask extends AbstractDelegateRunnableTask {
  @Inject private AwsCFHelperServiceDelegate awsCFHelperServiceDelegate;

  public AwsCFTask(
      DelegateTaskPackage delegateTaskPackage, Consumer<DelegateTaskResponse> consumer, Supplier<Boolean> preExecute) {
    super(delegateTaskPackage, consumer, preExecute);
  }

  @Override
  public AwsResponse run(TaskParameters parameters) {
    throw new NotImplementedException("not implemented");
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
              paramsRequest.getData(), paramsRequest.getType(), paramsRequest.getGitFileConfig(),
              paramsRequest.getGitConfig(), paramsRequest.getSourceRepoEncryptionDetails());
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
