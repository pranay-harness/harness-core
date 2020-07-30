package io.harness.connector.entities.embedded.appdynamicsconnector;

import io.harness.connector.entities.Connector;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@FieldNameConstants(innerTypeName = "AppDynamicsConfigKeys")
@Entity(value = "connectors", noClassnameStored = true)
@Persistent
@TypeAlias("io.harness.connector.entities.embedded.appdynamicsconnector.AppDynamicsConfig")
public class AppDynamicsConfig extends Connector {
  private String username;
  private String accountname;
  private char[] password;
  private String passwordReference;
  private String controllerUrl;
  private String accountId;
}
