/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.webhook.services.impl;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.ng.NextGenModule.CONNECTOR_DECORATOR_SERVICE;

import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.impl.ConnectorErrorMessagesHelper;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.task.scm.GitWebhookTaskType;
import io.harness.exception.InvalidRequestException;
import io.harness.gitsync.common.service.ScmOrchestratorService;
import io.harness.ng.BaseUrls;
import io.harness.ng.webhook.UpsertWebhookRequestDTO;
import io.harness.ng.webhook.UpsertWebhookResponseDTO;
import io.harness.ng.webhook.WebhookConstants;
import io.harness.ng.webhook.entities.WebhookEvent;
import io.harness.ng.webhook.services.api.WebhookEventService;
import io.harness.ng.webhook.services.api.WebhookService;
import io.harness.product.ci.scm.proto.CreateWebhookResponse;
import io.harness.repositories.ng.webhook.spring.WebhookEventRepository;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.service.DelegateGrpcClientWrapper;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@OwnedBy(PIPELINE)
public class WebhookServiceImpl implements WebhookService, WebhookEventService {
  private final WebhookEventRepository webhookEventRepository;
  private final DelegateGrpcClientWrapper delegateGrpcClientWrapper;
  private final SecretManagerClientService secretManagerClientService;
  private final ConnectorService connectorService;
  private final ConnectorErrorMessagesHelper connectorErrorMessagesHelper;
  private final BaseUrls baseUrls;
  private final ScmOrchestratorService scmOrchestratorService;

  @Inject
  public WebhookServiceImpl(WebhookEventRepository webhookEventRepository,
      DelegateGrpcClientWrapper delegateGrpcClientWrapper, SecretManagerClientService secretManagerClientService,
      @Named(CONNECTOR_DECORATOR_SERVICE) ConnectorService connectorService,
      ConnectorErrorMessagesHelper connectorErrorMessagesHelper, BaseUrls baseUrls,
      ScmOrchestratorService scmOrchestratorService) {
    this.webhookEventRepository = webhookEventRepository;
    this.delegateGrpcClientWrapper = delegateGrpcClientWrapper;
    this.secretManagerClientService = secretManagerClientService;
    this.connectorService = connectorService;
    this.connectorErrorMessagesHelper = connectorErrorMessagesHelper;
    this.baseUrls = baseUrls;
    this.scmOrchestratorService = scmOrchestratorService;
  }

  @Override
  public WebhookEvent addEventToQueue(WebhookEvent webhookEvent) {
    try {
      log.info(
          "received webhook event with uuid {}, accountId {} ", webhookEvent.getUuid(), webhookEvent.getAccountId());
      return webhookEventRepository.save(webhookEvent);
    } catch (Exception e) {
      throw new InvalidRequestException("Webhook event could not be saved for processing");
    }
  }

  public UpsertWebhookResponseDTO upsertWebhook(UpsertWebhookRequestDTO upsertWebhookRequestDTO) {
    String target = getTargetUrl(upsertWebhookRequestDTO.getAccountIdentifier());
    CreateWebhookResponse createWebhookResponse =
        scmOrchestratorService.processScmRequestUsingConnectorSettings(scmClientFacilitatorService
            -> scmClientFacilitatorService.upsertWebhook(upsertWebhookRequestDTO, target, GitWebhookTaskType.UPSERT),
            upsertWebhookRequestDTO.getProjectIdentifier(), upsertWebhookRequestDTO.getOrgIdentifier(),
            upsertWebhookRequestDTO.getAccountIdentifier(), upsertWebhookRequestDTO.getConnectorIdentifierRef());
    return UpsertWebhookResponseDTO.builder()
        .webhookResponse(createWebhookResponse.getWebhook())
        .error(createWebhookResponse.getError())
        .status(createWebhookResponse.getStatus())
        .build();
  }

  @VisibleForTesting
  String getTargetUrl(String accountIdentifier) {
    String webhookBaseUrl = getWebhookBaseUrl();
    if (!webhookBaseUrl.endsWith("/")) {
      webhookBaseUrl += "/";
    }
    StringBuilder webhookUrl = new StringBuilder(webhookBaseUrl)
                                   .append(WebhookConstants.WEBHOOK_ENDPOINT)
                                   .append('?')
                                   .append(NGCommonEntityConstants.ACCOUNT_KEY)
                                   .append('=')
                                   .append(accountIdentifier);
    return webhookUrl.toString();
  }

  @VisibleForTesting
  String getWebhookBaseUrl() {
    return baseUrls.getWebhookBaseUrl();
  }
}
