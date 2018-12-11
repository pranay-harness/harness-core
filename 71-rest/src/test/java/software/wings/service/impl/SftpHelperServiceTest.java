package software.wings.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static software.wings.common.Constants.ARTIFACT_FILE_NAME;
import static software.wings.common.Constants.ARTIFACT_PATH;
import static software.wings.common.Constants.PARENT;
import static software.wings.common.Constants.PATH;
import static software.wings.common.Constants.URL;
import static software.wings.helpers.ext.jenkins.BuildDetails.Builder.aBuildDetails;
import static software.wings.service.impl.instance.InstanceHelperTest.ACCOUNT_ID;

import com.google.api.client.util.Lists;
import com.google.inject.Inject;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.FileAttributes;
import net.schmizz.sshj.sftp.PathComponents;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import net.schmizz.sshj.sftp.SFTPClient;
import org.junit.Test;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.SftpConfig;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SftpHelperServiceTest extends WingsBaseTest {
  @Inject SftpHelperService sftpHelperService;
  @Mock SSHClient sshClient;
  @Mock SFTPClient sftpClient;
  @Mock SftpHelperService mockSftpHelperService;

  private static final String A_DIR = ".";
  private static final String A_PATH = "test.zip";
  private static final String SFTP_WIN_URL = "sftp:\\\\10.0.0.1";
  private static final String SFTP_UNIX_URL = "sftp://10.0.0.1";
  private static final String FTP_WIN_URL = "ftp:\\\\10.0.0.1";
  private static final String FTP_UNIX_URL = "ftp://10.0.0.1";
  private static final String DOMAIN = "test";
  private static final String USER = "test";
  private static final String PASSWORD = "test";

  private static final SftpConfig sftpConfig = SftpConfig.builder()
                                                   .accountId(ACCOUNT_ID)
                                                   .domain(DOMAIN)
                                                   .username(USER)
                                                   .password(PASSWORD.toCharArray())
                                                   .sftpUrl(SFTP_WIN_URL)
                                                   .build();

  @Test
  public void shouldGetSftpPaths() throws IOException {
    List<String> artifactPaths = new ArrayList<>();
    artifactPaths.add(ARTIFACT_PATH);

    // Create SFTP client
    doReturn(sftpClient).when(sshClient).newSFTPClient();
    List<RemoteResourceInfo> remoteResourceInfoList = new ArrayList<>();
    remoteResourceInfoList.add(new RemoteResourceInfo(new PathComponents(A_DIR, A_PATH, "\\"), FileAttributes.EMPTY));
    doReturn(remoteResourceInfoList).when(sftpClient).ls(A_DIR);

    assertThat(remoteResourceInfoList).isNotNull().hasSize(1);
    assertThat(remoteResourceInfoList.get(0).getName().equals(A_PATH));
    assertThat(remoteResourceInfoList.get(0).getParent().equals(A_DIR));
    assertThat(remoteResourceInfoList.get(0).getPath().equals(A_DIR + "\\" + A_PATH));
  }

  @Test
  public void shouldGetSftpConnectionHost() {
    assertThat(sftpHelperService.getSFTPConnectionHost(SFTP_WIN_URL)).isNotEmpty().isEqualTo("10.0.0.1");
    assertThat(sftpHelperService.getSFTPConnectionHost(SFTP_UNIX_URL)).isNotEmpty().isEqualTo("10.0.0.1");
    assertThat(sftpHelperService.getSFTPConnectionHost(FTP_WIN_URL)).isNotEmpty().isEqualTo("10.0.0.1");
    assertThat(sftpHelperService.getSFTPConnectionHost(FTP_UNIX_URL)).isNotEmpty().isEqualTo("10.0.0.1");
  }

  @Test
  public void shouldCheckConnectionSFTPServer() throws IOException {
    doNothing().when(sshClient).connect(sftpHelperService.getSFTPConnectionHost(SFTP_WIN_URL));
    doReturn(true)
        .when(mockSftpHelperService)
        .isConnectibleSFTPServer(sftpHelperService.getSFTPConnectionHost(SFTP_WIN_URL));
  }

  @Test
  public void shouldGetArtifactBuildDetails() throws IOException {
    // Create SFTP client
    doReturn(sftpClient).when(sshClient).newSFTPClient();
    List<RemoteResourceInfo> remoteResourceInfoList = new ArrayList<>();
    remoteResourceInfoList.add(new RemoteResourceInfo(new PathComponents(A_DIR, A_PATH, "\\"), FileAttributes.EMPTY));
    doReturn(remoteResourceInfoList).when(sftpClient).ls(A_DIR);

    assertThat(remoteResourceInfoList).isNotNull().hasSize(1);
    assertThat(remoteResourceInfoList.get(0).getName().equals(A_PATH));
    assertThat(remoteResourceInfoList.get(0).getParent().equals(A_DIR));
    assertThat(remoteResourceInfoList.get(0).getPath().equals(A_DIR + "\\" + A_PATH));

    List<BuildDetails> buildDetailsListForArtifactPath = Lists.newArrayList();
    Map<String, String> map = new HashMap<>();
    map.put(ARTIFACT_PATH, ARTIFACT_PATH);
    map.put(URL, SFTP_WIN_URL);
    map.put(ARTIFACT_FILE_NAME, remoteResourceInfoList.get(0).getName());
    map.put(PATH, remoteResourceInfoList.get(0).getPath());
    map.put(PARENT, remoteResourceInfoList.get(0).getParent());

    buildDetailsListForArtifactPath.add(aBuildDetails()
                                            .withNumber(remoteResourceInfoList.get(0).getName())
                                            .withArtifactPath(ARTIFACT_PATH)
                                            .withBuildUrl(SFTP_WIN_URL)
                                            .withBuildParameters(map)
                                            .build());

    List<String> artifactPaths = Lists.newArrayList();
    artifactPaths.add(A_PATH);
    doReturn(buildDetailsListForArtifactPath)
        .when(mockSftpHelperService)
        .getArtifactDetails(sftpConfig, null, artifactPaths);
    assertThat(buildDetailsListForArtifactPath).isNotNull().hasSize(1);
  }
}
