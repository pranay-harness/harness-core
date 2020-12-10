package io.harness.ng.core.dto;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.NGEntityName;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.Map;
import javax.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
@ApiModel(value = "Organization")
public class OrganizationDTO {
  String accountIdentifier;
  @ApiModelProperty(required = true) @EntityIdentifier(allowBlank = false) String identifier;
  @ApiModelProperty(required = true) @NGEntityName String name;
  @Size(max = 1024) String description;
  @Size(max = 128) Map<String, String> tags;
  @JsonIgnore Long version;
  @JsonIgnore boolean harnessManaged;

  @Builder
  public OrganizationDTO(
      String accountIdentifier, String identifier, String name, String description, Map<String, String> tags) {
    this.accountIdentifier = accountIdentifier;
    this.identifier = identifier;
    this.name = name;
    this.description = description;
    this.tags = tags;
  }
}