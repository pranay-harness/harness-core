/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
