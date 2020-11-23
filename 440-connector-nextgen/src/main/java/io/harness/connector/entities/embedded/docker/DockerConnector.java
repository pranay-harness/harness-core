package io.harness.connector.entities.embedded.docker;

import io.harness.connector.entities.Connector;
import io.harness.delegate.beans.connector.docker.DockerAuthType;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@FieldNameConstants(innerTypeName = "DockerConnectorKeys")
@Entity(value = "connectors", noClassnameStored = true)
@Persistent
@FieldDefaults(level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = true)
@TypeAlias("io.harness.connector.entities.embedded.docker.DockerConnector")
public class DockerConnector extends Connector {
  String url;
  DockerAuthType authType;
  DockerAuthentication dockerAuthentication;
}
