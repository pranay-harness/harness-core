package io.harness.app.beans.entities;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CIUsageReference {
  private String identifier;
  private String projectIdentifier;
  private String orgIdentifier;
}
