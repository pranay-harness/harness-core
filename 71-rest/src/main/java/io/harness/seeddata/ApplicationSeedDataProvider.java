package io.harness.seeddata;

import static io.harness.seeddata.SeedDataProviderConstants.KUBERNETES_APP_DESC;
import static io.harness.seeddata.SeedDataProviderConstants.KUBERNETES_APP_NAME;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.Application;
import software.wings.service.intfc.AppService;

@Singleton
public class ApplicationSeedDataProvider {
  @Inject private AppService appService;

  public Application createKubernetesApp(String accountId) {
    Application application = Application.Builder.anApplication()
                                  .withName(KUBERNETES_APP_NAME)
                                  .withDescription(KUBERNETES_APP_DESC)
                                  .withAccountId(accountId)
                                  .build();

    return appService.save(application);
  }
}
