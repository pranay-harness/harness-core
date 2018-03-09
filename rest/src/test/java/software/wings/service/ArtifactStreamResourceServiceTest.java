package software.wings.service;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Service.Builder.aService;
import static software.wings.beans.artifact.JenkinsArtifactStream.Builder.aJenkinsArtifactStream;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.JenkinsArtifactStream;
import software.wings.dl.PageRequest;
import software.wings.scheduler.JobScheduler;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.yaml.YamlDirectoryService;

import java.util.List;

/**
 * Created by peeyushaggarwal on 5/4/16.
 */
public class ArtifactStreamResourceServiceTest extends WingsBaseTest {
  private static final JenkinsArtifactStream artifactStream = aJenkinsArtifactStream()
                                                                  .withAppId(APP_ID)
                                                                  .withSourceName("job1")
                                                                  .withJobname("job1")
                                                                  .withSettingId("JENKINS_SETTING_ID")
                                                                  .withServiceId(SERVICE_ID)
                                                                  .withArtifactPaths(asList("dist/svr-*.war"))
                                                                  .build();

  @Mock private JobScheduler jobScheduler;
  @Mock private YamlDirectoryService yamlDirectoryService;

  @InjectMocks @Inject private ArtifactStreamService artifactStreamService;

  /**
   * setup for test.
   */
  @Before
  public void setUp() {
    wingsRule.getDatastore().save(anApplication().withUuid(APP_ID).build());
    wingsRule.getDatastore().save(aService().withUuid(SERVICE_ID).withAppId(APP_ID).withAppId(APP_ID).build());
  }

  /**
   * Should create artifact stream.
   */
  @Test
  public void shouldCreateArtifactStream() {
    assertThat(artifactStreamService.create(artifactStream)).isNotNull();
  }

  /**
   * Should list all artifact streams.
   */
  @Test
  public void shouldListAllArtifactStreams() {
    List<ArtifactStream> artifactStreams = Lists.newArrayList();
    artifactStreams.add(artifactStreamService.create(artifactStream));
    assertThat(artifactStreamService.list(new PageRequest<>())).hasSameElementsAs(artifactStreams);
  }

  /**
   * Should delete artifact stream.
   */
  @Test
  public void shouldDeleteArtifactStream() {
    ArtifactStream dbArtifactStream = artifactStreamService.create(artifactStream);
    artifactStreamService.delete(dbArtifactStream.getAppId(), dbArtifactStream.getUuid());
    assertThat(artifactStreamService.list(new PageRequest<>())).hasSize(0);
  }
}
