/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
