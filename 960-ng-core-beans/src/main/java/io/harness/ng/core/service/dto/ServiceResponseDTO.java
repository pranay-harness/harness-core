package io.harness.ng.core.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(name = "ServiceResponse", description = "This is the Service entity defined in Harness")
public class ServiceResponseDTO {
  String accountId;
  String identifier;
  String orgIdentifier;
  String projectIdentifier;
  String name;
  String description;
  boolean deleted;
  Map<String, String> tags;
  @JsonIgnore Long version;
}
