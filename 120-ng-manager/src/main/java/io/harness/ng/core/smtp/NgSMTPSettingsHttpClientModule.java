package io.harness.ng.core.smtp;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;

@OwnedBy(PL)
public class NgSMTPSettingsHttpClientModule extends AbstractModule {
  private final ServiceHttpClientConfig managerClientConfig;
  private final String managerServiceSecret;

  public NgSMTPSettingsHttpClientModule(ServiceHttpClientConfig managerClientConfig, String managerServiceSecret) {
    this.managerClientConfig = managerClientConfig;
    this.managerServiceSecret = managerServiceSecret;
  }

  @Override
  protected void configure() {
    bind(NgSMTPSettingsHttpClient.class).toProvider(NgSMTPSettingsHttpClientFactory.class).in(Scopes.SINGLETON);
    bind(SmtpNgService.class).to(SmtpNgServiceImpl.class);
  }

  @Provides
  private NgSMTPSettingsHttpClientFactory getNgSMTPSettingsHttpClientFactory(
      KryoConverterFactory kryoConverterFactory) {
    return new NgSMTPSettingsHttpClientFactory(
        managerClientConfig, managerServiceSecret, new ServiceTokenGenerator(), kryoConverterFactory);
  }
}
