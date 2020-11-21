package io.harness.beans;

import io.harness.common.EntityReference;
import io.harness.encryption.Scope;
import io.harness.utils.FullyQualifiedIdentifierHelper;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class IdentifierRef implements EntityReference {
  Scope scope;
  String identifier;
  String accountIdentifier;
  String orgIdentifier;
  String projectIdentifier;

  @Override
  public String getFullyQualifiedName() {
    return FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(
        accountIdentifier, orgIdentifier, projectIdentifier, identifier);
  }
}
