package io.harness.connector.entities.embedded.splunkconnector;

import io.harness.connector.entities.Connector;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@FieldNameConstants(innerTypeName = "SplunkConnectorKeys")
@Entity(value = "connectors", noClassnameStored = true)
@Persistent
@TypeAlias("io.harness.connector.entities.embedded.splunkconnector.SplunkConnector")
public class SplunkConnector extends Connector {
  private String username;
  private String passwordRef;
  private String splunkUrl;
  private String accountId;
}
