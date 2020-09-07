package io.harness.delegate.service;

import static io.harness.network.SafeHttpCall.execute;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.managerclient.ManagerClient;
import software.wings.beans.ConfigFile;
import software.wings.delegatetasks.DelegateConfigService;

import java.io.IOException;
import java.util.List;

@Singleton
public class DelegateConfigServiceImpl implements DelegateConfigService {
  @Inject private ManagerClient managerClient;

  @Override
  public List<ConfigFile> getConfigFiles(String appId, String envId, String uuid, String hostId, String accountId)
      throws IOException {
    return execute(managerClient.getConfigFiles(uuid, accountId, appId, envId, hostId)).getResource();
  }
}
