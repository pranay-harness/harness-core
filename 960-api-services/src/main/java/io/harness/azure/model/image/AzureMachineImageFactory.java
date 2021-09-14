/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
