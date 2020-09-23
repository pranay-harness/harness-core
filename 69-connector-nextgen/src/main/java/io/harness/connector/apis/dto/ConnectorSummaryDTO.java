package io.harness.connector.apis.dto;

import io.harness.connector.entities.ConnectorConnectivityDetails;
import io.harness.delegate.beans.connector.ConnectorCategory;
import io.harness.delegate.beans.connector.ConnectorType;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ConnectorSummaryDTO {
  String identifier;
  String name;
  String description;

  String accountIdentifier;
  String orgIdentifier;
  String projectIdentifier;

  String accountName;
  String orgName;
  String projectName;

  ConnectorType type;
  List<ConnectorCategory> categories;
  ConnectorConfigSummaryDTO connectorDetails;

  List<String> tags;

  Long createdAt;
  Long lastModifiedAt;
  Long version;

  ConnectorConnectivityDetails status;
}