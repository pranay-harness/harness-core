package software.wings.service.impl.aws.manager;

import static io.harness.rule.OwnerRule.SATYAM;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.utils.WingsTestConstants.SETTING_ID;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.AwsConfig;
import software.wings.beans.GitConfig;
import software.wings.service.impl.aws.model.AwsCFGetTemplateParamsResponse;
import software.wings.service.impl.aws.model.AwsCFTemplateParamsData;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.utils.GitUtilsManager;

import java.util.List;

public class AwsCFHelperServiceManagerImplTest extends CategoryTest {
  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testGetParamsData() throws InterruptedException {
    AwsCFHelperServiceManagerImpl service = spy(AwsCFHelperServiceManagerImpl.class);
    DelegateService mockDelegateService = mock(DelegateService.class);
    SettingsService mockSettingService = mock(SettingsService.class);
    SecretManager mockSecretManager = mock(SecretManager.class);
    GitUtilsManager mockGitUtilsManager = mock(GitUtilsManager.class);
    on(service).set("delegateService", mockDelegateService);
    on(service).set("settingService", mockSettingService);
    on(service).set("secretManager", mockSecretManager);
    on(service).set("gitUtilsManager", mockGitUtilsManager);
    doReturn(aSettingAttribute().withValue(AwsConfig.builder().build()).build()).when(mockSettingService).get(any());
    doReturn(emptyList()).when(mockSecretManager).getEncryptionDetails(any(), anyString(), anyString());
    doReturn(GitConfig.builder().build()).when(mockGitUtilsManager).getGitConfig(anyString());
    doReturn(AwsCFGetTemplateParamsResponse.builder()
                 .parameters(asList(AwsCFTemplateParamsData.builder().paramKey("k1").paramType("TEXT").build(),
                     AwsCFTemplateParamsData.builder().paramKey("k2").paramType("ENCRYPTED_TEXT").build()))
                 .build())
        .when(mockDelegateService)
        .executeTask(any());
    List<AwsCFTemplateParamsData> data = service.getParamsData(
        "GIT", "data", SETTING_ID, "us-east-1", "appId", SETTING_ID, "branch", "path", "commitId", true);
    assertThat(data).isNotNull();
    assertThat(data.size()).isEqualTo(2);
    assertThat(data.get(0).getParamKey()).isEqualTo("k1");
    assertThat(data.get(1).getParamKey()).isEqualTo("k2");
  }
}