package io.harness.k8s.model;

import static io.harness.rule.OwnerRule.ABOSII;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.k8s.model.Release.Status;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Collections;

public class ReleaseHistoryModelTest extends CategoryTest {
  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetSuccessfulRelease() {
    ReleaseHistory releaseHistory = ReleaseHistory.createNew();
    assertThat(releaseHistory.getRelease(1)).isNull();

    releaseHistory.createNewRelease(Collections.emptyList());
    releaseHistory.setReleaseNumber(1);
    releaseHistory.setReleaseStatus(Status.Succeeded);
    releaseHistory.createNewRelease(Collections.emptyList());
    releaseHistory.setReleaseNumber(2);
    releaseHistory.setReleaseStatus(Status.Succeeded);
    assertThat(releaseHistory.getRelease(1)).isNotNull();
    assertThat(releaseHistory.getRelease(1).getNumber()).isEqualTo(1);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testCleanup() {
    ReleaseHistory releaseHistory = ReleaseHistory.createNew();
    // check no exception thrown when empty
    releaseHistory.cleanup();

    // should delete failed release
    releaseHistory.createNewRelease(Collections.emptyList());
    releaseHistory.setReleaseNumber(0);
    releaseHistory.setReleaseStatus(Status.Failed);
    releaseHistory.cleanup();
    assertThat(releaseHistory.getReleases()).isEmpty();

    // should keep only latest successful and remove failed
    releaseHistory.createNewRelease(Collections.emptyList());
    releaseHistory.setReleaseNumber(1);
    releaseHistory.setReleaseStatus(Status.Succeeded);
    releaseHistory.createNewRelease(Collections.emptyList());
    releaseHistory.setReleaseNumber(2);
    releaseHistory.setReleaseStatus(Status.Failed);
    releaseHistory.createNewRelease(Collections.emptyList());
    releaseHistory.setReleaseNumber(3);
    releaseHistory.setReleaseStatus(Status.Failed);
    releaseHistory.createNewRelease(Collections.emptyList());
    releaseHistory.setReleaseNumber(4);
    releaseHistory.setReleaseStatus(Status.Succeeded);
    releaseHistory.cleanup();
    assertThat(releaseHistory.getReleases()).hasSize(1);
    assertThat(releaseHistory.getLatestRelease().getNumber()).isEqualTo(4);
  }
}
