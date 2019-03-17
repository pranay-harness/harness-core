package software.wings.helpers.ext.sftp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;
import static org.powermock.api.mockito.PowerMockito.when;
import static software.wings.common.Constants.ARTIFACT_PATH;
import static software.wings.helpers.ext.jenkins.BuildDetails.Builder.aBuildDetails;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.SftpConfig;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.service.impl.SftpHelperService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SftpServiceTest extends WingsBaseTest {
  @Mock private SftpHelperService sftpHelperService;
  @Inject private SftpService sftpService;
  private static final String TEST_SFTP_URL = "sftp://192.168.128.32";
  private static final String TEST_USER = "test";
  private static final String TEST_PASSWORD = "test";

  private static final SftpConfig sftpConfig = SftpConfig.builder()
                                                   .accountId(ACCOUNT_ID)
                                                   .sftpUrl(TEST_SFTP_URL)
                                                   .username(TEST_USER)
                                                   .password(TEST_PASSWORD.toCharArray())
                                                   .build();

  @Before
  public void setUp() {
    setInternalState(sftpService, "sftpHelperService", sftpHelperService);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldGetArtifactPaths() throws IOException {
    List<String> artifactPaths = new ArrayList(Arrays.asList("a.txt", "dir1/b.txt"));
    when(sftpService.getArtifactPaths(sftpConfig, null)).thenReturn(artifactPaths);
    assertThat(sftpService.getArtifactPaths(sftpConfig, null)).isNotNull().hasSize(2);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldGetBuildDetails() throws IOException {
    List<String> artifactPaths = new ArrayList(Arrays.asList("a.txt"));
    List<BuildDetails> buildDetailsList = Lists.newArrayList();
    Map<String, String> buildParams = new HashMap<>();
    buildParams.put(ARTIFACT_PATH, "a.txt");
    buildParams.put("fileName", "a.txt");
    buildParams.put("allocationSize", "100");

    buildDetailsList.add(aBuildDetails()
                             .withNumber(buildParams.get("filename"))
                             .withArtifactPath(buildParams.get(ARTIFACT_PATH))
                             .withBuildParameters(buildParams)
                             .build());

    when(sftpService.getBuildDetails(sftpConfig, null, artifactPaths, false)).thenReturn(buildDetailsList);
    assertThat(sftpService.getBuildDetails(sftpConfig, null, artifactPaths, false)).isNotNull().hasSize(1);
  }
}
