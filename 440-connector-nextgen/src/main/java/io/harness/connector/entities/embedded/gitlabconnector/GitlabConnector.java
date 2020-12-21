package io.harness.connector.entities.embedded.gitlabconnector;

import io.harness.connector.entities.Connector;
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.GitConnectionType;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@FieldNameConstants(innerTypeName = "GitlabConnectorKeys")
@Entity(value = "connectors", noClassnameStored = true)
@EqualsAndHashCode(callSuper = true)
@TypeAlias("io.harness.connector.entities.embedded.gitlabconnector.GitlabConnector")
@Persistent
public class GitlabConnector extends Connector {
  GitConnectionType connectionType;
  String url;
  GitAuthType authType;
  GitlabAuthentication authenticationDetails;
  boolean hasApiAccess;
  GitlabTokenApiAccess gitlabApiAccess;
}
