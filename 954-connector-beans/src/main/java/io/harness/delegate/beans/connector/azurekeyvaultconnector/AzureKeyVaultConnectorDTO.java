package io.harness.delegate.beans.connector.azurekeyvaultconnector;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.azure.AzureEnvironmentType.AZURE;

import static com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.harness.annotations.dev.OwnedBy;
import io.harness.azure.AzureEnvironmentType;
import io.harness.beans.DecryptableEntity;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;
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
public class AzureKeyVaultConnectorDTO extends ConnectorConfigDTO {
  @NotNull private String clientId;
  private String secretKey;
  @NotNull private String tenantId;
  @NotNull private String vaultName;
  @NotNull private String subscription;
  private boolean isDefault;

  @Builder.Default private AzureEnvironmentType azureEnvironmentType = AZURE;

  @Override
  public List<DecryptableEntity> getDecryptableEntities() {
    return new ArrayList<>();
  }

  @Override
  public void validate() {
    Preconditions.checkNotNull(this.clientId, "clientId cannot be empty");
    Preconditions.checkNotNull(this.tenantId, "tenantId cannot be empty");
    Preconditions.checkNotNull(this.vaultName, "vaultName cannot be empty");
    Preconditions.checkNotNull(this.subscription, "subscription cannot be empty");
  }
}
