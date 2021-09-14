/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.services.api;

import io.harness.cvng.core.entities.DeletedCVConfig;

import javax.annotation.Nullable;

public interface DeletedCVConfigService {
  DeletedCVConfig save(DeletedCVConfig deletedCVConfig);
  @Nullable DeletedCVConfig get(String deletedCVConfigId);
  void triggerCleanup(DeletedCVConfig deletedCVConfig);
}
