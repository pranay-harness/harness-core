package io.harness.delegate.app.modules;

import io.harness.delegate.configuration.DelegateConfiguration;
import io.harness.delegate.task.citasks.cik8handler.helper.DelegateServiceTokenHelper;
import io.harness.security.ServiceTokenGenerator;

import com.google.inject.AbstractModule;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DelegateTokensModule extends AbstractModule {
  private final DelegateConfiguration configuration;

  @Override
  protected void configure() {
    bind(DelegateServiceTokenHelper.class)
        .toInstance(DelegateServiceTokenHelper.builder()
                        .serviceTokenGenerator(new ServiceTokenGenerator())
                        .accountSecret(configuration.getAccountSecret())
                        .build());
  }
}
