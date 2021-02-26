package io.harness.delegate.task.aws;

import static io.harness.aws.AwsExceptionHandler.handleAmazonClientException;
import static io.harness.aws.AwsExceptionHandler.handleAmazonServiceException;

import io.harness.aws.AwsClient;
import io.harness.aws.AwsConfig;
import io.harness.connector.ConnectivityStatus;
import io.harness.connector.ConnectorValidationResult;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialType;
import io.harness.delegate.beans.connector.awsconnector.AwsTaskParams;
import io.harness.delegate.beans.connector.awsconnector.AwsTaskType;
import io.harness.delegate.beans.connector.awsconnector.AwsValidateTaskResponse;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.errorhandling.NGErrorHelper;
import io.harness.exception.InvalidRequestException;
import io.harness.security.encryption.EncryptedDataDetail;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.google.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;

@Slf4j
public class AwsDelegateTask extends AbstractDelegateRunnableTask {
  @Inject private AwsClient awsClient;
  @Inject private AwsNgConfigMapper awsNgConfigMapper;
  @Inject private NGErrorHelper ngErrorHelper;

  public AwsDelegateTask(DelegateTaskPackage delegateTaskPackage, ILogStreamingTaskClient logStreamingTaskClient,
      Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public DelegateResponseData run(Object[] parameters) {
    throw new NotImplementedException("Not implemented");
  }

  @Override
  public DelegateResponseData run(TaskParameters parameters) {
    final AwsTaskParams awsTaskParams = (AwsTaskParams) parameters;
    final AwsTaskType awsTaskType = awsTaskParams.getAwsTaskType();
    final List<EncryptedDataDetail> encryptionDetails = awsTaskParams.getEncryptionDetails();
    try {
      switch (awsTaskType) {
        // TODO: we can move this to factory method using guice mapbinder later
        case VALIDATE:
          return handleValidateTask(awsTaskParams, encryptionDetails);
        default:
          throw new InvalidRequestException("Task type not identified");
      }
    } catch (Exception e) {
      String errorMessage = e.getMessage();
      ConnectorValidationResult connectorValidationResult =
          ConnectorValidationResult.builder()
              .status(ConnectivityStatus.FAILURE)
              .errors(Collections.singletonList(ngErrorHelper.createErrorDetail(errorMessage)))
              .errorSummary(ngErrorHelper.getErrorSummary(errorMessage))
              .testedAt(System.currentTimeMillis())
              .delegateId(getDelegateId())
              .build();
      return AwsValidateTaskResponse.builder().connectorValidationResult(connectorValidationResult).build();
    }
  }

  public DelegateResponseData handleValidateTask(
      AwsTaskParams awsTaskParams, List<EncryptedDataDetail> encryptionDetails) {
    final AwsConnectorDTO awsConnector = awsTaskParams.getAwsConnector();
    final AwsCredentialDTO credential = awsConnector.getCredential();
    final AwsCredentialType awsCredentialType = credential.getAwsCredentialType();
    final AwsConfig awsConfig =
        awsNgConfigMapper.mapAwsConfigWithDecryption(credential, awsCredentialType, encryptionDetails);
    try {
      awsClient.validateAwsAccountCredential(awsConfig);
      ConnectorValidationResult connectorValidationResult = ConnectorValidationResult.builder()
                                                                .status(ConnectivityStatus.SUCCESS)
                                                                .delegateId(getDelegateId())
                                                                .testedAt(System.currentTimeMillis())
                                                                .build();
      return AwsValidateTaskResponse.builder().connectorValidationResult(connectorValidationResult).build();
    } catch (AmazonEC2Exception amazonEC2Exception) {
      handleAmazonServiceException(amazonEC2Exception);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    throw new InvalidRequestException("Unsuccessful validation");
  }
}
