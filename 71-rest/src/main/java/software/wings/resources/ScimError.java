package software.wings.resources;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.scim.ScimBaseResource;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class ScimError extends ScimBaseResource {
  private String detail;
  private int status;
}
