package io.harness.pms.beans.yaml;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.WRAPPER_OBJECT;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.EntityName;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.util.Map;

@Data
@Builder
@FieldNameConstants(innerTypeName = "BasicPipelineKeys")
@JsonTypeInfo(use = NAME, include = WRAPPER_OBJECT)
@JsonTypeName("pipeline")
public class BasicPipeline {
  @EntityName String name;
  @EntityIdentifier String identifier;

  String description;
  Map<String, String> tags;
}
