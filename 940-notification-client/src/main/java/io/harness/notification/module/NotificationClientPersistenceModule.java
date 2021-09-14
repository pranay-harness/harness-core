/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.notification.module;

import io.harness.notification.NotificationChannelPersistenceConfig;
import io.harness.springdata.PersistenceModule;

public class NotificationClientPersistenceModule extends PersistenceModule {
  @Override
  protected Class<?>[] getConfigClasses() {
    return new Class[] {NotificationChannelPersistenceConfig.class};
  }
}
