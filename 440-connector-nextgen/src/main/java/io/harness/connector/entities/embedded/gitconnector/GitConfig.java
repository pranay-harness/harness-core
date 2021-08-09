package io.harness.connector.entities.embedded.gitconnector;

import io.harness.connector.entities.Connector;
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.genericgitconnector.CustomCommitAttributes;

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
@FieldNameConstants(innerTypeName = "GitConfigKeys")
@Entity(value = "connectors", noClassnameStored = true)
@TypeAlias("io.harness.connector.entities.embedded.gitconnector.GitConfig")
@Persistent
public class GitConfig extends Connector {
  GitConnectionType connectionType;
  String url;
  String validationRepo;
  String branchName;
  GitAuthentication authenticationDetails;
  GitAuthType authType;
  CustomCommitAttributes customCommitAttributes;
  boolean supportsGitSync;
}
