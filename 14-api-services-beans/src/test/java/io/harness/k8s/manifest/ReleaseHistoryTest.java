package io.harness.k8s.manifest;

import static io.harness.k8s.model.ReleaseHistory.createFromData;
import static io.harness.rule.OwnerRule.PUNEET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.google.common.collect.ImmutableList;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.exception.WingsException;
import io.harness.k8s.model.KubernetesResourceId;
import io.harness.k8s.model.Release;
import io.harness.k8s.model.Release.Status;
import io.harness.k8s.model.ReleaseHistory;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ReleaseHistoryTest extends CategoryTest {
  @Test
  @Owner(developers = PUNEET)
  @Category(UnitTests.class)
  public void smokeTest() throws Exception {
    ReleaseHistory releaseHistory = ReleaseHistory.createNew();
    releaseHistory.createNewRelease(
        ImmutableList.of(KubernetesResourceId.builder().kind("Deployment").name("nginx").namespace("default").build()));

    String yamlHistory = releaseHistory.getAsYaml();

    ReleaseHistory releaseHistory1 = createFromData(yamlHistory);
    assertThat(releaseHistory1.getAsYaml()).isEqualTo(yamlHistory);
  }

  @Test
  @Owner(developers = PUNEET)
  @Category(UnitTests.class)
  public void noReleaseTest() {
    ReleaseHistory releaseHistory = ReleaseHistory.createNew();

    try {
      releaseHistory.getLatestRelease();
      fail("Should not reach here.");
    } catch (WingsException e) {
      assertThat(e.getMessage()).isEqualTo("No existing release found.");
    }

    try {
      releaseHistory.setReleaseStatus(Status.Succeeded);
      fail("Should not reach here.");
    } catch (WingsException e) {
      assertThat(e.getMessage()).isEqualTo("No existing release found.");
    }
  }

  @Test
  @Owner(developers = PUNEET)
  @Category(UnitTests.class)
  public void createReleaseTest() {
    ReleaseHistory releaseHistory = ReleaseHistory.createNew();
    releaseHistory.createNewRelease(
        ImmutableList.of(KubernetesResourceId.builder().kind("Deployment").name("nginx").namespace("default").build()));
    Release release = releaseHistory.getLatestRelease();

    assertThat(release.getNumber()).isEqualTo(1);
    assertThat(release.getStatus()).isEqualTo(Status.InProgress);
    assertThat(release.getResources()).hasSize(1);
    assertThat(release.getResources().get(0)).hasFieldOrPropertyWithValue("kind", "Deployment");
    assertThat(release.getResources().get(0)).hasFieldOrPropertyWithValue("name", "nginx");
    assertThat(release.getResources().get(0)).hasFieldOrPropertyWithValue("namespace", "default");

    releaseHistory.setReleaseStatus(Status.Succeeded);
    release = releaseHistory.getLatestRelease();
    assertThat(release.getStatus()).isEqualTo(Status.Succeeded);

    releaseHistory.createNewRelease(ImmutableList.of(
        KubernetesResourceId.builder().kind("Deployment").name("nginx-1").namespace("default").build()));
    release = releaseHistory.getLatestRelease();

    assertThat(release.getNumber()).isEqualTo(2);
    assertThat(release.getStatus()).isEqualTo(Status.InProgress);
    assertThat(release.getResources()).hasSize(1);
    assertThat(release.getResources().get(0)).hasFieldOrPropertyWithValue("kind", "Deployment");
    assertThat(release.getResources().get(0)).hasFieldOrPropertyWithValue("name", "nginx-1");
    assertThat(release.getResources().get(0)).hasFieldOrPropertyWithValue("namespace", "default");
  }

  @Test
  @Owner(developers = PUNEET)
  @Category(UnitTests.class)
  public void getLastSuccessfulReleaseTest() {
    ReleaseHistory releaseHistory = ReleaseHistory.createNew();

    Release release = releaseHistory.getLastSuccessfulRelease();
    assertThat(release).isNull();

    releaseHistory.createNewRelease(
        ImmutableList.of(KubernetesResourceId.builder().kind("Deployment").name("nginx").namespace("default").build()));

    release = releaseHistory.getLastSuccessfulRelease();
    assertThat(release).isNull();

    releaseHistory.setReleaseStatus(Status.Succeeded);
    release = releaseHistory.getLastSuccessfulRelease();
    assertThat(release).isNotNull();
    assertThat(release.getStatus()).isEqualTo(Status.Succeeded);

    releaseHistory.setReleaseStatus(Status.Failed);
    release = releaseHistory.getLastSuccessfulRelease();
    assertThat(release).isNull();
  }
}
