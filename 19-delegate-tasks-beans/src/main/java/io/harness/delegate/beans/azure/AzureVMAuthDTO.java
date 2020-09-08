package io.harness.delegate.beans.azure;

import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.beans.DecryptableEntity;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.encryption.Encrypted;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import software.wings.jersey.JsonViews;

import java.util.ArrayList;
import java.util.List;

@Data
@ToString(exclude = "key")
@EqualsAndHashCode(callSuper = false)
public class AzureVMAuthDTO implements DecryptableEntity, ExecutionCapabilityDemander {
  @Encrypted(fieldName = "key") private char[] key;
  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedKey;
  private AuthType authType;

  @Builder
  public AzureVMAuthDTO(String encryptedKey, AuthType authType) {
    this.encryptedKey = encryptedKey;
    this.authType = authType;
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return new ArrayList<>();
  }

  public enum AuthType { SSH, PASSWORD }
}
