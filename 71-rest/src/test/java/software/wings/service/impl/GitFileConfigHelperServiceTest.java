package software.wings.service.impl;

import static io.harness.rule.OwnerRule.ABOSII;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.GitFileConfig;
import software.wings.beans.SettingAttribute;
import software.wings.service.intfc.SettingsService;

public class GitFileConfigHelperServiceTest extends WingsBaseTest {
  @Mock private SettingsService settingsService;

  @InjectMocks @Inject GitFileConfigHelperService configHelperService;

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetGitFileConfigFromYaml() {
    SettingAttribute setting = SettingAttribute.Builder.aSettingAttribute().withUuid("uuid").build();
    GitFileConfig gitFileConfig = getBaseGitFileConfig();

    doReturn(setting).when(settingsService).getByName(ACCOUNT_ID, APP_ID, gitFileConfig.getConnectorName());
    GitFileConfig newGitFileConfig = configHelperService.getGitFileConfigFromYaml(ACCOUNT_ID, APP_ID, gitFileConfig);
    assertThat(newGitFileConfig.getConnectorId()).isEqualTo(setting.getUuid());
    assertThat(newGitFileConfig.getConnectorName()).isNull();
    assertGitFileConfigTheSame(gitFileConfig, newGitFileConfig);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetGitFileConfigForToYaml() {
    SettingAttribute setting = SettingAttribute.Builder.aSettingAttribute().withName("name").build();
    GitFileConfig gitFileConfig = getBaseGitFileConfig();

    doReturn(setting).when(settingsService).get(gitFileConfig.getConnectorId());
    GitFileConfig newGitFileConfig = configHelperService.getGitFileConfigForToYaml(gitFileConfig);
    assertThat(newGitFileConfig.getConnectorId()).isNull();
    assertThat(newGitFileConfig.getConnectorName()).isEqualTo(setting.getName());
    assertGitFileConfigTheSame(gitFileConfig, newGitFileConfig);
  }

  private GitFileConfig getBaseGitFileConfig() {
    return GitFileConfig.builder()
        .connectorName("gitConnector")
        .branch("master")
        .useBranch(true)
        .commitId("commitId")
        .filePath("filePath")
        .filePathList(asList("filePath1", "filePath2"))
        .build();
  }

  private void assertGitFileConfigTheSame(GitFileConfig oldConfig, GitFileConfig newConfig) {
    assertThat(newConfig.getBranch()).isEqualTo(oldConfig.getBranch());
    assertThat(newConfig.getCommitId()).isEqualTo(oldConfig.getCommitId());
    assertThat(newConfig.getFilePath()).isEqualTo(oldConfig.getFilePath());
    assertThat(newConfig.isUseBranch()).isEqualTo(oldConfig.isUseBranch());
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetGitFileConfigFromYamlWithNullSettingAttribute() {
    GitFileConfig gitFileConfig = getBaseGitFileConfig();
    doReturn(null).when(settingsService).get(ACCOUNT_ID, APP_ID, gitFileConfig.getConnectorName());
    assertThatThrownBy(() -> configHelperService.getGitFileConfigFromYaml(ACCOUNT_ID, APP_ID, gitFileConfig))
        .hasMessageContaining("INVALID_ARGUMENT");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetGitFileConfigForToYamlWithNullSettingAttribute() {
    GitFileConfig gitFileConfig = getBaseGitFileConfig();
    doReturn(null).when(settingsService).get(gitFileConfig.getConnectorId());
    assertThatThrownBy(() -> configHelperService.getGitFileConfigForToYaml(gitFileConfig))
        .hasMessageContaining("INVALID_ARGUMENT");
  }
}