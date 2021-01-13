package io.harness.ng.core.dto.secrets;

import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.NGEntityName;
import io.harness.ng.core.models.Secret;
import io.harness.secretmanagerclient.SecretType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Map;
import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class SecretDTOV2 {
  @NotNull private SecretType type;
  @NotNull @NGEntityName private String name;
  @NotNull @EntityIdentifier private String identifier;
  @EntityIdentifier(allowBlank = true) private String orgIdentifier;
  @EntityIdentifier(allowBlank = true) private String projectIdentifier;
  private Map<String, String> tags;
  private String description;

  @JsonProperty("spec")
  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type", visible = true)
  @Valid
  @NotNull
  private SecretSpecDTO spec;

  @Builder
  public SecretDTOV2(SecretType type, String name, String description, String identifier, String orgIdentifier,
      String projectIdentifier, Map<String, String> tags, SecretSpecDTO spec) {
    this.type = type;
    this.name = name;
    this.description = description;
    this.identifier = identifier;
    this.orgIdentifier = orgIdentifier;
    this.projectIdentifier = projectIdentifier;
    this.tags = tags;
    this.spec = spec;
  }

  public Secret toEntity() {
    return Secret.builder()
        .orgIdentifier(getOrgIdentifier())
        .projectIdentifier(getProjectIdentifier())
        .identifier(getIdentifier())
        .description(getDescription())
        .name(getName())
        .tags(getTags())
        .type(getType())
        .secretSpec(Optional.ofNullable(getSpec()).map(SecretSpecDTO::toEntity).orElse(null))
        .build();
  }
}
