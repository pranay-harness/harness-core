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
import software.wings.beans.GcpConfig;
import software.wings.beans.artifact.GcsArtifactStream;
import software.wings.delegatetasks.DelegateFileManager;
import software.wings.helpers.ext.gcs.GcsService;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.helpers.ext.jenkins.BuildDetails.Builder;
import software.wings.service.intfc.GcsBuildService;

import java.util.List;
import java.util.Map;

public class GcsBuildServiceTest extends WingsBaseTest {
  @Mock private GcsService gcsService;
  @Inject @InjectMocks private DelegateFileManager delegateFileManager;
  @Inject @InjectMocks private GcsBuildService gcsBuildService;
  private static final List<String> artifactPaths = Lists.newArrayList("path1", "path2");
  private static final GcpConfig gcpConfig = GcpConfig.builder().accountId("accountId").build();

  private static final GcsArtifactStream gcsArtifactStream = GcsArtifactStream.builder()
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
    when(gcsService.getArtifactsBuildDetails(any(), any(), any(), any(), anyBoolean())).thenReturn(buildDetails);
    List<BuildDetails> builds =
        gcsBuildService.getBuilds(APP_ID, gcsArtifactStream.getArtifactStreamAttributes(), gcpConfig, null);
    assertThat(builds).hasSize(1).extracting(BuildDetails::getNumber).containsExactly("10");
    assertThat(builds).extracting(BuildDetails::getArtifactPath).containsExactly("artifact1");
  }

  @Test
  public void shouldGetBuckets() {
    when(gcsService.listBuckets(gcpConfig, null, null))
        .thenReturn(ImmutableMap.of("bucket1", "bucket1", "bucket2", "bucket2"));
    Map<String, String> buckets = gcsBuildService.getBuckets(gcpConfig, null, null);
    assertThat(buckets).hasSize(2).containsKeys("bucket1", "bucket2");
  }

  @Test
  public void shouldGetArtifactPaths() {
    when(gcsService.getArtifactPaths(any(), any(), any())).thenReturn(Lists.newArrayList("path1"));
    List<String> artifactPaths = gcsBuildService.getArtifactPaths(BUILD_JOB_NAME, null, gcpConfig, null);
    assertThat(artifactPaths.size()).isEqualTo(1);
  }
}
