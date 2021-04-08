package io.harness.connector.entities.embedded.awskmsconnector;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.Connector;
import io.harness.delegate.beans.connector.awskmsconnector.AwsKmsCredentialType;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.annotation.TypeAlias;

import static io.harness.annotations.dev.HarnessTeam.PL;

@OwnedBy(PL)
@Value
@Builder
@EqualsAndHashCode(callSuper = true)
@FieldNameConstants(innerTypeName = "AwsKmsConnectorKeys")
@Entity(value = "connectors", noClassnameStored = true)
@Persistent
@TypeAlias("io.harness.connector.entities.embedded.awskmsconnector.AwsKmsConnector")
public class AwsKmsConnector extends Connector {
  private String kmsArn;
  private String region;
  private boolean isDefault;
  AwsKmsCredentialType credentialType;
  AwsKmsCredentialSpec credentialSpec;
  @Builder.Default Boolean harnessManaged = Boolean.FALSE;
}
