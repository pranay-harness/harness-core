package io.harness.secretmanagerclient.dto.awssecretmanager;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.awssecretmanager.AwsSecretManagerCredentialType;

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
@FieldNameConstants(innerTypeName = "BaseAwsSMConfigDTOKeys")
@JsonIgnoreProperties(ignoreUnknown = true)
public class BaseAwsSMConfigDTO {
  AwsSecretManagerCredentialType credentialType;
  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "credentialType", include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
      visible = true)
  AwsSMCredentialSpecConfig credential;
  String region;
  String secretNamePrefix;
  String secretPrefixName;
  Set<String> delegateSelectors;
}