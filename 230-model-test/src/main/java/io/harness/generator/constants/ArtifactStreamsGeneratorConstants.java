package io.harness.generator.constants;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.CDC)
public class ArtifactStreamsGeneratorConstants {
  private ArtifactStreamsGeneratorConstants() {}

  // AzureMachineImageArtifactStreamGenerator
  public static final String AZURE_MACHINE_IMAGE_ARTIFACT_STREAM_NAME = "azure-machine-image";
  public static final String IMAGE_GALLERY_NAME = "sharedImageGallery";
  public static final String LINUX_IMAGE_DEFINITION_NAME = "linuxImageDefinition";
}
