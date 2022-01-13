/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.registries.exceptions;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.HarnessStringUtils;
import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;
import io.harness.exception.WingsException;

@OwnedBy(CDC)
public class DuplicateRegistryException extends WingsException {
  private static final String DETAILS_KEY = "details";

  public DuplicateRegistryException(String registryType, String message) {
    super(message, null, ErrorCode.REGISTRY_EXCEPTION, Level.ERROR, null, null);
    super.param(DETAILS_KEY, HarnessStringUtils.join("", "[RegistryType: ", registryType, "]", message));
  }
}
