package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.expression.SecretString.SECRET_MASK;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SecretManagerConfig;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.encryption.Encrypted;
import io.harness.expression.ExpressionEvaluator;
import io.harness.mongo.index.FdIndex;
import io.harness.security.encryption.AccessType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;
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
@AllArgsConstructor
@ToString(exclude = {"authToken", "secretId"})
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldNameConstants(innerTypeName = "BaseVaultConfigKeys")
public abstract class BaseVaultConfig extends SecretManagerConfig {
  @Attributes(title = "Name", required = true) public String name;
  @Attributes(title = "Vault Url", required = true) @FdIndex public String vaultUrl;
  @Attributes(title = "Auth token") @Encrypted(fieldName = "auth_token") public String authToken;
  @Attributes(title = "AppRole Id") public String appRoleId;
  @Attributes(title = "Secret Id") @Encrypted(fieldName = "secret_id") public String secretId;
  @Attributes(title = "Secret Engine Name") public String secretEngineName;
  @Attributes(title = "Token Renewal Interval in minutes", required = true) private long renewalInterval;
  @Attributes(title = "Is Secret Engine Manually Entered") private boolean engineManuallyEntered;
  @JsonIgnore @SchemaIgnore boolean isCertValidationRequired;
  private long renewedAt;

  public boolean isCertValidationRequired() {
    return isCertValidationRequired;
  }

  @Override
  public void maskSecrets() {
    this.authToken = SECRET_MASK;
    this.secretId = SECRET_MASK;
  }

  @JsonIgnore
  @SchemaIgnore
  @Override
  public String getValidationCriteria() {
    return getEncryptionServiceUrl();
  }

  @JsonIgnore
  @SchemaIgnore
  @Override
  public String getEncryptionServiceUrl() {
    return vaultUrl;
  }

  @JsonIgnore
  @SchemaIgnore
  public AccessType getAccessType() {
    return isNotEmpty(appRoleId) ? AccessType.APP_ROLE : AccessType.TOKEN;
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    return Collections.singletonList(
        HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(vaultUrl, maskingEvaluator));
  }
}