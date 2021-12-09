package io.harness.observer.consumer;

import io.harness.observer.RemoteObserver;
import io.harness.observer.RemoteObserverInformer;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import java.util.Set;

public abstract class AbstractRemoteObserverModule extends AbstractModule {
  @Override
  protected void configure() {
    if (noOpProducer()) {
      bind(RemoteObserverProcessor.class).to(NoOpRemoteObserverProcessorImpl.class);
    } else {
      bind(RemoteObserverProcessor.class).to(RemoteObserverProcessorImpl.class);
    }
    bind(RemoteObserverInformer.class).to(getRemoteObserverImpl());
  }

  public abstract boolean noOpProducer();

  public abstract Set<RemoteObserver> observers();

  @Provides
  @Singleton
  public Set<RemoteObserver> registerObservers() {
    return observers();
  }

  public abstract Class<? extends RemoteObserverInformer> getRemoteObserverImpl();
}
