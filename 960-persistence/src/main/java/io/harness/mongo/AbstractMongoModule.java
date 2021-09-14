/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.mongo;

import io.harness.persistence.UserProvider;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractMongoModule extends AbstractModule {
  @Override
  protected void configure() {
    install(MongoModule.getInstance());
  }

  @Provides
  @Singleton
  protected UserProvider injectUserProvider() {
    return userProvider();
  };

  public abstract UserProvider userProvider();
}
