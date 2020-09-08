package io.harness.delegate.beans.azure;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.beans.DecryptableEntity;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.encryption.Encrypted;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import software.wings.jersey.JsonViews;

import java.util.Collections;
import java.util.List;

@JsonTypeName("AZURE")
@Data
@ToString(exclude = "key")
@EqualsAndHashCode(callSuper = false)
public class AzureConfigDTO implements DecryptableEntity, ExecutionCapabilityDemander {
  private static final String AZURE_URL = "https://azure.microsoft.com/";
  private String clientId;
  private String tenantId;
  @Encrypted(fieldName = "key") private char[] key;
  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedKey;

  @Builder
  public AzureConfigDTO(String clientId, String tenantId, char[] key, String encryptedKey) {
    this.clientId = clientId;
    this.tenantId = tenantId;
    this.key = key == null ? null : key.clone();
    this.encryptedKey = encryptedKey;
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return Collections.singletonList(
        HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(AZURE_URL));
  }
}
