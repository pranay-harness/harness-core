package io.harness.ccm.views.entities;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ViewField {
  private String fieldId;
  private String fieldName;
  private ViewFieldIdentifier identifier;
  private String identifierName;
}
