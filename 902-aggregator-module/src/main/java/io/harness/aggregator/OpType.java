package io.harness.aggregator;

import java.util.Optional;

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
