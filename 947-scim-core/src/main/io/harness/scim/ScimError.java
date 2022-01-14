package io.harness.scim;

import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class ScimError extends ScimBaseResource {
  private String detail;
  private int status;
  private Set<String> schemas;
}
