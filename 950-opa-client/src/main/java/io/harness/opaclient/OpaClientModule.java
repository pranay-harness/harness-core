package io.harness.opaclient;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.AbstractModule;

@OwnedBy(HarnessTeam.PIPELINE)
public class OpaClientModule extends AbstractModule {
  private String opaServiceBaseUrl;

  public OpaClientModule(String opaServiceBaseUrl) {
    this.opaServiceBaseUrl = opaServiceBaseUrl;
  }

  @Override
  public void configure() {
    bind(OpaServiceClient.class).toProvider(new OpaClientFactory(opaServiceBaseUrl));
  }
}
