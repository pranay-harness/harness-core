package io.harness.ccm.communication;

import static io.harness.persistence.HPersistence.upsertReturnNewOptions;
import static io.harness.persistence.HQuery.excludeValidate;

import io.harness.ccm.communication.entities.CESlackWebhook;
import io.harness.ccm.communication.entities.CESlackWebhook.CESlackWebhookKeys;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

public class CESlackWebhookDao {
  @Inject private HPersistence persistence;

  public CESlackWebhook upsert(CESlackWebhook slackWebhook) {
    Query<CESlackWebhook> query =
        persistence.createQuery(CESlackWebhook.class).filter(CESlackWebhookKeys.accountId, slackWebhook.getAccountId());
    UpdateOperations<CESlackWebhook> updateOperations = persistence.createUpdateOperations(CESlackWebhook.class);
    if (null != slackWebhook.getWebhookUrl()) {
      updateOperations.set(CESlackWebhookKeys.webhookUrl, slackWebhook.getWebhookUrl());
      updateOperations.set(CESlackWebhookKeys.sendCostReport, slackWebhook.isSendCostReport());
      updateOperations.set(CESlackWebhookKeys.sendAnomalyAlerts, slackWebhook.isSendAnomalyAlerts());
    }
    return persistence.upsert(query, updateOperations, upsertReturnNewOptions);
  }

  public CESlackWebhook getByAccountId(String accountId) {
    return persistence.createQuery(CESlackWebhook.class, excludeValidate)
        .filter(CESlackWebhookKeys.accountId, accountId)
        .get();
  }
}
