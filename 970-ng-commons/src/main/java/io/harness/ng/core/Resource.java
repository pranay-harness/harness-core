package io.harness.ng.core;

import io.harness.ng.core.common.beans.KeyValuePair;

import java.util.List;
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
  List<KeyValuePair> labels;
}
