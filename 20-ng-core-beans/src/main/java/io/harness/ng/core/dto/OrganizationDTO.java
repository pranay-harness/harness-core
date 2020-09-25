package io.harness.ng.core.dto;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
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
@ApiModel(value = "Organization")
public class OrganizationDTO {
  String accountIdentifier;
  String identifier;
  String name;
  String color;
  @Size(max = 1024) String description;
  @Size(max = 128) List<String> tags;
  Long lastModifiedAt;
  @JsonIgnore Long version;
}
