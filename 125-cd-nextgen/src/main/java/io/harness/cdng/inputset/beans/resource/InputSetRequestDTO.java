package io.harness.cdng.inputset.beans.resource;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

import javax.validation.constraints.NotNull;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(NON_NULL)
public class InputSetRequestDTO {
  @NotNull String identifier;
  @NotNull String yaml;
  String description;

  @NotNull String pipelineIdentifier;

  // Add Tags
}
