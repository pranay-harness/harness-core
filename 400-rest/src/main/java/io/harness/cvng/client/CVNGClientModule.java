package io.harness.cvng.client;

import com.google.inject.AbstractModule;
import io.harness.security.ServiceTokenGenerator;

public class CVNGClientModule extends AbstractModule {
  String cvngBaseUrl;
  String cvNgServiceSecret;

  public CVNGClientModule(CVNGClientConfig cvngClientConfig) {
    this.cvngBaseUrl = cvngClientConfig.getBaseUrl() + (cvngClientConfig.getBaseUrl().endsWith("/") ? "api/" : "/api/");
    this.cvNgServiceSecret = cvngClientConfig.getCvNgServiceSecret();
  }

  @Override
  protected void configure() {
    ServiceTokenGenerator tokenGenerator = new ServiceTokenGenerator();
    bind(ServiceTokenGenerator.class).toInstance(tokenGenerator);
    bind(CVNGServiceClient.class)
        .toProvider(new CVNGServiceClientFactory(cvngBaseUrl, cvNgServiceSecret, tokenGenerator));
  }
}