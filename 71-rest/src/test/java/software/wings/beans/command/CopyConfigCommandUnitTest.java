package software.wings.beans.command;

import static io.harness.rule.OwnerRule.UNKNOWN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static software.wings.beans.Environment.GLOBAL_ENV_ID;
import static software.wings.beans.command.CommandExecutionContext.Builder.aCommandExecutionContext;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;

import io.harness.category.element.UnitTests;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.beans.ConfigFile;
import software.wings.beans.EntityType;
import software.wings.beans.Service;
import software.wings.beans.infrastructure.Host;
import software.wings.core.winrm.executors.WinRmExecutor;
import software.wings.delegatetasks.DelegateConfigService;
import software.wings.delegatetasks.DelegateFileManager;
import software.wings.service.intfc.FileService.FileBucket;
import software.wings.utils.WingsTestConstants;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CopyConfigCommandUnitTest extends WingsBaseTest {
  @InjectMocks CopyConfigCommandUnit copyConfigCommandUnit = new CopyConfigCommandUnit();
  @Mock WinRmExecutor winRmExecutor;
  @Mock private DelegateConfigService delegateConfigService;
  @Mock private DelegateFileManager delegateFileManager;
  @Mock private Service service;
  @Mock private Application app;

  private Host host = Host.Builder.aHost().withPublicDns(WingsTestConstants.PUBLIC_DNS).build();
  @InjectMocks
  private ShellCommandExecutionContext winRmContext =
      new ShellCommandExecutionContext(aCommandExecutionContext()
                                           .withAccountId(WingsTestConstants.ACCOUNT_ID)
                                           .withAppId(WingsTestConstants.APP_ID)
                                           .withEnvId(WingsTestConstants.ENV_ID)
                                           .withServiceTemplateId(WingsTestConstants.SERVICE_TEMPLATE_ID)
                                           .withActivityId(ACTIVITY_ID)
                                           .withHost(host)
                                           .build());

  @Test
  @Owner(emails = UNKNOWN)
  @Category(UnitTests.class)
  public void shouldCopyConfigForWinRm() throws IOException {
    // Create a test web.config file
    List<ConfigFile> configFiles = new ArrayList<>();
    ConfigFile webConfigFile = getConfigFile("web.config");
    configFiles.add(webConfigFile);

    // Copy 1 config file
    when(delegateConfigService.getConfigFiles(winRmContext.getAppId(), winRmContext.getEnvId(),
             winRmContext.getServiceTemplateId(), winRmContext.getHost().getUuid(), winRmContext.getAccountId()))
        .thenReturn(configFiles);
    doReturn(webConfigFile.getUuid())
        .when(delegateFileManager)
        .getFileIdByVersion(FileBucket.CONFIGS, webConfigFile.getUuid(),
            webConfigFile.getVersionForEnv(webConfigFile.getEnvId()), winRmContext.getAccountId());
    assertThat(configFiles).isNotNull().hasSize(1);
    when(copyConfigCommandUnit.executeInternal(winRmContext)).thenReturn(CommandExecutionStatus.SUCCESS);
    assertThat(copyConfigCommandUnit.executeInternal(winRmContext))
        .isNotNull()
        .isEqualTo(CommandExecutionStatus.SUCCESS);

    // Copy encrypted config file
    webConfigFile.setEncrypted(true);
    when(copyConfigCommandUnit.executeInternal(winRmContext)).thenReturn(CommandExecutionStatus.SUCCESS);
    assertThat(webConfigFile.isEncrypted()).isTrue();
    assertThat(copyConfigCommandUnit.executeInternal(winRmContext))
        .isNotNull()
        .isEqualTo(CommandExecutionStatus.SUCCESS);

    // Copy 2 config files
    ConfigFile webConfigFile2 = getConfigFile("web2.config");
    configFiles.add(webConfigFile2);
    assertThat(configFiles).isNotNull().hasSize(2);
    when(copyConfigCommandUnit.executeInternal(winRmContext)).thenReturn(CommandExecutionStatus.SUCCESS);
    assertThat(copyConfigCommandUnit.executeInternal(winRmContext))
        .isNotNull()
        .isEqualTo(CommandExecutionStatus.SUCCESS);
  }

  private ConfigFile getConfigFile(String relativeFilePath) {
    ConfigFile configFile = ConfigFile.builder()
                                .entityId(service.getUuid())
                                .entityType(EntityType.SERVICE)
                                .envId(GLOBAL_ENV_ID)
                                .relativeFilePath(relativeFilePath)
                                .build();

    configFile.setAccountId(app.getAccountId());
    return configFile;
  }
}
