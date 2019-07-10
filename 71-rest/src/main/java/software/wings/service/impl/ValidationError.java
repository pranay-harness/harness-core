package software.wings.service.impl;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.Collection;

@Value
@Builder
public class ValidationError {
  private String message;
  @Singular private Collection<String> restrictedFeatures;
}
