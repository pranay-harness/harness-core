package io.harness.ng.core.common.beans;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@FieldNameConstants(innerTypeName = "NGTagKeys")
@Schema(name = "NGTag", description = "This is the view of the tags of the entity.")
public class NGTag implements Serializable {
  @NotNull private String key;
  @NotNull private String value;
}
