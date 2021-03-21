package io.harness.connector.entities.embedded.localconnector;

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
@FieldNameConstants(innerTypeName = "LocalConnectorKeys")
@Entity(value = "connectors", noClassnameStored = true)
@Persistent
@TypeAlias("io.harness.connector.entities.embedded.localconnector.LocalConnector")
@OwnedBy(DX)
public class LocalConnector extends Connector {
  boolean isDefault;
  @Builder.Default Boolean harnessManaged = Boolean.TRUE;
}
