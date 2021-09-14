/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.generator;

import software.wings.beans.AppContainer;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AppContainerService;

import com.google.inject.Inject;

/**
 * Created by sgurubelli on 9/10/18.
 */
public class AppContainerGenerator {
  @Inject AccountGenerator accountGenerator;

  @Inject AppContainerService appContainerService;
  @Inject WingsPersistence wingsPersistence;

  public AppContainer ensureAppContainer(AppContainer appContainer) {
    AppContainer existing = exists(appContainer);
    if (existing != null) {
      return existing;
    }

    return GeneratorUtils.suppressDuplicateException(
        () -> appContainerService.save(appContainer), () -> exists(appContainer));
  }

  public AppContainer exists(AppContainer appContainer) {
    return appContainerService.get(appContainer.getAccountId(), appContainer.getName());
  }
}
