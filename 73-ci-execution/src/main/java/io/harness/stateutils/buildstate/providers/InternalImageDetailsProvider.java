package io.harness.stateutils.buildstate.providers;

import static io.harness.common.CIExecutionConstants.ADDON_IMAGE_NAME;
import static io.harness.common.CIExecutionConstants.ADDON_IMAGE_TAG;
import static io.harness.common.CIExecutionConstants.DEFAULT_INTERNAL_IMAGE_CONNECTOR;
import static io.harness.common.CIExecutionConstants.LITE_ENGINE_IMAGE_NAME;
import static io.harness.common.CIExecutionConstants.LITE_ENGINE_IMAGE_TAG;
import static io.harness.govern.Switch.unhandled;

import lombok.experimental.UtilityClass;
import software.wings.beans.ci.pod.ImageDetailsWithConnector;
import software.wings.beans.container.ImageDetails;

/**
 * Provides image details for internally used containers
 */
@UtilityClass
// TODO: fetch constants from config file.
public class InternalImageDetailsProvider {
  public enum ImageKind { ADDON_IMAGE, LITE_ENGINE_IMAGE }

  public ImageDetailsWithConnector getImageDetails(ImageKind kind) {
    if (kind == null) {
      return null;
    }
    ImageDetails imageDetails;
    switch (kind) {
      case ADDON_IMAGE:
        imageDetails = ImageDetails.builder().name(ADDON_IMAGE_NAME).tag(ADDON_IMAGE_TAG).build();
        break;
      case LITE_ENGINE_IMAGE:
        imageDetails = ImageDetails.builder().name(LITE_ENGINE_IMAGE_NAME).tag(LITE_ENGINE_IMAGE_TAG).build();
        break;
      default:
        unhandled(kind);
        return null;
    }
    return ImageDetailsWithConnector.builder()
        .imageDetails(imageDetails)
        .connectorName(DEFAULT_INTERNAL_IMAGE_CONNECTOR)
        .build();
  }
}
