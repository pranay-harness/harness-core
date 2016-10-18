package software.wings.service;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Service.Builder.aService;
import static software.wings.beans.artifact.ArtifactPathServiceEntry.Builder.anArtifactPathServiceEntry;
import static software.wings.beans.artifact.JenkinsArtifactStream.Builder.aJenkinsArtifactStream;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;

import com.google.common.collect.Lists;

import org.junit.Before;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.JenkinsArtifactStream;
import software.wings.dl.PageRequest;
import software.wings.rules.RealMongo;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.utils.ArtifactType;

import java.util.List;
import javax.inject.Inject;

/**
 * Created by peeyushaggarwal on 5/4/16.
 */
@RealMongo
public class ArtifactStreamResourceServiceTest extends WingsBaseTest {
  private static final JenkinsArtifactStream artifactStream =
      aJenkinsArtifactStream()
          .withAppId(APP_ID)
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

  @Inject private ArtifactStreamService artifactStreamService;

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
  public void shouldCreateArtifactStream() {
    assertThat(artifactStreamService.create(artifactStream)).isNotNull();
  }

  /**
   * Should list all releases.
   */
  @Test
  public void shouldListAllReleases() {
    List<ArtifactStream> artifactStreams = Lists.newArrayList();
    artifactStreams.add(artifactStreamService.create(artifactStream));
    assertThat(artifactStreamService.list(new PageRequest<>())).hasSameElementsAs(artifactStreams);
  }

  @Test
  public void shouldDeleteRelease() {
    ArtifactStream artifactStream = artifactStreamService.create(ArtifactStreamResourceServiceTest.artifactStream);
    artifactStreamService.delete(artifactStream.getUuid(), artifactStream.getAppId());
    assertThat(artifactStreamService.list(new PageRequest<>())).hasSize(0);
  }
}
