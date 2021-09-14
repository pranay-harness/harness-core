/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.expression;

import lombok.Builder;

@Builder
public class SecretString {
  public static final String SECRET_MASK = "**************";

  private String value;

  @Override
  public String toString() {
    return value;
  }
}
