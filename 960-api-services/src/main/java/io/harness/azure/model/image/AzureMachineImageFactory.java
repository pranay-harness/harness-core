/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
