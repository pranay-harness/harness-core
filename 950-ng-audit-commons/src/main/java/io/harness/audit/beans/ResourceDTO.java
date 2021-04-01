package io.harness.audit.beans;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.Resource;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(PL)
@Data
@Builder
@JsonInclude(NON_NULL)
@FieldNameConstants(innerTypeName = "ResourceKeys")
public class ResourceDTO {
  @NotEmpty String type;
  @NotEmpty String identifier;
  Map<String, String> labels;

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
