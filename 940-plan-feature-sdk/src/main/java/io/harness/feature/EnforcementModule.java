/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.feature;

import io.harness.feature.annotation.FeatureCheck;
import io.harness.feature.annotation.interceptor.FeatureCheckInterceptor;
import io.harness.feature.cache.LicenseInfoCache;
import io.harness.feature.cache.impl.LicenseInfoCacheImpl;
import io.harness.feature.cache.impl.LicenseInfoLoader;
import io.harness.feature.handlers.RestrictionHandlerFactory;
import io.harness.feature.services.FeatureLoader;
import io.harness.feature.services.FeatureService;
import io.harness.feature.services.impl.FeatureLoaderImpl;
import io.harness.feature.services.impl.FeatureServiceImpl;
import io.harness.govern.ProviderMethodInterceptor;
import io.harness.lock.PersistentLockModule;

import com.google.inject.AbstractModule;
import com.google.inject.matcher.Matchers;

public class EnforcementModule extends AbstractModule {
  private static EnforcementModule instance;

  private EnforcementModule() {}

  public static EnforcementModule getInstance() {
    if (instance == null) {
      instance = new EnforcementModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    install(PersistentLockModule.getInstance());

    bind(FeatureService.class).to(FeatureServiceImpl.class);
    bind(FeatureLoader.class).to(FeatureLoaderImpl.class);
    bind(LicenseInfoCache.class).to(LicenseInfoCacheImpl.class);
    bind(LicenseInfoLoader.class);
    bind(RestrictionHandlerFactory.class);

    ProviderMethodInterceptor featureCheck = new ProviderMethodInterceptor(getProvider(FeatureCheckInterceptor.class));
    bindInterceptor(Matchers.any(), Matchers.annotatedWith(FeatureCheck.class), featureCheck);
  }
}
