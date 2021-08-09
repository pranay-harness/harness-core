package io.harness.distribution.idempotence;

import lombok.AllArgsConstructor;
import lombok.Value;

/*
 * Extra strong typed idempotent id class.
 */

@Value
@AllArgsConstructor
public class IdempotentId {
  private String value;
}
