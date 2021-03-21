package io.harness.connector.entities.embedded.bitbucketconnector;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
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
@FieldNameConstants(innerTypeName = "BitbucketConnectorKeys")
@EqualsAndHashCode(callSuper = true)
@Entity(value = "connectors", noClassnameStored = true)
@TypeAlias("io.harness.connector.entities.embedded.bitbucketconnector.BitbucketConnector")
@Persistent
@OwnedBy(DX)
public class BitbucketConnector extends Connector {
  GitConnectionType connectionType;
  String url;
  GitAuthType authType;
  BitbucketAuthentication authenticationDetails;
  boolean hasApiAccess;
  BitbucketUsernamePasswordApiAccess bitbucketApiAccess;
}
