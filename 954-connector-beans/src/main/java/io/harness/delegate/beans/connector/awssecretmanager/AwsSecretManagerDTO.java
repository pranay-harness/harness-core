/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.beans.connector.awssecretmanager;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.delegate.beans.connector.awssecretmanager.AwsSecretManagerCredentialType.ASSUME_IAM_ROLE;
import static io.harness.delegate.beans.connector.awssecretmanager.AwsSecretManagerCredentialType.ASSUME_STS_ROLE;
import static io.harness.eraro.ErrorCode.INVALID_REQUEST;
import static io.harness.exception.WingsException.USER;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DecryptableEntity;
import io.harness.connector.DelegateSelectable;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.exception.InvalidRequestException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@OwnedBy(PL)
@Getter
@Setter
@NoArgsConstructor
@ToString
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "AwsSecretManager", description = "Returns AWS Secret Manager configuration details")
public class AwsSecretManagerDTO extends ConnectorConfigDTO implements DelegateSelectable {
  @Valid @NotNull AwsSecretManagerCredentialDTO credential;
  @NotNull private String region;
  private boolean isDefault;
  @JsonIgnore private boolean harnessManaged;
  private String secretNamePrefix;
  private Set<String> delegateSelectors;

  @Builder
  public AwsSecretManagerDTO(String region, AwsSecretManagerCredentialDTO credential, boolean isDefault,
      String secretNamePrefix, Set<String> delegateSelectors) {
    this.region = region;
    this.credential = credential;
    this.isDefault = isDefault;
    this.secretNamePrefix = secretNamePrefix;
    this.delegateSelectors = delegateSelectors;
  }

  @Override
  public List<DecryptableEntity> getDecryptableEntities() {
    List<DecryptableEntity> decryptableEntities = new ArrayList<>();
    decryptableEntities.add(this);
    if (credential.getCredentialType() == AwsSecretManagerCredentialType.MANUAL_CONFIG) {
      AwsSMCredentialSpecManualConfigDTO awsKmsManualCredentials =
          (AwsSMCredentialSpecManualConfigDTO) credential.getConfig();
      decryptableEntities.add(awsKmsManualCredentials);
    }
    return decryptableEntities;
  }

  @Override
  public void validate() {
    AwsSecretManagerCredentialType credentialType = this.credential.getCredentialType();
    if (ASSUME_IAM_ROLE.equals(credentialType) || ASSUME_STS_ROLE.equals(credentialType)) {
      if (isEmpty(delegateSelectors)) {
        throw new InvalidRequestException(
            "Delegate Selectors are mandatory when credential type is AssumeRole", INVALID_REQUEST, USER);
      }
    }
  }
}
