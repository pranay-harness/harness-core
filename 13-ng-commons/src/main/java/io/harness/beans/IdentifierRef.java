package io.harness.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.harness.common.EntityReference;
import io.harness.encryption.Scope;
import io.harness.serializer.JsonUtils;
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
  @JsonIgnore
  public String getFullyQualifiedName() {
    return FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(
        accountIdentifier, orgIdentifier, projectIdentifier, identifier);
  }

  public static IdentifierRef fromString(String stringValue) {
    return JsonUtils.asObject(stringValue, IdentifierRef.class);
  }
}
