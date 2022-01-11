/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.artifactory;

import io.harness.connector.ConnectorValidationResult;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.artifactory.ArtifactoryTaskParams;
import io.harness.delegate.beans.artifactory.ArtifactoryTaskParams.TaskType;
import io.harness.delegate.beans.artifactory.ArtifactoryTaskResponse;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryAuthCredentialsDTO;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryConnectorDTO;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryValidationParams;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.errorhandling.NGErrorHelper;
import io.harness.exception.InvalidRequestException;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.SecretDecryptionService;

import com.google.inject.Inject;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import org.apache.commons.lang3.NotImplementedException;

public class ArtifactoryDelegateTask extends AbstractDelegateRunnableTask {
  @Inject SecretDecryptionService decryptionService;
  @Inject ArtifactoryRequestMapper artifactoryRequestMapper;
  @Inject NGErrorHelper ngErrorHelper;
  @Inject ArtifactoryValidationHandler artifactoryValidationHandler;

  public ArtifactoryDelegateTask(DelegateTaskPackage delegateTaskPackage,
      ILogStreamingTaskClient logStreamingTaskClient, Consumer<DelegateTaskResponse> consumer,
      BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public DelegateResponseData run(Object[] parameters) {
    throw new NotImplementedException("not implemented");
  }

  @Override
  public DelegateResponseData run(TaskParameters parameters) {
    final ArtifactoryTaskParams artifactoryTaskParams = (ArtifactoryTaskParams) parameters;
    final ArtifactoryConnectorDTO artifactoryConnectorDTO = artifactoryTaskParams.getArtifactoryConnectorDTO();
    final List<EncryptedDataDetail> encryptedDataDetails = artifactoryTaskParams.getEncryptedDataDetails();
    final ArtifactoryAuthCredentialsDTO credentials = artifactoryConnectorDTO.getAuth().getCredentials();
    decryptionService.decrypt(credentials, encryptedDataDetails);
    final TaskType taskType = artifactoryTaskParams.getTaskType();
    switch (taskType) {
      case VALIDATE:
        return validateArtifactoryConfig(artifactoryConnectorDTO, encryptedDataDetails);
      default:
        throw new InvalidRequestException("No task found for " + taskType.name());
    }
  }

  private DelegateResponseData validateArtifactoryConfig(
      ArtifactoryConnectorDTO artifactoryConnectorDTO, List<EncryptedDataDetail> encryptedDataDetails) {
    final ArtifactoryValidationParams artifactoryValidationParams =
        ArtifactoryValidationParams.builder()
            .encryptedDataDetails(encryptedDataDetails)
            .artifactoryConnectorDTO(artifactoryConnectorDTO)
            .build();
    ConnectorValidationResult connectorValidationResult =
        artifactoryValidationHandler.validate(artifactoryValidationParams, getAccountId());
    connectorValidationResult.setDelegateId(getDelegateId());
    return ArtifactoryTaskResponse.builder().connectorValidationResult(connectorValidationResult).build();
  }

  @Override
  public boolean isSupportingErrorFramework() {
    return true;
  }
}
