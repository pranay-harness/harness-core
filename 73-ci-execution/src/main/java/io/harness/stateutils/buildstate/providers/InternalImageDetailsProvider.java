package io.harness.stateutils.buildstate.providers;

import static io.harness.common.CIExecutionConstants.ADDON_IMAGE_NAME;
import static io.harness.common.CIExecutionConstants.ADDON_IMAGE_TAG;
import static io.harness.common.CIExecutionConstants.CI_REGISTRY_URL;
import static io.harness.common.CIExecutionConstants.DEFAULT_REGISTRY_USERNAME;
import static io.harness.common.CIExecutionConstants.LITE_ENGINE_IMAGE_NAME;
import static io.harness.common.CIExecutionConstants.LITE_ENGINE_IMAGE_TAG;
import static io.harness.govern.Switch.unhandled;

import lombok.experimental.UtilityClass;
import software.wings.beans.container.ImageDetails;
import software.wings.beans.container.ImageDetails.ImageDetailsBuilder;

/**
 * Provides image details for internally used containers
 */
@UtilityClass
// TODO: fetch constants from config file.
public class InternalImageDetailsProvider {
  public enum ImageKind { ADDON_IMAGE, LITE_ENGINE_IMAGE }

  public ImageDetails getImageDetails(ImageKind kind) {
    return getImageDetails(kind, DEFAULT_REGISTRY_USERNAME, "");
  }

  public ImageDetails getImageDetails(ImageKind kind, String userName, String password) {
    if (kind == null) {
      return null;
    }
    ImageDetailsBuilder imageDetailsBuilder = ImageDetails.builder();
    switch (kind) {
      case ADDON_IMAGE:
        imageDetailsBuilder =
            imageDetailsBuilder.name(ADDON_IMAGE_NAME).tag(ADDON_IMAGE_TAG).registryUrl(CI_REGISTRY_URL);
        break;
      case LITE_ENGINE_IMAGE:
        imageDetailsBuilder =
            imageDetailsBuilder.name(LITE_ENGINE_IMAGE_NAME).tag(LITE_ENGINE_IMAGE_TAG).registryUrl(CI_REGISTRY_URL);
        break;
      default:
        unhandled(kind);
    }
    return imageDetailsBuilder.username(userName).password(password).build();
  }
}
