package io.harness.service;

import software.wings.beans.Event.Type;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.service.intfc.yaml.YamlPushService;

public class NoOpYamlPushService implements YamlPushService {
  @Override
  public <T> void pushYamlChangeSet(
      String accountId, T oldEntity, T newEntity, Type type, boolean syncFromGit, boolean isRename) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <R, T> void pushYamlChangeSet(String accountId, R helperEntity, T entity, Type type, boolean syncFromGit) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void pushYamlChangeSet(String accountId, String appId, ChangeType changeType, boolean syncFromGit) {
    throw new UnsupportedOperationException();
  }
}
