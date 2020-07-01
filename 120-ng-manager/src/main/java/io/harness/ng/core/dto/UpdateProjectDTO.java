package io.harness.ng.core.dto;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.harness.data.validator.EntityName;
import io.swagger.annotations.ApiModelProperty;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.List;
import javax.validation.constraints.NotNull;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateProjectDTO {
  @ApiModelProperty(required = true) @NotEmpty @EntityName String name;
  @ApiModelProperty(required = true) @NotEmpty String description;
  @ApiModelProperty(required = true) @NotNull List<String> owners;
  @ApiModelProperty(required = true) @NotNull List<String> tags;
  @ApiModelProperty(required = true) @NotNull List<String> purposeList;
}
