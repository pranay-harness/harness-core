package software.wings.delegatetasks.aws;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.beans.ExecutionStatus.SUCCESS;

import com.google.inject.Inject;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import software.wings.beans.DelegateTaskPackage;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;
import software.wings.service.impl.aws.model.AwsEcrGetAuthTokenRequest;
import software.wings.service.impl.aws.model.AwsEcrGetAuthTokenResponse;
import software.wings.service.impl.aws.model.AwsEcrGetImageUrlRequest;
import software.wings.service.impl.aws.model.AwsEcrGetImageUrlResponse;
import software.wings.service.impl.aws.model.AwsEcrRequest;
import software.wings.service.impl.aws.model.AwsEcrRequest.AwsEcrRequestType;
import software.wings.service.impl.aws.model.AwsResponse;
import software.wings.service.intfc.aws.delegate.AwsEcrHelperServiceDelegate;

import java.util.function.Consumer;
import java.util.function.Supplier;

@OwnedBy(CDC)
@Slf4j
public class AwsEcrTask extends AbstractDelegateRunnableTask {
  @Inject private AwsEcrHelperServiceDelegate ecrServiceDelegate;

  public AwsEcrTask(
      DelegateTaskPackage delegateTaskPackage, Consumer<DelegateTaskResponse> consumer, Supplier<Boolean> preExecute) {
    super(delegateTaskPackage, consumer, preExecute);
  }

  @Override
  public AwsResponse run(TaskParameters parameters) {
    throw new NotImplementedException("not implemented");
  }

  @Override
  public AwsResponse run(Object[] parameters) {
    AwsEcrRequest request = (AwsEcrRequest) parameters[0];
    AwsEcrRequestType requestType = request.getRequestType();
    try {
      switch (requestType) {
        case GET_ECR_IMAGE_URL: {
          String imageUrl = ecrServiceDelegate.getEcrImageUrl(request.getAwsConfig(), request.getEncryptionDetails(),
              request.getRegion(), ((AwsEcrGetImageUrlRequest) request).getImageName());
          return AwsEcrGetImageUrlResponse.builder().ecrImageUrl(imageUrl).executionStatus(SUCCESS).build();
        }
        case GET_ECR_AUTH_TOKEN: {
          String ecrAuthToken =
              ecrServiceDelegate.getAmazonEcrAuthToken(request.getAwsConfig(), request.getEncryptionDetails(),
                  ((AwsEcrGetAuthTokenRequest) request).getAwsAccount(), request.getRegion());
          return AwsEcrGetAuthTokenResponse.builder().ecrAuthToken(ecrAuthToken).executionStatus(SUCCESS).build();
        }
        default: {
          throw new InvalidRequestException("Invalid request type [" + requestType + "]", WingsException.USER);
        }
      }
    } catch (WingsException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new InvalidRequestException(ex.getMessage(), WingsException.USER);
    }
  }
}