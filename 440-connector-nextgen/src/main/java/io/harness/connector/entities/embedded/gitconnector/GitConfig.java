package io.harness.connector.entities.embedded.gitconnector;

import io.harness.connector.entities.Connector;
import io.harness.delegate.beans.connector.gitconnector.CustomCommitAttributes;
import io.harness.delegate.beans.connector.gitconnector.GitAuthType;
import io.harness.delegate.beans.connector.gitconnector.GitConnectionType;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@FieldNameConstants(innerTypeName = "GitConfigKeys")
@Entity(value = "connectors", noClassnameStored = true)
@TypeAlias("io.harness.connector.entities.embedded.gitconnector.GitConfig")
@Persistent
public class GitConfig extends Connector {
  GitConnectionType connectionType;
  String url;
  String branchName;
  GitAuthentication authenticationDetails;
  GitAuthType authType;
  CustomCommitAttributes customCommitAttributes;
  boolean supportsGitSync;
}
