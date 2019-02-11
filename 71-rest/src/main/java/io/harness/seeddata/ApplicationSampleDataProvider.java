package io.harness.seeddata;

import static io.harness.seeddata.SampleDataProviderConstants.HARNESS_SAMPLE_APP;
import static io.harness.seeddata.SampleDataProviderConstants.HARNESS_SAMPLE_APP_DESC;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.Application;
import software.wings.service.intfc.AppService;

@Singleton
public class ApplicationSampleDataProvider {
  @Inject private AppService appService;

  public Application createKubernetesApp(String accountId) {
    Application application = Application.Builder.anApplication()
                                  .withName(HARNESS_SAMPLE_APP)
                                  .withDescription(HARNESS_SAMPLE_APP_DESC)
                                  .withAccountId(accountId)
                                  .build();

    return appService.save(application);
  }

  public Application createApp(String accountId, String name, String description) {
    Application application = Application.Builder.anApplication()
                                  .withName(name)
                                  .withDescription(description)
                                  .withAccountId(accountId)
                                  .build();

    return appService.save(application);
  }
}
