package io.harness.ccm.views.entities;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Schema(description = "An individual Perspective field")
public class ViewField {
  private String fieldId;
  private String fieldName;
  private ViewFieldIdentifier identifier;
  private String identifierName;
}
