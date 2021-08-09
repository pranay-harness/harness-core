package software.wings.resources;

import software.wings.beans.scim.ScimBaseResource;

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
