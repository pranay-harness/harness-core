package io.harness.connector.entities.embedded.newrelicconnector;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.Connector;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@EqualsAndHashCode(callSuper = true)
@FieldNameConstants(innerTypeName = "NewRelicConnectorKeys")
@Entity(value = "connectors", noClassnameStored = true)
@Persistent
@TypeAlias("io.harness.connector.entities.embedded.newrelicconnector.NewRelicConnector")
@OwnedBy(DX)
public class NewRelicConnector extends Connector {
  private String url;
  private String apiKeyRef;
  private String newRelicAccountId;
}
