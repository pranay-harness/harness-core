package software.wings.beans.template.artifacts;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class CustomRepositoryMapping {
  private String artifactRoot;
  private String buildNoPath;
  private List<AttributeMapping> artifactAttributes;

  @Value
  @Builder
  public static class AttributeMapping {
    private String relativePath;
    private String mappedAttribute;
  }
}