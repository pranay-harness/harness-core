/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.managerclient;

import io.harness.security.TokenGenerator;

import com.google.inject.AbstractModule;

public class WatcherManagerClientModule extends AbstractModule {
  private String managerBaseUrl;
  private String accountId;
  private String accountSecret;

  public WatcherManagerClientModule(String managerBaseUrl, String accountId, String accountSecret) {
    this.managerBaseUrl = managerBaseUrl;
    this.accountId = accountId;
    this.accountSecret = accountSecret;
  }

  @Override
  protected void configure() {
    TokenGenerator tokenGenerator = new TokenGenerator(accountId, accountSecret);
    bind(TokenGenerator.class).toInstance(tokenGenerator);
    bind(ManagerClientV2.class).toProvider(new WatcherManagerClientV2Factory(managerBaseUrl, tokenGenerator));
  }
}
