package io.harness.templates.google;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.templates.google.impl.GoogleCloudFileServiceImpl;

import com.google.inject.AbstractModule;

@OwnedBy(HarnessTeam.GTM)
public class GoogleCloudFileModule extends AbstractModule {
  private static GoogleCloudFileModule instance;

  private GoogleCloudFileModule() {}

  public static GoogleCloudFileModule getInstance() {
    if (instance == null) {
      instance = new GoogleCloudFileModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    bind(GoogleCloudFileService.class).to(GoogleCloudFileServiceImpl.class);
  }
}
