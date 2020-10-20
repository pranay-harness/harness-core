package io.harness.stateutils.buildstate.providers;

import static io.harness.common.CIExecutionConstants.ADDON_IMAGE_NAME;
import static io.harness.common.CIExecutionConstants.DEFAULT_INTERNAL_IMAGE_CONNECTOR;
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
import software.wings.beans.ci.pod.ImageDetailsWithConnector;

public class InternalImageDetailsProviderTest extends CIExecutionTest {
  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldGetImageDetails() {
    ImageDetailsWithConnector imageDetailsWithConnector = InternalImageDetailsProvider.getImageDetails(ADDON_IMAGE);
    assertThat(imageDetailsWithConnector.getImageDetails().getName()).isEqualTo(ADDON_IMAGE_NAME);
    imageDetailsWithConnector = InternalImageDetailsProvider.getImageDetails(LITE_ENGINE_IMAGE);
    assertThat(imageDetailsWithConnector.getImageDetails().getName()).isEqualTo(LITE_ENGINE_IMAGE_NAME);
    imageDetailsWithConnector = InternalImageDetailsProvider.getImageDetails(null);
    assertThat(imageDetailsWithConnector).isNull();
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldGetImageDetailsWithHarnessImageConnector() {
    ImageDetailsWithConnector imageDetailsWithConnector = InternalImageDetailsProvider.getImageDetails(ADDON_IMAGE);
    assertThat(imageDetailsWithConnector.getConnectorName()).isEqualTo(DEFAULT_INTERNAL_IMAGE_CONNECTOR);
  }
}