/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.beans;

public enum SecretManagerCapabilities {
  CREATE_INLINE_SECRET,
  CREATE_REFERENCE_SECRET,
  CREATE_PARAMETERIZED_SECRET,
  CREATE_FILE_SECRET,
  TRANSITION_SECRET_TO_SM,
  TRANSITION_SECRET_FROM_SM,
  CAN_BE_DEFAULT_SM;
}
