package io.harness.pms.beans;

import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.EntityName;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.util.Map;

@Data
@Builder
@FieldNameConstants(innerTypeName = "BasicPipelineKeys")
public class BasicPipeline {
  @EntityName String name;
  @EntityIdentifier String identifier;

  String description;
  Map<String, String> tags;
}
