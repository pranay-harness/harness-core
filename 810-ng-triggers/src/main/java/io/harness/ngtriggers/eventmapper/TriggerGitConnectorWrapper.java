package io.harness.ngtriggers.eventmapper;

import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.ngtriggers.beans.dto.TriggerDetails;

import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TriggerGitConnectorWrapper {
  ConnectorConfigDTO connectorConfigDTO;
  ConnectorType connectorType;
  GitConnectionType gitConnectionType;
  String url;
  List<TriggerDetails> triggers;
  String connectorFQN;
}
