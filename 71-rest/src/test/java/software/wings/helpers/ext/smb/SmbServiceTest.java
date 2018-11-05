package software.wings.helpers.ext.smb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;
import static org.powermock.api.mockito.PowerMockito.when;
import static software.wings.common.Constants.ARTIFACT_PATH;
import static software.wings.helpers.ext.jenkins.BuildDetails.Builder.aBuildDetails;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.SmbConfig;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.service.impl.SmbHelperService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SmbServiceTest extends WingsBaseTest {
  @Mock private SmbHelperService smbHelperService;
  @Inject private SmbService smbService;
  private static final String TEST_SMB_URL = "smb://192.168.128.161/share";
  private static final String TEST_USER = "test";
  private static final String TEST_PASSWORD = "test";

  private static final SmbConfig smbConfig = SmbConfig.builder()
                                                 .accountId(ACCOUNT_ID)
                                                 .smbUrl(TEST_SMB_URL)
                                                 .username(TEST_USER)
                                                 .password(TEST_PASSWORD.toCharArray())
                                                 .build();

  @Before
  public void setUp() {
    setInternalState(smbService, "smbHelperService", smbHelperService);
  }

  @Test
  public void shouldGetArtifactPaths() throws IOException {
    List<String> artifactPaths = new ArrayList(Arrays.asList("a.txt", "dir1/b.txt"));
    when(smbService.getArtifactPaths(smbConfig, null)).thenReturn(artifactPaths);
    assertThat(smbService.getArtifactPaths(smbConfig, null)).isNotNull().hasSize(2);
  }

  @Test
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

    when(smbService.getBuildDetails(smbConfig, null, artifactPaths, false)).thenReturn(buildDetailsList);
    assertThat(smbService.getBuildDetails(smbConfig, null, artifactPaths, false)).isNotNull().hasSize(1);
  }
}
