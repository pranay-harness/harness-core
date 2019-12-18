package io.harness.delegate.service;

import static io.harness.network.SafeHttpCall.execute;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.managerclient.ManagerClientV2;
import software.wings.beans.ConfigFile;
import software.wings.delegatetasks.DelegateConfigService;

import java.io.IOException;
import java.util.List;

/**
 * Created by peeyushaggarwal on 1/10/17.
 */
@Singleton
public class DelegateConfigServiceImpl implements DelegateConfigService {
  @Inject private ManagerClientV2 managerClient;

  @Override
  public List<ConfigFile> getConfigFiles(String appId, String envId, String uuid, String hostId, String accountId)
      throws IOException {
    return execute(managerClient.getConfigFiles(uuid, accountId, appId, envId, hostId)).getResource();
  }
}
