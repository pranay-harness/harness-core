/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.service;

import io.harness.git.model.ChangeType;

import software.wings.beans.Event.Type;
import software.wings.service.intfc.yaml.YamlPushService;

import java.util.concurrent.Future;

public class NoOpYamlPushService implements YamlPushService {
  @Override
  public <T> Future<?> pushYamlChangeSet(
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
