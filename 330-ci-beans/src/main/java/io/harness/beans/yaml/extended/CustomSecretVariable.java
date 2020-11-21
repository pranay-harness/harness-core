package io.harness.beans.yaml.extended;

import io.harness.encryption.SecretRefData;

import com.fasterxml.jackson.annotation.JsonTypeName;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonTypeName("secret")
public class CustomSecretVariable implements CustomVariable {
  @Builder.Default @NotNull Type type = Type.SECRET;
  @NotNull String name;
  @NotNull SecretRefData value;
}
