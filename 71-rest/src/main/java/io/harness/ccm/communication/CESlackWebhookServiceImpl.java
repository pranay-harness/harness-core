package io.harness.ccm.communication;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.ccm.communication.entities.CESlackWebhook;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class CESlackWebhookServiceImpl implements CESlackWebhookService {
  @Inject private CESlackWebhookDao ceSlackWebhookDao;

  public CESlackWebhook upsert(CESlackWebhook slackWebhook) {
    return ceSlackWebhookDao.upsert(slackWebhook);
  }

  public CESlackWebhook getByAccountId(String accountId) {
    return ceSlackWebhookDao.getByAccountId(accountId);
  }
}
