package io.harness.mongo.index;

import io.harness.mongo.CollationLocale;
import io.harness.mongo.CollationStrength;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Collation {
  CollationLocale locale;
  CollationStrength strength;
}
