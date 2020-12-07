package io.harness.utils;

import io.harness.pms.sdk.core.data.Outcome;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@Data
@AllArgsConstructor
@TypeAlias("dummyOutcome25")
public class DummyOutcome implements Outcome {
  String name;
}
