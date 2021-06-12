package io.harness.account;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

public abstract class AbstractAccountModule extends AbstractModule {
  @Override
  protected void configure() {
    install(AccountModule.getInstance());
  }

  @Provides
  @Singleton
  protected AccountConfig injectAccountConfiguration() {
    return accountConfiguration();
  }

  public abstract AccountConfig accountConfiguration();
}
