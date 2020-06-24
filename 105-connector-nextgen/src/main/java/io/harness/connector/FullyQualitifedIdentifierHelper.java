package io.harness.connector;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import lombok.experimental.UtilityClass;
import org.apache.http.util.Asserts;

@UtilityClass
public class FullyQualitifedIdentifierHelper {
  public String getFullyQualifiedIdentifier(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier) {
    if (isNotBlank(projectIdentifier)) {
      Asserts.notEmpty(projectIdentifier, "Project Identifier");
      Asserts.notEmpty(accountIdentifier, "Account Identifier");
      return String.format("%s/%s/%s/%s", accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier);
    } else if (isNotBlank(orgIdentifier)) {
      Asserts.notEmpty(accountIdentifier, "Account Identifier");
      return String.format("%s/%s/%s", accountIdentifier, orgIdentifier, connectorIdentifier);
    } else if (isNotBlank(accountIdentifier)) {
      Asserts.notEmpty(accountIdentifier, "Account Identifier");
      return String.format("%s/%s", accountIdentifier, connectorIdentifier);
    }
    return connectorIdentifier;
  }
}
