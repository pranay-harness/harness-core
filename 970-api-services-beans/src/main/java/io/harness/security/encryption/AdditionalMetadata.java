package io.harness.security.encryption;

import java.util.Map;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
@EqualsAndHashCode
public class AdditionalMetadata {
  @Singular private Map<String, Object> values;
}
