/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.services.api;

import io.harness.cvng.core.entities.DeletedCVConfig;

import java.time.Duration;
import javax.annotation.Nullable;

public interface DeletedCVConfigService {
  DeletedCVConfig save(DeletedCVConfig deletedCVConfig, Duration toDeleteAfterDuration);
  @Nullable DeletedCVConfig get(String deletedCVConfigId);
  void triggerCleanup(DeletedCVConfig deletedCVConfig);
}
