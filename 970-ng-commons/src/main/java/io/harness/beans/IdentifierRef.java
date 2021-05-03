package io.harness.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.common.EntityReference;
import io.harness.encryption.Scope;
import io.harness.utils.FullyQualifiedIdentifierHelper;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.PIPELINE)
public class IdentifierRef implements EntityReference {
  Scope scope;
  String identifier;
  String accountIdentifier;
  String orgIdentifier;
  String projectIdentifier;
  Map<String, String> metadata;
  String repoIdentifier;
  String branch;
  Boolean isDefault;

  @Override
  public String getFullyQualifiedName() {
    return FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(
        accountIdentifier, orgIdentifier, projectIdentifier, identifier);
  }

  @Override
  public Boolean isDefault() {
    return isDefault;
  }
}
