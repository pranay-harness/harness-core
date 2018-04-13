package software.wings.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.when;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_NAME;
import static software.wings.utils.WingsTestConstants.BUILD_JOB_NAME;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.AwsConfig;
import software.wings.beans.artifact.AmazonS3ArtifactStream;
import software.wings.delegatetasks.DelegateFileManager;
import software.wings.helpers.ext.amazons3.AmazonS3Service;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.helpers.ext.jenkins.BuildDetails.Builder;
import software.wings.service.intfc.AmazonS3BuildService;

import java.util.List;
import java.util.Map;

/**
 * @author rktummala on 09/30/17
 */
public class AmazonS3BuildServiceTest extends WingsBaseTest {
  @Mock private AmazonS3Service amazonS3Service;
  @Inject @InjectMocks private DelegateFileManager delegateFileManager;
  @Inject @InjectMocks private AmazonS3BuildService amazonS3BuildService;
  private static final List<String> artifactPaths = Lists.newArrayList("path1", "path2");
  private static final AwsConfig awsConfig =
      AwsConfig.builder().accessKey("access").secretKey("secret".toCharArray()).accountId("accountId").build();
  private static final AmazonS3ArtifactStream amazonS3ArtifactStream = AmazonS3ArtifactStream.builder()
                                                                           .uuid(ARTIFACT_STREAM_ID)
                                                                           .appId(APP_ID)
                                                                           .settingId("")
                                                                           .sourceName(ARTIFACT_STREAM_NAME)
                                                                           .jobname(BUILD_JOB_NAME)
                                                                           .artifactPaths(artifactPaths)
                                                                           .build();

  @Before
  public void setUp() throws Exception {}

  @Test
  public void shouldGetBuilds() {
    List<BuildDetails> buildDetails = Lists.newArrayList(
        Builder.aBuildDetails().withNumber("10").withRevision("10").withArtifactPath("artifact1").build());
    when(amazonS3Service.getArtifactsBuildDetails(any(), any(), any(), any(), anyBoolean())).thenReturn(buildDetails);
    List<BuildDetails> builds =
        amazonS3BuildService.getBuilds(APP_ID, amazonS3ArtifactStream.getArtifactStreamAttributes(), awsConfig, null);
    assertThat(builds).hasSize(1).extracting(BuildDetails::getNumber).containsExactly("10");
    assertThat(builds).extracting(BuildDetails::getArtifactPath).containsExactly("artifact1");
  }

  @Test
  public void shouldGetPlans() {
    when(amazonS3Service.getBuckets(awsConfig, null))
        .thenReturn(ImmutableMap.of("bucket1", "bucket1", "bucket2", "bucket2"));
    Map<String, String> plans = amazonS3BuildService.getPlans(awsConfig, null);
    assertThat(plans).hasSize(2).containsKeys("bucket1", "bucket2");
  }

  @Test
  public void shouldGetArtifactPaths() {
    when(amazonS3Service.getArtifactPaths(any(), any(), any())).thenReturn(Lists.newArrayList("path1"));

    List<String> artifactPaths = amazonS3BuildService.getArtifactPaths(BUILD_JOB_NAME, null, awsConfig, null);
    assertThat(artifactPaths.size()).isEqualTo(1);
  }
}
