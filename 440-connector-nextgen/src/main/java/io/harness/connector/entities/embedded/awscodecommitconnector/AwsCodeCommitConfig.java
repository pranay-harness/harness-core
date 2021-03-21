package io.harness.connector.entities.embedded.awscodecommitconnector;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.Connector;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitUrlType;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@FieldNameConstants(innerTypeName = "AwsCodeCommitConfigKeys")
@EqualsAndHashCode(callSuper = false)
@Entity(value = "connectors", noClassnameStored = true)
@Persistent
@TypeAlias("io.harness.connector.entities.embedded.awscodecommitconnector.AwsCodeCommitConfig")
@OwnedBy(DX)
public class AwsCodeCommitConfig extends Connector {
  AwsCodeCommitUrlType urlType;
  String url;
  AwsCodeCommitAuthentication authentication;
}
