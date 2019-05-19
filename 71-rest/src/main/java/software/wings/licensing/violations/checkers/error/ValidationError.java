package software.wings.licensing.violations.checkers.error;

import lombok.Builder;
import lombok.Value;
import software.wings.licensing.violations.RestrictedFeature;

import java.util.Collection;

@Value
@Builder
public class ValidationError {
  private String message;
  private Collection<RestrictedFeature> restrictedFeatures;
}
