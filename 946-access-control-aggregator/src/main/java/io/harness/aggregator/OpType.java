/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.aggregator;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.Optional;

@OwnedBy(HarnessTeam.PL)
public enum OpType {
  CREATE("c"),
  UPDATE("u"),
  DELETE("d"),
  SNAPSHOT("r");

  String op;

  OpType(String op) {
    this.op = op;
  }

  public static Optional<OpType> fromString(String opStr) {
    for (OpType opType : values()) {
      if (opType.op.equals(opStr)) {
        return Optional.of(opType);
      }
    }
    return Optional.empty();
  }
}
