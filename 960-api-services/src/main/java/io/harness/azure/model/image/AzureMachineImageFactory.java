package io.harness.azure.model.image;

import io.harness.azure.model.AzureMachineImageArtifact;
import io.harness.azure.model.AzureMachineImageArtifact.ImageType;

public class AzureMachineImageFactory {
  private AzureMachineImageFactory() {}

  public static AzureMachineImage getAzureImage(AzureMachineImageArtifact image) {
    ImageType imageType = image.getImageType();
    if (imageType == ImageType.IMAGE_GALLERY) {
      return new SharedGalleryImage(image);
    }
    throw new IllegalArgumentException("Unsupported image type " + imageType);
  }
}
