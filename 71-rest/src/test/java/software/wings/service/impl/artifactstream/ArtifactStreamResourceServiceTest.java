package software.wings.service.impl.artifactstream;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Service;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.JenkinsArtifactStream;
import software.wings.scheduler.JobScheduler;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.yaml.YamlDirectoryService;

import java.util.List;

/**
 * Created by peeyushaggarwal on 5/4/16.
 */
public class ArtifactStreamResourceServiceTest extends WingsBaseTest {
  private static final JenkinsArtifactStream artifactStream = JenkinsArtifactStream.builder()
                                                                  .appId(APP_ID)
                                                                  .sourceName("job1")
                                                                  .jobname("job1")
                                                                  .settingId("JENKINS_SETTING_ID")
                                                                  .serviceId(SERVICE_ID)
                                                                  .artifactPaths(asList("dist/svr-*.war"))
                                                                  .build();

  @Mock private JobScheduler jobScheduler;
  @Mock private YamlDirectoryService yamlDirectoryService;

  @InjectMocks @Inject private ArtifactStreamService artifactStreamService;

  /**
   * setup for test.
   */
  @Before
  public void setUp() {
    wingsRule.getDatastore().save(anApplication().withUuid(APP_ID).withAccountId(ACCOUNT_ID).build());
    wingsRule.getDatastore().save(Service.builder().uuid(SERVICE_ID).appId(APP_ID).build());
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
    assertThat(artifactStreamService.list(
                   aPageRequest().addFilter(ArtifactStream.APP_ID_KEY, EQ, artifactStream.getAppId()).build()))
        .hasSameElementsAs(artifactStreams);
  }

  /**
   * Should delete artifact stream.
   */
  @Test
  public void shouldDeleteArtifactStream() {
    ArtifactStream dbArtifactStream = artifactStreamService.create(artifactStream);
    artifactStreamService.delete(dbArtifactStream.getAppId(), dbArtifactStream.getUuid());
    assertThat(artifactStreamService.list(
                   aPageRequest().addFilter(ArtifactStream.APP_ID_KEY, EQ, dbArtifactStream.getAppId()).build()))
        .hasSize(0);
  }
}
