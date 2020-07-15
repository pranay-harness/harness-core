package io.harness.ng.core.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProjectDTO {
  String id;
  String accountIdentifier;
  String orgIdentifier;
  String identifier;
  String name;
  String color;
  List<String> purposeList;
  String description;
  List<String> owners;
  List<String> tags;
}