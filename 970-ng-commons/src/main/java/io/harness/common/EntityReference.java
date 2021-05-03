package io.harness.common;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.beans.InputSetReference;
import io.harness.ng.core.NGAccess;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModel;

@OwnedBy(HarnessTeam.PIPELINE)
@ApiModel(value = "EntityReference", subTypes = {IdentifierRef.class, InputSetReference.class}, discriminator = "type")
public interface EntityReference extends NGAccess {
  @JsonIgnore String getFullyQualifiedName();
  String getBranch();
  String getRepoIdentifier();
  Boolean isDefault();
}