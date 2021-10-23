package io.harness.connector;

import io.harness.connector.service.git.NGGitService;
import io.harness.connector.service.git.NGGitServiceImpl;

import com.google.inject.AbstractModule;
import java.util.concurrent.atomic.AtomicReference;

public class ConnectorTaskModule extends AbstractModule {
  private static volatile ConnectorTaskModule instance;

  private static final AtomicReference<ConnectorTaskModule> instanceRef = new AtomicReference();

  public ConnectorTaskModule() {}

  @Override
  protected void configure() {
    bind(NGGitService.class).to(NGGitServiceImpl.class);
  }

  public static ConnectorTaskModule getInstance() {
    if (instanceRef.get() == null) {
      instanceRef.compareAndSet(null, new ConnectorTaskModule());
    }
    return instanceRef.get();
  }
}