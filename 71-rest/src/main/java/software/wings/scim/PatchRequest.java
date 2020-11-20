package software.wings.scim;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import software.wings.beans.scim.ScimBaseResource;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Getter
@Setter
public final class PatchRequest extends ScimBaseResource {
  @JsonProperty(value = "Operations", required = true) private List<PatchOperation> operations;
  @JsonProperty(value = "schemas", required = true) private Set<String> schemas;

  @JsonCreator
  public PatchRequest(@JsonProperty(value = "Operations", required = true) final List<PatchOperation> operations) {
    this.operations = Collections.unmodifiableList(operations);
  }
}
