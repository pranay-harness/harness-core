package io.harness.connector.entities.embedded.gcpkmsconnector;

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
@FieldNameConstants(innerTypeName = "GcpKmsConnectorKeys")
@Entity(value = "connectors", noClassnameStored = true)
@Persistent
@TypeAlias("io.harness.connector.entities.embedded.gcpkmsconnector.GcpKmsConnector")
public class GcpKmsConnector extends Connector {
  String projectId;
  String region;
  String keyRing;
  String keyName;
  boolean isDefault;
  @Builder.Default Boolean harnessManaged = Boolean.FALSE;
}
