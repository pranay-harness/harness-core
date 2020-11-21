package software.wings.service.impl;

import java.util.Collection;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
public class ValidationError {
  private String message;
  @Singular private Collection<String> restrictedFeatures;
}
