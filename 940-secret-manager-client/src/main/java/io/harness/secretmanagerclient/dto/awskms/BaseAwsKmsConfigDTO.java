package io.harness.secretmanagerclient.dto.awskms;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.awskmsconnector.AwsKmsConnectorCredentialDTO;
import io.harness.delegate.beans.connector.awskmsconnector.AwsKmsCredentialType;
import lombok.*;
import lombok.experimental.SuperBuilder;

import static io.harness.annotations.dev.HarnessTeam.PL;

@OwnedBy(PL)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@ToString
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class BaseAwsKmsConfigDTO {
  AwsKmsCredentialType credentialType;
  AwsKmsCredentialSpecConfig credential;
  String name;
  String kmsArn;
  String region;
}
