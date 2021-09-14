/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.seeddata;

import static io.harness.seeddata.SampleDataProviderConstants.HARNESS_SAMPLE_APP;
import static io.harness.seeddata.SampleDataProviderConstants.HARNESS_SAMPLE_APP_DESC;

import software.wings.beans.Application;
import software.wings.service.intfc.AppService;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ApplicationSampleDataProvider {
  @Inject private AppService appService;

  public Application createKubernetesApp(String accountId) {
    Application application = Application.Builder.anApplication()
                                  .name(HARNESS_SAMPLE_APP)
                                  .description(HARNESS_SAMPLE_APP_DESC)
                                  .accountId(accountId)
                                  .sample(true)
                                  .build();

    return appService.save(application);
  }

  public Application createApp(String accountId, String name, String description) {
    Application application = Application.Builder.anApplication()
                                  .name(name)
                                  .description(description)
                                  .accountId(accountId)
                                  .sample(true)
                                  .build();

    return appService.save(application);
  }
}
