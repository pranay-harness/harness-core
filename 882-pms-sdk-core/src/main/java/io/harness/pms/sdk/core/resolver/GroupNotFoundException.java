/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.pms.sdk.core.resolver;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.eraro.ErrorCode.ENGINE_SWEEPING_OUTPUT_EXCEPTION;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.Level;
import io.harness.exception.WingsException;

@OwnedBy(CDC)
public class GroupNotFoundException extends WingsException {
  private static final String DETAILS_KEY = "details";

  @SuppressWarnings("squid:CallToDeprecatedMethod")
  public GroupNotFoundException(String groupName) {
    super(groupName, null, ENGINE_SWEEPING_OUTPUT_EXCEPTION, Level.ERROR, null, null);
    super.param(DETAILS_KEY, format("Group not found: %s", groupName));
  }
}
