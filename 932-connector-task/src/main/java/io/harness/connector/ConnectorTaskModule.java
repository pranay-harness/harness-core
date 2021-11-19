package io.harness.connector;

import io.harness.connector.helper.DecryptionHelper;
import io.harness.connector.helper.DecryptionHelperViaManager;

import com.google.inject.AbstractModule;
import java.util.concurrent.atomic.AtomicReference;

public class ConnectorTaskModule extends AbstractModule {
  private static volatile ConnectorTaskModule instance;

  private static final AtomicReference<ConnectorTaskModule> instanceRef = new AtomicReference();

  public ConnectorTaskModule() {}

  @Override
  protected void configure() {
    bind(DecryptionHelper.class).to(DecryptionHelperViaManager.class);
  }

  public static ConnectorTaskModule getInstance() {
    if (instanceRef.get() == null) {
      instanceRef.compareAndSet(null, new ConnectorTaskModule());
    }
    return instanceRef.get();
  }
}