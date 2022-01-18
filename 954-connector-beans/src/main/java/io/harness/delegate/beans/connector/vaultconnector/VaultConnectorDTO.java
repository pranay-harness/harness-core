/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.beans.connector.vaultconnector;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.INVALID_REQUEST;
import static io.harness.exception.WingsException.USER;

import static com.fasterxml.jackson.annotation.JsonInclude.Include;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DecryptableEntity;
import io.harness.connector.DelegateSelectable;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretReference;
import io.harness.exception.InvalidRequestException;
import io.harness.security.encryption.AccessType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@OwnedBy(PL)
@Getter
@Setter
@Builder
@ToString(exclude = {"authToken", "secretId", "sinkPath"})
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@Schema(name = "VaultConnector", description = "This contains the Vault Connector configuration")
public class VaultConnectorDTO extends ConnectorConfigDTO implements DelegateSelectable {
  @SecretReference @ApiModelProperty(dataType = "string") private SecretRefData authToken;
  private String basePath;
  private String vaultUrl;
  private boolean isReadOnly;
  private long renewalIntervalMinutes;
  private boolean secretEngineManuallyConfigured;
  private String secretEngineName;
  private String appRoleId;
  @SecretReference @ApiModelProperty(dataType = "string") private SecretRefData secretId;
  private boolean isDefault;
  private int secretEngineVersion;
  private Set<String> delegateSelectors;
  private String namespace;
  private String sinkPath;
  private boolean useVaultAgent;

  public AccessType getAccessType() {
    if (useVaultAgent) {
      return AccessType.VAULT_AGENT;
    } else {
      return isNotEmpty(appRoleId) ? AccessType.APP_ROLE : AccessType.TOKEN;
    }
  }

  @Override
  public List<DecryptableEntity> getDecryptableEntities() {
    return Collections.singletonList(this);
  }

  @Override
  public void validate() {
    try {
      new URL(vaultUrl);
    } catch (MalformedURLException malformedURLException) {
      throw new InvalidRequestException("Please check the url and try again.", INVALID_REQUEST, USER);
    }
    if (secretEngineVersion <= 0) {
      throw new InvalidRequestException(
          String.format("Invalid value for secret engine version: %s", secretEngineVersion), INVALID_REQUEST, USER);
    }
    if (renewalIntervalMinutes <= 0) {
      throw new InvalidRequestException(
          String.format("Invalid value for renewal interval: %s", renewalIntervalMinutes), INVALID_REQUEST, USER);
    }
    if (isReadOnly && isDefault) {
      throw new InvalidRequestException("Read only secret manager cannot be set as default", INVALID_REQUEST, USER);
    }
    if (isUseVaultAgent()) {
      if (isBlank(getSinkPath())) {
        throw new InvalidRequestException(
            "You must provide a sink path to read token if you are using VaultAgent", INVALID_REQUEST, USER);
      }
      if (isEmpty(getDelegateSelectors())) {
        throw new InvalidRequestException(
            "You must provide a delegate selector to read token if you are using VaultAgent", INVALID_REQUEST, USER);
      }
    }
  }
}
