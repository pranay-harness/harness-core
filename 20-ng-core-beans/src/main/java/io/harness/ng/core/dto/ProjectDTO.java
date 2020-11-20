package io.harness.ng.core.dto;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.harness.ModuleType;
import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.NGEntityName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;
import javax.validation.constraints.Size;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
@ApiModel(value = "Project")
public class ProjectDTO {
  String accountIdentifier;
  @ApiModelProperty(required = true) @EntityIdentifier String orgIdentifier;
  @ApiModelProperty(required = true) @EntityIdentifier String identifier;
  @ApiModelProperty(required = true) @NGEntityName String name;
  String color;
  @Size(max = 1024) List<ModuleType> modules;
  @Size(max = 1024) String description;
  @Size(max = 128) List<String> owners;
  @Size(max = 128) List<String> tags;
  Long lastModifiedAt;
  @JsonIgnore Long version;
}
