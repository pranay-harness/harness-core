package io.harness.ng.core.dto;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.harness.data.validator.EntityName;
import io.harness.ng.ModuleType;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateProjectDTO {
  @EntityName String name;
  String color;
  String description;
  List<String> owners;
  List<String> tags;
  List<ModuleType> modules;
}
