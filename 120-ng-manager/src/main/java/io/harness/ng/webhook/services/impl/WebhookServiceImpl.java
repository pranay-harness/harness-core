package io.harness.ng.webhook.services.impl;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.ng.NextGenModule.CONNECTOR_DECORATOR_SERVICE;
import static io.harness.utils.DelegateOwner.getNGTaskSetupAbstractionsWithOwner;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DecryptableEntity;
import io.harness.beans.DelegateTaskRequest;
import io.harness.beans.IdentifierRef;
import io.harness.beans.gitsync.GitWebhookDetails;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.helper.GitApiAccessDecryptionHelper;
import io.harness.connector.impl.ConnectorErrorMessagesHelper;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.task.scm.GitWebhookTaskType;
import io.harness.delegate.task.scm.ScmGitWebhookTaskParams;
import io.harness.delegate.task.scm.ScmGitWebhookTaskResponseData;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.BaseNGAccess;
import io.harness.ng.webhook.UpsertWebhookRequestDTO;
import io.harness.ng.webhook.UpsertWebhookResponseDTO;
import io.harness.ng.webhook.entities.WebhookEvent;
import io.harness.ng.webhook.services.api.WebhookService;
import io.harness.product.ci.scm.proto.CreateWebhookResponse;
import io.harness.repositories.ng.webhook.spring.WebhookEventRepository;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.service.DelegateGrpcClientWrapper;
import io.harness.utils.IdentifierRefHelper;

import software.wings.beans.TaskType;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.protobuf.InvalidProtocolBufferException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@OwnedBy(PIPELINE)
public class WebhookServiceImpl implements WebhookService {
  private final WebhookEventRepository webhookEventRepository;
  private final DelegateGrpcClientWrapper delegateGrpcClientWrapper;
  private final SecretManagerClientService secretManagerClientService;
  private final ConnectorService connectorService;
  private final ConnectorErrorMessagesHelper connectorErrorMessagesHelper;

  @Inject
  public WebhookServiceImpl(WebhookEventRepository webhookEventRepository,
      DelegateGrpcClientWrapper delegateGrpcClientWrapper, SecretManagerClientService secretManagerClientService,
      @Named(CONNECTOR_DECORATOR_SERVICE) ConnectorService connectorService,
      ConnectorErrorMessagesHelper connectorErrorMessagesHelper) {
    this.webhookEventRepository = webhookEventRepository;
    this.delegateGrpcClientWrapper = delegateGrpcClientWrapper;
    this.secretManagerClientService = secretManagerClientService;
    this.connectorService = connectorService;
    this.connectorErrorMessagesHelper = connectorErrorMessagesHelper;
  }

  @Override
  public WebhookEvent addEventToQueue(WebhookEvent webhookEvent) {
    try {
      return webhookEventRepository.save(webhookEvent);
    } catch (Exception e) {
      throw new InvalidRequestException("Webhook event could not be saved for processing");
    }
  }

