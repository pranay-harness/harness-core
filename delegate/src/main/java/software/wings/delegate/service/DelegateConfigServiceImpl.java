package software.wings.delegate.service;

import com.google.inject.Singleton;

import software.wings.beans.ConfigFile;
import software.wings.delegatetasks.DelegateConfigService;
import software.wings.managerclient.ManagerClient;

import java.io.IOException;
import java.util.List;
import javax.inject.Inject;

/**
 * Created by peeyushaggarwal on 1/10/17.
 */
@Singleton
public class DelegateConfigServiceImpl implements DelegateConfigService {
  @Inject private ManagerClient managerClient;

  @Override
  public List<ConfigFile> getConfigFiles(String appId, String envId, String uuid, String hostName, String accountId)
      throws IOException {
    return managerClient.getConfigFiles(uuid, accountId, appId, envId, hostName).execute().body().getResource();
  }
}
