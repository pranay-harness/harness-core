package software.wings.delegate.service;

import software.wings.beans.Log;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.managerclient.ManagerClient;

import java.io.IOException;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by peeyushaggarwal on 1/9/17.
 */
@Singleton
public class DelegateLogServiceImpl implements DelegateLogService {
  @Inject private ManagerClient managerClient;

  @Override
  public void save(String accountId, Log log) {
    try {
      managerClient.saveLog(accountId, log).execute();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
