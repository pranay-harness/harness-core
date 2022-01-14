package io.harness.connector.utils;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
@OwnedBy(HarnessTeam.CV)
public class FieldValues {
  @Singular Map<String, List<String>> fieldValues;
}