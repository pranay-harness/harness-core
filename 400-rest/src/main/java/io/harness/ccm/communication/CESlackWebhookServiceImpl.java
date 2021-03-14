package io.harness.ccm.communication;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.ccm.communication.entities.CESlackWebhook;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@TargetModule(Module._490_CE_COMMONS)
public class CESlackWebhookServiceImpl implements CESlackWebhookService {
  @Inject private CESlackWebhookDao ceSlackWebhookDao;

  public CESlackWebhook upsert(CESlackWebhook slackWebhook) {
    return ceSlackWebhookDao.upsert(slackWebhook);
  }

  public CESlackWebhook getByAccountId(String accountId) {
    return ceSlackWebhookDao.getByAccountId(accountId);
  }
}
