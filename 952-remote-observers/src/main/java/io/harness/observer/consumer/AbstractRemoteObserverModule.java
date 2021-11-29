package io.harness.observer.consumer;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.observer.RemoteObserver;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class AbstractRemoteObserverModule extends AbstractModule {
  @Override
  protected void configure() {
    if (noOpProducer()) {
      bind(RemoteObserverProcessor.class).to(NoOpRemoteObserverProcessorImpl.class);
    } else {
      bind(RemoteObserverProcessor.class).to(RemoteObserverProcessorImpl.class);
    }
  }

  public abstract boolean noOpProducer();

  public abstract Set<RemoteObserver> observers();

  @Provides
  @Singleton
  public Map<String, RemoteObserver> registerObservers() {
    final Set<RemoteObserver> observers = observers();
    Map<String, RemoteObserver> remoteObserverMap = new HashMap<>();
    if (isNotEmpty(observers)) {
      remoteObserverMap = observers.stream().collect(
          Collectors.toMap(observer -> observer.getSubjectCLass().getName(), Function.identity()));
    }
    return remoteObserverMap;
  }
}
