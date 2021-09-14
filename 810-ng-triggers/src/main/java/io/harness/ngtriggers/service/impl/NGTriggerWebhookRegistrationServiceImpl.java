/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ngtriggers.service.impl;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.remote.client.NGRestUtils.getResponse;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.HookEventType;
import io.harness.ng.webhook.UpsertWebhookRequestDTO;
import io.harness.ng.webhook.UpsertWebhookResponseDTO;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;
import io.harness.ngtriggers.beans.entity.metadata.WebhookRegistrationStatus;
import io.harness.ngtriggers.service.NGTriggerWebhookRegistrationService;
import io.harness.product.ci.scm.proto.WebhookResponse;
import io.harness.webhook.remote.WebhookEventClient;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(PIPELINE)
public class NGTriggerWebhookRegistrationServiceImpl implements NGTriggerWebhookRegistrationService {
  private final WebhookEventClient webhookEventClient;

  @Override
  public WebhookRegistrationStatus registerWebhook(NGTriggerEntity ngTriggerEntity) {
    return registerWebhookInternal(ngTriggerEntity.getProjectIdentifier(), ngTriggerEntity.getOrgIdentifier(),
        ngTriggerEntity.getAccountId(), ngTriggerEntity.getMetadata().getWebhook().getGit().getRepoName(),
        ngTriggerEntity.getMetadata().getWebhook().getGit().getConnectorIdentifier());
  }

  private WebhookRegistrationStatus registerWebhookInternal(String projectIdentifier, String orgIdentifier,
      String accountIdentifier, String repoUrl, String connectorIdentifierRef) {
    UpsertWebhookRequestDTO upsertWebhookRequestDTO = UpsertWebhookRequestDTO.builder()
                                                          .projectIdentifier(projectIdentifier)
                                                          .orgIdentifier(orgIdentifier)
                                                          .accountIdentifier(accountIdentifier)
                                                          .connectorIdentifierRef(connectorIdentifierRef)
                                                          .repoURL(repoUrl)
                                                          .hookEventType(HookEventType.TRIGGER_EVENTS)
                                                          .build();
    UpsertWebhookResponseDTO upsertWebhookResponseDTO = null;
    try {
      upsertWebhookResponseDTO = getResponse(webhookEventClient.upsertWebhook(upsertWebhookRequestDTO));
    } catch (Exception ex) {
      log.error("Failed to register webhook", ex);
      return WebhookRegistrationStatus.ERROR;
    }
    if (upsertWebhookResponseDTO.getStatus() > 300) {
      return WebhookRegistrationStatus.FAILED;
    }
    WebhookResponse webhookResponse = upsertWebhookResponseDTO.getWebhookResponse();
    if (webhookResponse != null) {
      log.info("Auto registered webhook with following events: {}", webhookResponse.getName());
    }
    return WebhookRegistrationStatus.SUCCESS;
  }
}
