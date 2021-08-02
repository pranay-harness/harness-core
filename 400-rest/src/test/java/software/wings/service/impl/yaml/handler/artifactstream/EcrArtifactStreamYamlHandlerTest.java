package software.wings.service.impl.yaml.handler.artifactstream;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.git.model.ChangeType.MODIFY;
import static io.harness.rule.OwnerRule.DEEPAK_PUTHRAYA;

import static software.wings.beans.artifact.ArtifactStreamType.ECR;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SETTING_ID;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.beans.Application;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.EcrArtifactStream;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.GitFileChange;
import software.wings.beans.yaml.YamlType;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.SettingsService;
import software.wings.yaml.handler.YamlHandlerTestBase;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@OwnedBy(CDC)
public class EcrArtifactStreamYamlHandlerTest extends YamlHandlerTestBase {
  @InjectMocks @Inject private EcrArtifactStreamYamlHandler yamlHandler;
  @Mock AppService appService;
  @Mock ArtifactStreamService artifactStreamService;
  @Mock SettingsService settingsService;
  @Mock private YamlHelper yamlHelper;

  @Test
  @Owner(developers = DEEPAK_PUTHRAYA)
  @Category(UnitTests.class)
  public void toYaml() {
    EcrArtifactStream artifactStream =
        EcrArtifactStream.builder().settingId(SETTING_ID).region("us-east-1").imageName("hello-world").build();
    when(settingsService.get(eq(SETTING_ID)))
        .thenReturn(SettingAttribute.Builder.aSettingAttribute().withUuid(SETTING_ID).build());
    EcrArtifactStream.Yaml yaml = yamlHandler.toYaml(artifactStream, APP_ID);
    assertThat(yaml.getImageName()).isEqualTo("hello-world");
    assertThat(yaml.getRegion()).isEqualTo("us-east-1");
  }

  @Test
  @Owner(developers = DEEPAK_PUTHRAYA)
  @Category(UnitTests.class)
  public void upsertFromYaml() {
    SettingAttribute settingAttribute = SettingAttribute.Builder.aSettingAttribute().withAccountId(ACCOUNT_ID).build();
    when(settingsService.get(SETTING_ID)).thenReturn(settingAttribute);
    when(settingsService.getByName(ACCOUNT_ID, APP_ID, "test server")).thenReturn(settingAttribute);
    EcrArtifactStream.Yaml baseYaml = EcrArtifactStream.Yaml.builder()
                                          .imageName("hello-world")
                                          .region("us-east-1")
                                          .harnessApiVersion("1.0")
                                          .serverName("test server")
                                          .build();
    ChangeContext changeContext = ChangeContext.Builder.aChangeContext()
                                      .withYamlType(YamlType.ARTIFACT_STREAM)
                                      .withYaml(baseYaml)
                                      .withChange(GitFileChange.Builder.aGitFileChange()
                                                      .withFilePath("Setup/Applications/a1/Services/s1/as1/test.yaml")
                                                      .withFileContent("harnessApiVersion: '1.0'\n"
                                                          + "type: ECR\n"
                                                          + "region: us-east-1\n"
                                                          + "imageName: hello-world\n"
                                                          + "serverName: test server")
                                                      .withAccountId(ACCOUNT_ID)
                                                      .withChangeType(MODIFY)
                                                      .build())
                                      .build();
    Application application = Application.Builder.anApplication().name("a1").uuid(APP_ID).accountId(ACCOUNT_ID).build();
    when(appService.getAppByName(ACCOUNT_ID, "a1")).thenReturn(application);
    when(appService.get(APP_ID)).thenReturn(application);
    when(yamlHelper.getAppId(
             changeContext.getChange().getAccountId(), "Setup/Applications/a1/Services/s1/as1/test.yaml"))
        .thenReturn(APP_ID);
    when(yamlHelper.getServiceId(APP_ID, "Setup/Applications/a1/Services/s1/as1/test.yaml")).thenReturn(SERVICE_ID);
    final ArgumentCaptor<EcrArtifactStream> captor = ArgumentCaptor.forClass(EcrArtifactStream.class);
    yamlHandler.upsertFromYaml(changeContext, asList(changeContext));
    verify(artifactStreamService).createWithBinding(anyString(), captor.capture(), anyBoolean());
    final EcrArtifactStream artifactStream = captor.getValue();
    assertThat(artifactStream).isNotNull();
    assertThat(artifactStream.getArtifactStreamType()).isEqualTo(ECR.name());
    assertThat(artifactStream.getImageName()).isEqualTo("hello-world");
    assertThat(artifactStream.getRegion()).isEqualTo("us-east-1");
  }

  @Test
  @Owner(developers = DEEPAK_PUTHRAYA)
  @Category(UnitTests.class)
  public void testGetYamlClass() {
    assertThat(yamlHandler.getYamlClass()).isEqualTo(EcrArtifactStream.Yaml.class);
  }
}
