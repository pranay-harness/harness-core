/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.services.impl;

import io.harness.cvng.core.entities.WebhookToken;
import io.harness.cvng.core.entities.WebhookToken.WebhookTokenKeys;
import io.harness.cvng.core.services.api.WebhookService;
import io.harness.cvng.exception.CVWebhookException;
import io.harness.persistence.HPersistence;
import io.harness.utils.CryptoUtils;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;

@Slf4j
public class WebhookServiceImpl implements WebhookService {
  @Inject HPersistence hPersistence;

  @Override
  public boolean validateWebhookToken(String webhookToken, String projectIdentifier, String orgIdentifier) {
    WebhookToken token = hPersistence.createQuery(WebhookToken.class)
                             .filter(WebhookTokenKeys.projectIdentifier, projectIdentifier)
                             .filter(WebhookTokenKeys.orgIdentifier, orgIdentifier)
                             .get();
    if (token != null && token.getToken().equals(webhookToken)) {
      return true;
    }
    return false;
  }

  @Override
  public String createWebhookToken(String projectIdentifier, String orgIdentifier) {
    // check to see if a token already exists
    WebhookToken token = hPersistence.createQuery(WebhookToken.class)
                             .filter(WebhookTokenKeys.projectIdentifier, projectIdentifier)
                             .filter(WebhookTokenKeys.orgIdentifier, orgIdentifier)
                             .get();
    if (token != null) {
      log.info("Token already present for project {} and org {}", projectIdentifier, orgIdentifier);
      return token.getToken();
    }
    token = WebhookToken.builder()
                .token(CryptoUtils.secureRandAlphaNumString(40))
                .orgIdentifier(orgIdentifier)
                .projectIdentifier(projectIdentifier)
                .build();
    hPersistence.save(token);
    log.info("Created a new Webhook token for project {} and org {}", projectIdentifier, orgIdentifier);
    return token.getToken();
  }

  @Override
  public String recreateWebhookToken(String projectIdentifier, String orgIdentifier) {
    Preconditions.checkNotNull(projectIdentifier);
    Preconditions.checkNotNull(orgIdentifier);

    log.info("Recreating a new webhook for project {} and org {}", projectIdentifier, orgIdentifier);
    String newToken = CryptoUtils.secureRandAlphaNumString(40);
    Query<WebhookToken> tokenQuery = hPersistence.createQuery(WebhookToken.class)
                                         .filter(WebhookTokenKeys.projectIdentifier, projectIdentifier)
                                         .filter(WebhookTokenKeys.orgIdentifier, orgIdentifier);
    UpdateOperations<WebhookToken> tokenUpdate =
        hPersistence.createUpdateOperations(WebhookToken.class).set(WebhookTokenKeys.token, newToken);
    UpdateResults updateResults = hPersistence.update(tokenQuery, tokenUpdate);
    if (updateResults.getUpdatedCount() == 1) {
      return newToken;
    }
    throw new CVWebhookException(
        "Trying to recreate a webhook without an existing one. Create one first before recreating");
  }

  @Override
  public void deleteWebhookToken(String projectIdentifier, String orgIdentifier) {
    Preconditions.checkNotNull(projectIdentifier);
    Preconditions.checkNotNull(orgIdentifier);

    log.info("Deleting the webhook for project {} and org {}", projectIdentifier, orgIdentifier);
    hPersistence.delete(hPersistence.createQuery(WebhookToken.class)
                            .filter(WebhookTokenKeys.projectIdentifier, projectIdentifier)
                            .filter(WebhookTokenKeys.orgIdentifier, orgIdentifier));
  }
}
