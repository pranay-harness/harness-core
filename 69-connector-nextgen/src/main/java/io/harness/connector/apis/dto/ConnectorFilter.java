package io.harness.connector.apis.dto;

import io.harness.delegate.beans.connector.ConnectorType;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ConnectorFilter {
  String accountId;
  String projectId;
  String orgId;
  ConnectorType type;
  // todo: @deepak Add tags here
  List<String> tag;
  Long lastActivity;
  String name;
}
