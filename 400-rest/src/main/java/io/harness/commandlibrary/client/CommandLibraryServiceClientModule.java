/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.commandlibrary.client;

import io.harness.commandlibrary.CommandLibraryServiceConfig;
import io.harness.security.ServiceTokenGenerator;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class CommandLibraryServiceClientModule extends AbstractModule {
  private final CommandLibraryServiceConfig commandLibraryServiceConfig;

  public CommandLibraryServiceClientModule(CommandLibraryServiceConfig commandLibraryServiceConfig) {
    this.commandLibraryServiceConfig = commandLibraryServiceConfig;
  }

  @Override
  protected void configure() {
    final ServiceTokenGenerator tokenGenerator = new ServiceTokenGenerator();
    bind(CommandLibraryServiceHttpClient.class)
        .toProvider(new CommandLibraryServiceHttpClientFactory(commandLibraryServiceConfig.getBaseUrl(), tokenGenerator,
            commandLibraryServiceConfig.isPublishingAllowed(), commandLibraryServiceConfig.getPublishingSecret()))
        .in(Scopes.SINGLETON);
  }
}
