package io.harness.utils;

import static io.harness.data.structure.HasPredicate.hasNone;
import static io.harness.data.structure.HasPredicate.hasSome;

import io.harness.exception.InvalidRequestException;

import lombok.experimental.UtilityClass;
import org.apache.http.util.Asserts;

@UtilityClass
public class FullyQualifiedIdentifierHelper {
  public String getFullyQualifiedIdentifier(
      String accountId, String orgIdentifier, String projectIdentifier, String identifier) {
    if (hasNone(identifier)) {
      throw new InvalidRequestException("Invalid Request Exception: No identifier provided.");
    }
    if (hasSome(projectIdentifier)) {
      Asserts.notEmpty(orgIdentifier, "Org Identifier");
      Asserts.notEmpty(accountId, "Account Identifier");
      return String.format("%s/%s/%s/%s", accountId, orgIdentifier, projectIdentifier, identifier);
    } else if (hasSome(orgIdentifier)) {
      Asserts.notEmpty(accountId, "Account Identifier");
      return String.format("%s/%s/%s", accountId, orgIdentifier, identifier);
    } else if (hasSome(accountId)) {
      return String.format("%s/%s", accountId, identifier);
    }
    throw new InvalidRequestException("Invalid Request Exception: No account ID provided.");
  }
}
