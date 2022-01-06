/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
