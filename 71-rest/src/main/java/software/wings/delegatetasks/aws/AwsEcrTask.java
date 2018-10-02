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

public class AwsEcrTask extends AbstractDelegateRunnableTask {
  private static final Logger logger = LoggerFactory.getLogger(AwsEcrTask.class);
  @Inject private AwsEcrHelperServiceDelegate ecrServiceDelegate;

  public AwsEcrTask(String delegateId, DelegateTask delegateTask, Consumer<DelegateTaskResponse> consumer,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
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