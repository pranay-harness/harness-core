package io.harness.beans.yaml.extended;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.encryption.SecretRefData;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotNull;

@Value
@Builder
@JsonTypeName("secret")
public class CustomSecretVariable implements CustomVariable {
  @Builder.Default @NotNull Type type = Type.SECRET;
  @NotNull String name;
  @NotNull SecretRefData value;
}
