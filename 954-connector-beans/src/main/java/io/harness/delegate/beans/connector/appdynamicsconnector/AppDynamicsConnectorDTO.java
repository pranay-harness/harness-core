package io.harness.delegate.beans.connector.appdynamicsconnector;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DecryptableEntity;
import io.harness.connector.DelegateSelectable;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretReference;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.base.Preconditions;
import io.swagger.annotations.ApiModelProperty;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(HarnessTeam.CV)
public class AppDynamicsConnectorDTO extends ConnectorConfigDTO implements DecryptableEntity, DelegateSelectable {
  String username;
  @NotNull String accountname;
  @NotNull String controllerUrl;
  @NotNull String accountId;
  Set<String> delegateSelectors;

  @ApiModelProperty(dataType = "string") @SecretReference SecretRefData passwordRef;
  @ApiModelProperty(dataType = "string") @SecretReference SecretRefData clientSecretRef;
  private String clientId;

  @Builder.Default private AppDynamicsAuthType authType = AppDynamicsAuthType.USERNAME_PASSWORD;

  public String getControllerUrl() {
    if (controllerUrl.endsWith("/")) {
      return controllerUrl;
    }
    return controllerUrl + "/";
  }

  public AppDynamicsAuthType getAuthType() {
    if (authType == null) {
      return AppDynamicsAuthType.USERNAME_PASSWORD;
    }
    return authType;
  }

  @Override
  public void validate() {
    if (getAuthType().equals(AppDynamicsAuthType.USERNAME_PASSWORD)) {
      Preconditions.checkNotNull(username, "Username cannot be empty");
      Preconditions.checkNotNull(passwordRef, "Password cannot be empty");
    } else if (getAuthType().equals(AppDynamicsAuthType.API_CLIENT_TOKEN)) {
      Preconditions.checkNotNull(clientId, "Client ID cannot be empty");
      Preconditions.checkNotNull(clientSecretRef, "Client Secret cannot be empty");
    }
  }

  @Override
  public List<DecryptableEntity> getDecryptableEntities() {
    return Collections.singletonList(this);
  }
}
