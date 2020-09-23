package io.harness.ng.core.dto.secrets;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.ng.core.models.TGTGenerationSpec;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "tgtGenerationMethod",
    visible = true)
public abstract class TGTGenerationSpecDTO {
  public abstract TGTGenerationSpec toEntity();
}
