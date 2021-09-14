/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.audit.beans;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.Resource;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotBlank;

@OwnedBy(PL)
@Data
@Builder
@JsonInclude(NON_NULL)
@FieldNameConstants(innerTypeName = "ResourceKeys")
public class ResourceDTO {
  @NotNull @NotBlank String type;
  @NotNull @NotBlank String identifier;
  @Size(max = 5) Map<String, String> labels;

  public static ResourceDTO fromResource(Resource resource) {
    if (resource == null) {
      return null;
    }
    return ResourceDTO.builder()
        .type(resource.getType())
        .identifier(resource.getIdentifier())
        .labels(resource.getLabels())
        .build();
  }
}
