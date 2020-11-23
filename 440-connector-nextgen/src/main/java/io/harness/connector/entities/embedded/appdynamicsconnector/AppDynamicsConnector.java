package io.harness.connector.entities.embedded.appdynamicsconnector;

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
@FieldNameConstants(innerTypeName = "AppDynamicsConnectorKeys")
@Entity(value = "connectors", noClassnameStored = true)
@Persistent
@EqualsAndHashCode(callSuper = true)
@TypeAlias("io.harness.connector.entities.embedded.appdynamicsconnector.AppDynamicsConnector")
public class AppDynamicsConnector extends Connector {
  private String username;
  private String accountname;
  private String passwordRef;
  private String controllerUrl;
  private String accountId;
}
