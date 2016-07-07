package software.wings.service;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.AppContainer.AppContainerBuilder.anAppContainer;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.ArtifactPathServiceEntry.Builder.anArtifactPathServiceEntry;
import static software.wings.beans.JenkinsArtifactSource.Builder.aJenkinsArtifactSource;
import static software.wings.beans.Release.ReleaseBuilder.aRelease;
import static software.wings.beans.Service.Builder.aService;
import static software.wings.utils.WingsTestConstants.APP_ID;

import com.google.common.collect.Lists;

import org.junit.Before;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.ArtifactSource.ArtifactType;
import software.wings.beans.JenkinsArtifactSource;
import software.wings.beans.Release;
import software.wings.beans.Release.ReleaseBuilder;
import software.wings.dl.PageRequest;
import software.wings.rules.RealMongo;
import software.wings.service.intfc.ReleaseService;

import java.util.List;
import javax.inject.Inject;

// TODO: Auto-generated Javadoc

/**
 * Created by peeyushaggarwal on 5/4/16.
 */
public class ReleaseServiceTest extends WingsBaseTest {
  private static final ReleaseBuilder releaseBuilder =
      aRelease().withAppId(APP_ID).withReleaseName("REL1").withDescription("RELEASE 1");

  private static final JenkinsArtifactSource artifactSource =
      aJenkinsArtifactSource()
          .withArtifactType(ArtifactType.WAR)
          .withSourceName("job1")
          .withJobname("job1")
          .withJenkinsSettingId("JENKINS_SETTING_ID")
          .withArtifactPathServices(Lists.newArrayList(
              anArtifactPathServiceEntry()
                  .withArtifactPathRegex("dist/svr-*.war")
                  .withServices(Lists.newArrayList(aService().withUuid("SERVICE_ID").withAppId(APP_ID).build()))
                  .build()))
          .build();

  private static final long futureOffset = MILLISECONDS.convert(10, MINUTES);

  @Inject private ReleaseService releaseService;

  /**
   * setup for test.
   */
  @Before
  public void setUp() {
    wingsRule.getDatastore().save(anApplication().withUuid(APP_ID).build());
    wingsRule.getDatastore().save(anAppContainer().withUuid("UUID").withAppId(APP_ID).build());
    wingsRule.getDatastore().save(
        aService()
            .withUuid("SERVICE_ID")
            .withAppId(APP_ID)
            .withAppContainer(anAppContainer().withUuid("APP_CONTAINER_ID").withAppId(APP_ID).build())
            .build());
  }

  /**
   * Should create valid release.
   */
  @Test
  public void shouldCreateValidRelease() {
    assertThat(
        releaseService.create(releaseBuilder.but().withTargetDate(System.currentTimeMillis() + futureOffset).build()))
        .isNotNull();
  }

  /**
   * Should update release.
   */
  @Test
  public void shouldUpdateRelease() {
    Release release =
        releaseService.create(releaseBuilder.but().withTargetDate(System.currentTimeMillis() + futureOffset).build());
    assertThat(release.getDescription()).isEqualTo("RELEASE 1");

    Release updatedRelease = releaseService.update(releaseBuilder.but()
                                                       .withUuid(release.getUuid())
                                                       .withDescription("RELEASE 1.1")
                                                       .withTargetDate(System.currentTimeMillis() + futureOffset)
                                                       .build());
    assertThat(updatedRelease.getDescription()).isEqualTo("RELEASE 1.1");
  }

  /**
   * Should list all releases.
   */
  @Test
  public void shouldListAllReleases() {
    List<Release> releases = Lists.newArrayList();
    releases.add(
        releaseService.create(releaseBuilder.but().withTargetDate(System.currentTimeMillis() + futureOffset).build()));

    releases.add(releaseService.create(releaseBuilder.but()
                                           .withTargetDate(System.currentTimeMillis() + futureOffset)
                                           .withReleaseName("REL2")
                                           .withDescription("BUILD2")
                                           .build()));

    assertThat(releaseService.list(new PageRequest<>())).hasSameElementsAs(releases);
  }

  /**
   * Should soft delete release.
   */
  @Test
  public void shouldSoftDeleteRelease() {
    Release release =
        releaseService.create(releaseBuilder.but().withTargetDate(System.currentTimeMillis() + futureOffset).build());
    releaseService.softDelete(release.getUuid(), release.getAppId());
    assertThat(releaseService.list(new PageRequest<>())).hasSize(0);
  }

  /**
   * Should delete inactive release.
   */
  @Test
  public void shouldDeleteInactiveRelease() {
    Release release = releaseService.create(
        releaseBuilder.but().withTargetDate(System.currentTimeMillis() + futureOffset).withActive(false).build());
    assertThat(releaseService.list(new PageRequest<>())).hasSize(0);
  }

  /**
   * Should not delete active release.
   */
  @Test
  public void shouldNotDeleteActiveRelease() {
    Release release =
        releaseService.create(releaseBuilder.but().withTargetDate(System.currentTimeMillis() + futureOffset).build());
    assertThat(releaseService.delete(release.getUuid(), release.getAppId())).isFalse();
    assertThat(releaseService.list(new PageRequest<>())).hasSize(1);
  }

  /**
   * Should add artifact source.
   */
  @RealMongo
  @Test
  public void shouldAddArtifactSource() {
    Release release =
        releaseService.create(releaseBuilder.but().withTargetDate(System.currentTimeMillis() + futureOffset).build());

    releaseService.addArtifactSource(release.getUuid(), APP_ID, artifactSource);

    release = releaseService.list(new PageRequest<>()).get(0);
    assertThat(release.getArtifactSources()).hasSize(1).containsExactly(artifactSource);
  }

  /**
   * Should delete artifact source.
   */
  @RealMongo
  @Test
  public void shouldDeleteArtifactSource() {
    Release release =
        releaseService.create(releaseBuilder.but().withTargetDate(System.currentTimeMillis() + futureOffset).build());

    releaseService.addArtifactSource(release.getUuid(), APP_ID, artifactSource);

    releaseService.deleteArtifactSource(release.getUuid(), APP_ID, artifactSource.getSourceName());

    release = releaseService.list(new PageRequest<>()).get(0);
    assertThat(release.getArtifactSources()).hasSize(0);
  }
}
