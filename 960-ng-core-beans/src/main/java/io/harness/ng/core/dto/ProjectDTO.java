package io.harness.ng.core.dto;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import io.harness.ModuleType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.NGEntityName;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
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
@ApiModel(value = "Project")
@Schema(name = "Project", description = "This is the view of the Project entity defined in Harness")
public class ProjectDTO {
  @EntityIdentifier(allowBlank = true) String orgIdentifier;
  @ApiModelProperty(required = true) @EntityIdentifier(allowBlank = false) String identifier;
  @ApiModelProperty(required = true) @NGEntityName String name;
  String color;
  @Size(max = 1024) List<ModuleType> modules;
  @Size(max = 1024) String description;
  @Size(max = 128) Map<String, String> tags;
  @JsonIgnore Long version;

  @Builder
  public ProjectDTO(String orgIdentifier, String identifier, String name, String color, List<ModuleType> modules,
      String description, Map<String, String> tags) {
    this.orgIdentifier = orgIdentifier;
    this.identifier = identifier;
    this.name = name;
    this.color = color;
    this.modules = modules;
    this.description = description;
    this.tags = tags;
  }
}
