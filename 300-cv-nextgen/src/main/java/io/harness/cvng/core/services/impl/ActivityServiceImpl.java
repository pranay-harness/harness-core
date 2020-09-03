package io.harness.cvng.core.services.impl;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import io.harness.cvng.core.beans.ActivityDTO;
import io.harness.cvng.core.entities.Activity;
import io.harness.cvng.core.services.api.ActivityService;
import io.harness.cvng.core.services.api.WebhookService;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ActivityServiceImpl implements ActivityService {
  @Inject WebhookService webhookService;
  @Inject HPersistence hPersistence;

  @Override
  public void register(String accountId, String webhookToken, ActivityDTO activityDTO) {
    webhookService.validateWebhookToken(
        webhookToken, activityDTO.getProjectIdentifier(), activityDTO.getOrgIdentifier());
    Preconditions.checkNotNull(activityDTO);
    Activity activity = activityDTO.toEntity();
    logger.info("Registering a new activity of type {} for account {}", activity.getType(), accountId);
    hPersistence.save(activity);
  }
}
