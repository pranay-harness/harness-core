/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.beans.connector.azurekeyvaultconnector;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.azure.AzureEnvironmentType.AZURE;

import static com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.harness.annotations.dev.OwnedBy;
import io.harness.azure.AzureEnvironmentType;
import io.harness.beans.DecryptableEntity;
import io.harness.connector.DelegateSelectable;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretReference;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.base.Preconditions;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@OwnedBy(PL)
@Getter
@Setter
@Builder
@ToString(exclude = {"secretKey"})
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@Schema(name = "AzureKeyVaultConnector", description = "Returns Azure KeyVault Secret Manager configuration")
public class AzureKeyVaultConnectorDTO extends ConnectorConfigDTO implements DelegateSelectable {
  @NotNull private String clientId;
  @SecretReference @ApiModelProperty(dataType = "string") @NotNull private SecretRefData secretKey;
  @NotNull private String tenantId;
  @NotNull private String vaultName;
  @NotNull private String subscription;
  private boolean isDefault;

  @Builder.Default private AzureEnvironmentType azureEnvironmentType = AZURE;
  private Set<String> delegateSelectors;

  @Override
  public List<DecryptableEntity> getDecryptableEntities() {
    return Collections.singletonList(this);
  }

  @Override
  public void validate() {
    Preconditions.checkNotNull(this.clientId, "clientId cannot be empty");
    Preconditions.checkNotNull(this.tenantId, "tenantId cannot be empty");
    Preconditions.checkNotNull(this.vaultName, "vaultName cannot be empty");
    Preconditions.checkNotNull(this.subscription, "subscription cannot be empty");
  }
}
