package io.harness.stateutils.buildstate.providers;

import static io.harness.common.CIExecutionConstants.ADDON_IMAGE_NAME;
import static io.harness.common.CIExecutionConstants.LITE_ENGINE_IMAGE_NAME;
import static io.harness.rule.OwnerRule.ALEKSANDAR;
import static io.harness.stateutils.buildstate.providers.InternalImageDetailsProvider.ImageKind.ADDON_IMAGE;
import static io.harness.stateutils.buildstate.providers.InternalImageDetailsProvider.ImageKind.LITE_ENGINE_IMAGE;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.executionplan.CIExecutionTest;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.container.ImageDetails;

public class InternalImageDetailsProviderTest extends CIExecutionTest {
  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldGetImageDetails() {
    ImageDetails imageDetails = InternalImageDetailsProvider.getImageDetails(ADDON_IMAGE);
    assertThat(imageDetails.getName()).isEqualTo(ADDON_IMAGE_NAME);
    imageDetails = InternalImageDetailsProvider.getImageDetails(LITE_ENGINE_IMAGE);
    assertThat(imageDetails.getName()).isEqualTo(LITE_ENGINE_IMAGE_NAME);
    imageDetails = InternalImageDetailsProvider.getImageDetails(null);
    assertThat(imageDetails).isNull();
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldGetImageDetailsWithCustomUsernameAndPassword() {
    ImageDetails imageDetails = InternalImageDetailsProvider.getImageDetails(ADDON_IMAGE, "username", "password");
    assertThat(imageDetails.getUsername()).isEqualTo("username");
    assertThat(imageDetails.getPassword()).isEqualTo("password");
  }
}