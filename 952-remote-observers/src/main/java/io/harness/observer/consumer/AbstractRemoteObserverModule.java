/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

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
