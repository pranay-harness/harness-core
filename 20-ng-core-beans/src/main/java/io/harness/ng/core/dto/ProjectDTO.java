package io.harness.ng.core.dto;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import io.harness.ModuleType;
import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.NGEntityName;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
@ApiModel(value = "Project")
public class ProjectDTO {
  String accountIdentifier;
  @EntityIdentifier(allowBlank = true) String orgIdentifier;
  @ApiModelProperty(required = true) @EntityIdentifier(allowBlank = false) String identifier;
  @ApiModelProperty(required = true) @NGEntityName String name;
  String color;
  @Size(max = 1024) List<ModuleType> modules;
  @Size(max = 1024) String description;
  @Size(max = 128) Map<String, String> tags;
  Long lastModifiedAt;
  @JsonIgnore Long version;
}
