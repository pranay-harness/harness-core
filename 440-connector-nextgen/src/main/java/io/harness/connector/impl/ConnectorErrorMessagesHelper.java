package io.harness.connector.impl;

import static io.harness.annotations.dev.HarnessTeam.DX;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Singleton;

@Singleton
@OwnedBy(DX)
public class ConnectorErrorMessagesHelper {
  public String createConnectorNotFoundMessage(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier) {
    StringBuilder stringBuilder = new StringBuilder(256);
    stringBuilder.append("No connector exists with the identifier ")
        .append(connectorIdentifier)
        .append(" in account ")
        .append(accountIdentifier);
    if (isNotBlank(orgIdentifier)) {
      stringBuilder.append(", organisation ").append(orgIdentifier);
    }
    if (isNotBlank(projectIdentifier)) {
      stringBuilder.append(", project ").append(projectIdentifier);
    }
    return stringBuilder.toString();
  }
}
