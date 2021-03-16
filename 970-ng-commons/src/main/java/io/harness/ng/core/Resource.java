package io.harness.ng.core;

import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@Builder
@FieldNameConstants(innerTypeName = "ResourceKeys")
public class Resource {
  @NotEmpty String type;
  @NotEmpty String identifier;
  Map<String, String> labels;
}
