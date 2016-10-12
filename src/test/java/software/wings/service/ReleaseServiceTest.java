package software.wings.service;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.ArtifactPathServiceEntry.Builder.anArtifactPathServiceEntry;
import static software.wings.beans.JenkinsArtifactSource.Builder.aJenkinsArtifactSource;
import static software.wings.beans.Release.Builder.aRelease;
import static software.wings.beans.Service.Builder.aService;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;

import com.google.common.collect.Lists;

import org.junit.Before;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.JenkinsArtifactSource;
import software.wings.beans.Release;
import software.wings.beans.Release.Builder;
import software.wings.dl.PageRequest;
import software.wings.rules.RealMongo;
import software.wings.service.intfc.ReleaseService;
import software.wings.utils.ArtifactType;

import java.util.List;
import javax.inject.Inject;

/**
 * Created by peeyushaggarwal on 5/4/16.
 */
@RealMongo
public class ReleaseServiceTest extends WingsBaseTest {
  private static final Builder releaseBuilder =
      aRelease()
          .withAppId(APP_ID)
          .withReleaseName("REL1")
          .withDescription("RELEASE 1")
          .withServices(newHashSet(aService().withUuid(SERVICE_ID).withAppId(APP_ID).build()));

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
    wingsRule.getDatastore().save(aService().withUuid(SERVICE_ID).withAppId(APP_ID).withAppId(APP_ID).build());
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
  public void shouldDeleteRelease() {
    Release release =
        releaseService.create(releaseBuilder.but().withTargetDate(System.currentTimeMillis() + futureOffset).build());
    releaseService.delete(release.getUuid(), release.getAppId());
    assertThat(releaseService.list(new PageRequest<>())).hasSize(0);
  }

  /**
   * Should add artifact source.
   */
  @Test
  public void shouldAddArtifactSource() {
    Release release =
        releaseService.create(releaseBuilder.but().withTargetDate(System.currentTimeMillis() + futureOffset).build());

    releaseService.addArtifactSource(release.getUuid(), APP_ID, artifactSource);

    release = releaseService.list(new PageRequest<>()).get(0);
    assertThat(release.getArtifactSources()).hasSize(1).containsExactly(artifactSource);
  }

  /**
   * Should add artifact source.
   */
  @Test
  public void shouldUpdateArtifactSource() {
    Release release =
        releaseService.create(releaseBuilder.but().withTargetDate(System.currentTimeMillis() + futureOffset).build());

    releaseService.addArtifactSource(release.getUuid(), APP_ID, artifactSource);

    JenkinsArtifactSource updatedArtifactSource =
        aJenkinsArtifactSource()
            .withArtifactType(ArtifactType.WAR)
            .withSourceName("job1")
            .withJobname("job1")
            .withJenkinsSettingId("JENKINS_SETTING_ID2")
            .withArtifactPathServices(Lists.newArrayList(
                anArtifactPathServiceEntry()
                    .withArtifactPathRegex("dist/svr-*.war")
                    .withServices(Lists.newArrayList(aService().withUuid("SERVICE_ID").withAppId(APP_ID).build()))
                    .build()))
            .build();

    releaseService.updateArtifactSource(release.getUuid(), APP_ID, updatedArtifactSource);
    release = releaseService.list(new PageRequest<>()).get(0);
    assertThat(release.getArtifactSources()).hasSize(1).containsExactly(updatedArtifactSource);
  }

  /**
   * Should delete artifact source.
   */
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
