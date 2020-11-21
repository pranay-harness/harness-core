package io.harness.connector.apis.dto;

import io.harness.delegate.beans.connector.ConnectorType;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ConnectorFilter {
  ConnectorType type;
  List<String> tag;
  Long lastActivity;
  String name;
}
