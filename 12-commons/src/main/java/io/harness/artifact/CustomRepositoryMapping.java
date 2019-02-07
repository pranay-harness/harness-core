package io.harness.artifact;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class CustomRepositoryMapping {
  private String artifactRoot;
  private List<AttributeMapping> artifactAttributes;

  @Value
  @Builder
  public static class AttributeMapping {
    private String fromField;
    private String toField;
  }
}