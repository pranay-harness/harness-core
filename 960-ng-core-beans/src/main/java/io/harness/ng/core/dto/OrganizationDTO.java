package io.harness.ng.core.dto;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.NGEntityName;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import javax.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@OwnedBy(PL)
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
@ApiModel(value = "Organization")
@Schema(name = "Organization", description = "View of Organization entity as defined in Harness.")
public class OrganizationDTO {
  @ApiModelProperty(required = true) @EntityIdentifier(allowBlank = false) String identifier;
  @ApiModelProperty(required = true) @NGEntityName String name;
  @Size(max = 1024) String description;
  @Size(max = 128) Map<String, String> tags;
  @JsonIgnore Long version;
  @JsonIgnore boolean harnessManaged;

  @Builder
  public OrganizationDTO(String identifier, String name, String description, Map<String, String> tags) {
    this.identifier = identifier;
    this.name = name;
    this.description = description;
    this.tags = tags;
  }
}