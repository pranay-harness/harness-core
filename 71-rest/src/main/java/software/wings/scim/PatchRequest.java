package software.wings.scim;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.unboundid.scim2.common.BaseScimResource;
import com.unboundid.scim2.common.annotations.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.List;

@Schema(id = "urn:ietf:params:scim:api:messages:2.0:PatchOp", name = "Patch Operation",
    description = "SCIM 2.0 Patch Operation Request")
@Getter
@Setter
public final class PatchRequest extends BaseScimResource {
  @JsonProperty(value = "Operations", required = true) private final List<PatchOperation> operations;

  @JsonCreator
  public PatchRequest(@JsonProperty(value = "Operations", required = true) final List<PatchOperation> operations) {
    this.operations = Collections.unmodifiableList(operations);
  }
}