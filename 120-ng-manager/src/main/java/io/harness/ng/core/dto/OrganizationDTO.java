package io.harness.ng.core.dto;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;
import javax.validation.constraints.Size;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrganizationDTO {
  String id;
  String accountIdentifier;
  String identifier;
  String name;
  String color;
  String description;
  @Size(max = 128) List<String> tags;
}
