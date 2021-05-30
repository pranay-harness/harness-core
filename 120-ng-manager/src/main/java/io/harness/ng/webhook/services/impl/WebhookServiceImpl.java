package io.harness.ng.webhook.services.impl;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.ng.NextGenModule.CONNECTOR_DECORATOR_SERVICE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DecryptableEntity;
import io.harness.beans.DelegateTaskRequest;
import io.harness.beans.HookEventType;
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
import io.harness.ng.webhook.entities.WebhookEvent;
import io.harness.ng.webhook.services.api.WebhookService;
import io.harness.repositories.ng.webhook.spring.WebhookEventRepository;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.service.DelegateGrpcClientWrapper;
import io.harness.utils.IdentifierRefHelper;

import software.wings.beans.TaskType;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.time.Duration;
import java.util.List;
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

  public ScmGitWebhookTaskResponseData upsertWebhook(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String connectorIdentifierRef, String target, HookEventType hookEventType,
      String repoURL) {
    final ScmConnector scmConnector =
        getScmConnector(accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifierRef);
    if (!isEmpty(repoURL)) {
      scmConnector.setUrl(repoURL);
    }
    final List<EncryptedDataDetail> encryptionDetails =
        getEncryptedDataDetails(accountIdentifier, orgIdentifier, projectIdentifier, scmConnector);
    final ScmGitWebhookTaskParams gitWebhookTaskParams =
        ScmGitWebhookTaskParams.builder()
            .gitWebhookTaskType(GitWebhookTaskType.UPSERT)
            .scmConnector(scmConnector)
            .encryptedDataDetails(encryptionDetails)
            .gitWebhookDetails(GitWebhookDetails.builder().hookEventType(hookEventType).target(target).build())
            .build();
    DelegateTaskRequest delegateTaskRequest = DelegateTaskRequest.builder()
                                                  .accountId(accountIdentifier)
                                                  .taskType(TaskType.SCM_GIT_WEBHOOK_TASK.name())
                                                  .taskParameters(gitWebhookTaskParams)
                                                  .executionTimeout(Duration.ofMinutes(2))
                                                  .build();
    return (ScmGitWebhookTaskResponseData) delegateGrpcClientWrapper.executeSyncTask(delegateTaskRequest);
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
