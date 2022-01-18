/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.beans.connector.gcpkmsconnector;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DecryptableEntity;
import io.harness.connector.DelegateSelectable;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretReference;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Collections;
import java.util.List;
import java.util.Set;
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
@ToString(exclude = {"credentials"})
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "GcpKmsConnector", description = "This contains GCP KMS SecretManager configuration")
public class GcpKmsConnectorDTO extends ConnectorConfigDTO implements DelegateSelectable {
  private String projectId;
  private String region;
  private String keyRing;
  private String keyName;
  @SecretReference @ApiModelProperty(dataType = "string") @NotNull SecretRefData credentials;
  private boolean isDefault;
  @JsonIgnore private boolean harnessManaged;
  private Set<String> delegateSelectors;

  @Builder
  public GcpKmsConnectorDTO(String projectId, String region, String keyRing, String keyName, SecretRefData credentials,
      boolean isDefault, Set<String> delegateSelectors) {
    this.projectId = projectId;
    this.region = region;
    this.keyRing = keyRing;
    this.keyName = keyName;
    this.credentials = credentials;
    this.isDefault = isDefault;
    this.delegateSelectors = delegateSelectors;
  }

  @Override
  public List<DecryptableEntity> getDecryptableEntities() {
    return Collections.singletonList(this);
  }
}
