package io.harness.accesscontrol.scopes.core;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

@Value
@Builder
public class Scope {
  public static final String PATH_DELIMITER = "/";

  @NotNull ScopeLevel level;
  @NotEmpty String instanceId;
  Scope parentScope;

  @Override
  public String toString() {
    String identifier = PATH_DELIMITER + level.getResourceType() + PATH_DELIMITER + instanceId;
    if (parentScope != null) {
      return parentScope.toString().concat(identifier);
    }
    return identifier;
  }
}
