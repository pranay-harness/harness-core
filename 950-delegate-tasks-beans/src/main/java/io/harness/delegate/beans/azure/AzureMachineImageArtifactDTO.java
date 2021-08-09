package io.harness.delegate.beans.azure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName("AzureMachineImage")
public class AzureMachineImageArtifactDTO {
  private ImageType imageType;
  private OSType imageOSType;
  private GalleryImageDefinitionDTO imageDefinition;

  public enum OSType { LINUX, WINDOWS }

  public enum ImageType { IMAGE_GALLERY }
}
