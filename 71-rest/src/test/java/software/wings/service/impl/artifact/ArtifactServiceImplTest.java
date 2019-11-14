package software.wings.service.impl.artifact;

import static io.harness.rule.OwnerRule.UNKNOWN;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_SOURCE_NAME;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SETTING_ID;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.beans.artifact.AmiArtifactStream;
import software.wings.beans.artifact.JenkinsArtifactStream;

public class ArtifactServiceImplTest extends WingsBaseTest {
  @Inject ArtifactServiceImpl artifactService;

  private AmiArtifactStream artifactStream = new AmiArtifactStream();

  private JenkinsArtifactStream jenkinsArtifactStream = JenkinsArtifactStream.builder()
                                                            .appId(APP_ID)
                                                            .uuid(ARTIFACT_STREAM_ID)
                                                            .sourceName(ARTIFACT_SOURCE_NAME)
                                                            .settingId(SETTING_ID)
                                                            .jobname("JOB")
                                                            .serviceId(SERVICE_ID)
                                                            .artifactPaths(asList("*WAR"))
                                                            .build();

  @Before
  public void setUp() throws Exception {}

  @Test
  @Owner(emails = UNKNOWN)
  @Category(UnitTests.class)
  public void shouldFetchAMIBuilds() {
    artifactStream.setRegion("TestRegion");

    assertThat(artifactService.prepareArtifactWithMetadataQuery(artifactStream)).isNotNull();
  }

  @Test
  @Owner(emails = UNKNOWN)
  @Category(UnitTests.class)
  public void shouldFetchNonAMIBuilds() {
    assertThat(artifactService.prepareArtifactWithMetadataQuery(jenkinsArtifactStream)).isNotNull();
  }
}