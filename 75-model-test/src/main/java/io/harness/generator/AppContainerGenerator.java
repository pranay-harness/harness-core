package io.harness.generator;

import com.google.inject.Inject;

import software.wings.beans.AppContainer;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AppContainerService;

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

    return appContainerService.save(appContainer);
  }

  public AppContainer exists(AppContainer appContainer) {
    return appContainerService.get(appContainer.getAccountId(), appContainer.getName());
  }
}
