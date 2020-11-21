package io.harness.yaml.extended.ci.codebase;

import io.harness.yaml.extended.ci.codebase.impl.GitHubCodeBase;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CodeBase {
  @NotNull @JsonProperty("type") CodeBaseType codeBaseType;

  @JsonProperty("spec")
  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
  @JsonSubTypes({ @JsonSubTypes.Type(value = GitHubCodeBase.class, name = "GitHub") })
  @NotNull
  CodeBaseSpec codeBaseSpec;

  @Builder
  public CodeBase(CodeBaseType codeBaseType, CodeBaseSpec codeBaseSpec) {
    this.codeBaseType = codeBaseType;
    this.codeBaseSpec = codeBaseSpec;
  }
}
