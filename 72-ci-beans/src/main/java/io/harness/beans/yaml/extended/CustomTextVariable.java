package io.harness.beans.yaml.extended;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotNull;

@Value
@Builder
@JsonTypeName("text")
public class CustomTextVariable implements CustomVariable {
  @Builder.Default @NotNull Type type = Type.TEXT;
  @NotNull String name;
  @NotNull String value;
}