  public UpsertWebhookResponseDTO upsertWebhook(UpsertWebhookRequestDTO upsertWebhookRequestDTO) {
    final ScmConnector scmConnector =
        getScmConnector(upsertWebhookRequestDTO.getAccountIdentifier(), upsertWebhookRequestDTO.getOrgIdentifier(),
            upsertWebhookRequestDTO.getProjectIdentifier(), upsertWebhookRequestDTO.getConnectorIdentifierRef());
    if (!isEmpty(upsertWebhookRequestDTO.getRepoURL())) {
      scmConnector.setUrl(upsertWebhookRequestDTO.getRepoURL());
    }
    final List<EncryptedDataDetail> encryptionDetails =
        getEncryptedDataDetails(upsertWebhookRequestDTO.getAccountIdentifier(),
            upsertWebhookRequestDTO.getOrgIdentifier(), upsertWebhookRequestDTO.getProjectIdentifier(), scmConnector);
    final ScmGitWebhookTaskParams gitWebhookTaskParams =
        ScmGitWebhookTaskParams.builder()
            .gitWebhookTaskType(GitWebhookTaskType.UPSERT)
            .scmConnector(scmConnector)
            .encryptedDataDetails(encryptionDetails)
            .gitWebhookDetails(GitWebhookDetails.builder()
                                   .hookEventType(upsertWebhookRequestDTO.getHookEventType())
                                   .target(upsertWebhookRequestDTO.getTarget())
                                   .build())
            .build();
    final Map<String, String> ngTaskSetupAbstractionsWithOwner =
        getNGTaskSetupAbstractionsWithOwner(upsertWebhookRequestDTO.getAccountIdentifier(),
            upsertWebhookRequestDTO.getOrgIdentifier(), upsertWebhookRequestDTO.getProjectIdentifier());
    DelegateTaskRequest delegateTaskRequest = DelegateTaskRequest.builder()
                                                  .accountId(upsertWebhookRequestDTO.getAccountIdentifier())
                                                  .taskSetupAbstractions(ngTaskSetupAbstractionsWithOwner)
                                                  .taskType(TaskType.SCM_GIT_WEBHOOK_TASK.name())
                                                  .taskParameters(gitWebhookTaskParams)
                                                  .executionTimeout(Duration.ofMinutes(2))
                                                  .build();
    ScmGitWebhookTaskResponseData scmGitWebhookTaskResponseData =
        (ScmGitWebhookTaskResponseData) delegateGrpcClientWrapper.executeSyncTask(delegateTaskRequest);
    try {
      final CreateWebhookResponse createWebhookResponse =
          CreateWebhookResponse.parseFrom(scmGitWebhookTaskResponseData.getCreateWebhookResponse());
      return UpsertWebhookResponseDTO.builder()
          .webhookResponse(createWebhookResponse.getWebhook())
          .error(createWebhookResponse.getError())
          .status(createWebhookResponse.getStatus())
          .build();
    } catch (InvalidProtocolBufferException e) {
      throw new InvalidRequestException(String.format("Exception in unpacking Create Webhook Response", e));
    }
  }

  private List<EncryptedDataDetail> getEncryptedDataDetails(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, ScmConnector scmConnector) {
    final BaseNGAccess baseNGAccess = getBaseNGAccess(accountIdentifier, orgIdentifier, projectIdentifier);
    final DecryptableEntity apiAccessDecryptableEntity =
        GitApiAccessDecryptionHelper.getAPIAccessDecryptableEntity(scmConnector);
    return secretManagerClientService.getEncryptionDetails(baseNGAccess, apiAccessDecryptableEntity);
  }

  private ScmConnector getScmConnector(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifierRef) {
    final IdentifierRef gitConnectorIdentifierRef =
        getConnectorIdentifierRef(accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifierRef);
    return getScmConnector(gitConnectorIdentifierRef);
  }

  private BaseNGAccess getBaseNGAccess(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    return BaseNGAccess.builder()
        .accountIdentifier(accountIdentifier)
        .orgIdentifier(orgIdentifier)
        .projectIdentifier(projectIdentifier)
        .build();
  }

  private ScmConnector getScmConnector(IdentifierRef connectorIdentifierRef) {
    final ConnectorResponseDTO connectorResponseDTO =
        connectorService
            .get(connectorIdentifierRef.getAccountIdentifier(), connectorIdentifierRef.getOrgIdentifier(),
                connectorIdentifierRef.getProjectIdentifier(), connectorIdentifierRef.getIdentifier())
            .orElseThrow(
                ()
                    -> new InvalidRequestException(connectorErrorMessagesHelper.createConnectorNotFoundMessage(
                        connectorIdentifierRef.getAccountIdentifier(), connectorIdentifierRef.getOrgIdentifier(),
                        connectorIdentifierRef.getProjectIdentifier(), connectorIdentifierRef.getIdentifier())));
    return (ScmConnector) connectorResponseDTO.getConnector().getConnectorConfig();
  }

  private IdentifierRef getConnectorIdentifierRef(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifierRef) {
    return IdentifierRefHelper.getIdentifierRef(
        connectorIdentifierRef, accountIdentifier, orgIdentifier, projectIdentifier);
  }
}
