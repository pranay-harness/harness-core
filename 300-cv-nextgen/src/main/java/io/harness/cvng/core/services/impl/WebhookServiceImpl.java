package io.harness.cvng.core.services.impl;

import io.harness.cvng.beans.change.ChangeEventDTO;
import io.harness.cvng.beans.change.ChangeSourceType;
import io.harness.cvng.beans.change.PagerDutyEventMetaData;
import io.harness.cvng.core.beans.PagerDutyIncidentDTO;
import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.core.beans.params.ServiceEnvironmentParams;
import io.harness.cvng.core.entities.PagerDutyWebhook;
import io.harness.cvng.core.entities.PagerDutyWebhook.PagerDutyWebhookKeys;
import io.harness.cvng.core.entities.Webhook;
import io.harness.cvng.core.entities.Webhook.WebhookKeys;
import io.harness.cvng.core.services.api.ChangeEventService;
import io.harness.cvng.core.services.api.WebhookService;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import groovy.util.logging.Slf4j;
import java.time.Instant;

@Slf4j
public class WebhookServiceImpl implements WebhookService {
  @Inject private HPersistence hPersistence;
  @Inject private ChangeEventService changeEventService;

  @Override
  public void createPagerdutyWebhook(
      ServiceEnvironmentParams serviceEnvironmentParams, String token, String webhookId, String changeSourceId) {
    PagerDutyWebhook pagerDutyWebhook = PagerDutyWebhook.builder()
                                            .accountId(serviceEnvironmentParams.getAccountIdentifier())
                                            .orgIdentifier(serviceEnvironmentParams.getOrgIdentifier())
                                            .projectIdentifier(serviceEnvironmentParams.getProjectIdentifier())
                                            .serviceIdentifier(serviceEnvironmentParams.getServiceIdentifier())
                                            .envIdentifier(serviceEnvironmentParams.getEnvironmentIdentifier())
                                            .pagerdutyChangeSourceId(changeSourceId)
                                            .token(token)
                                            .webhookId(webhookId)
                                            .build();
    hPersistence.save(pagerDutyWebhook);
  }

  @Override
  public PagerDutyWebhook getPagerdutyWebhook(ProjectParams projectParams, String changeSourceId) {
    return (PagerDutyWebhook) hPersistence.createQuery(Webhook.class)
        .filter(PagerDutyWebhookKeys.pagerdutyChangeSourceId, changeSourceId)
        .filter(WebhookKeys.accountId, projectParams.getAccountIdentifier())
        .filter(WebhookKeys.projectIdentifier, projectParams.getProjectIdentifier())
        .filter(WebhookKeys.orgIdentifier, projectParams.getOrgIdentifier())
        .get();
  }

  @Override
  public void deleteWebhook(Webhook webhook) {
    hPersistence.delete(webhook);
  }

  @Override
  public void handlePagerDutyWebhook(String token, PagerDutyIncidentDTO payload) {
    PagerDutyWebhook webhook =
        (PagerDutyWebhook) hPersistence.createQuery(Webhook.class).filter(WebhookKeys.token, token).get();
    PagerDutyEventMetaData eventMetaData = PagerDutyEventMetaData.builder()
                                               .eventId(payload.getId())
                                               .pagerDutyUrl(payload.getSelf())
                                               .title(payload.getTitle())
                                               .build();
    ChangeEventDTO changeEventDTO = ChangeEventDTO.builder()
                                        .accountId(webhook.getAccountId())
                                        .orgIdentifier(webhook.getOrgIdentifier())
                                        .projectIdentifier(webhook.getProjectIdentifier())
                                        .serviceIdentifier(webhook.getServiceIdentifier())
                                        .envIdentifier(webhook.getEnvIdentifier())
                                        .type(ChangeSourceType.PAGER_DUTY)
                                        .eventTime(Instant.now().toEpochMilli())
                                        .changeEventMetaData(eventMetaData)
                                        .build();
    changeEventService.register(changeEventDTO);
  }

  @Override
  public void deleteByProjectIdentifier(
      Class<Webhook> clazz, String accountId, String orgIdentifier, String projectIdentifier) {
    hPersistence.delete(hPersistence.createQuery(Webhook.class)
                            .filter(WebhookKeys.accountId, accountId)
                            .filter(WebhookKeys.orgIdentifier, orgIdentifier)
                            .filter(WebhookKeys.projectIdentifier, projectIdentifier));
  }

  @Override
  public void deleteByOrgIdentifier(Class<Webhook> clazz, String accountId, String orgIdentifier) {
    hPersistence.delete(hPersistence.createQuery(Webhook.class)
                            .filter(WebhookKeys.accountId, accountId)
                            .filter(WebhookKeys.orgIdentifier, orgIdentifier));
  }

  @Override
  public void deleteByAccountIdentifier(Class<Webhook> clazz, String accountId) {
    hPersistence.delete(hPersistence.createQuery(Webhook.class).filter(WebhookKeys.accountId, accountId));
  }
}
