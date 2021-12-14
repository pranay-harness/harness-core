package io.harness.ng.core.service.entity;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@OwnedBy(CDC)
@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(NON_NULL)
public class ServiceBasicInfo {
  String identifier;
  String name;
  String description;

  String accountIdentifier;
  String orgIdentifier;
  String projectIdentifier;

  Map<String, String> tags;
}
