package io.harness.ng.core.dto;

import static java.util.Collections.emptyList;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.EntityName;
import io.harness.ng.ModuleType;
import io.swagger.annotations.ApiModelProperty;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.List;
import javax.validation.constraints.NotNull;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateProjectDTO {
  @ApiModelProperty(required = true) @NotEmpty String accountIdentifier;
  @ApiModelProperty(required = true) @NotEmpty @EntityIdentifier String identifier;
  @ApiModelProperty(required = true) @NotEmpty @EntityName String name;
  @ApiModelProperty(required = true) @NotEmpty String color;
  List<ModuleType> modules = emptyList();
  String description;
  @ApiModelProperty(required = true) @NotNull List<String> owners;
  @ApiModelProperty(required = true) @NotNull List<String> tags;
}
