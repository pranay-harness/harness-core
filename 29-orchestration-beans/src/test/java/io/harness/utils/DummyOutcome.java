package io.harness.utils;

import io.harness.data.Outcome;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DummyOutcome implements Outcome {
  String name;
}
