package io.harness.secretmanagerclient.dto.awskms;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.awskmsconnector.AwsKmsCredentialType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;

@OwnedBy(PL)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@ToString
@EqualsAndHashCode
@FieldNameConstants(innerTypeName = "BaseAwsKmsConfigDTOKeys")
@JsonIgnoreProperties(ignoreUnknown = true)
public class BaseAwsKmsConfigDTO {
  AwsKmsCredentialType credentialType;
  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "credentialType", include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
      visible = true)
  AwsKmsCredentialSpecConfig credential;
  String kmsArn;
  String region;
  Set<String> delegateSelectors;
}
