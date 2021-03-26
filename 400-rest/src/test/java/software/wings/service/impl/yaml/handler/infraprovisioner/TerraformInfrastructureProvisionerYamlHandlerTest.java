package software.wings.service.impl.yaml.handler.infraprovisioner;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.GEORGE;

import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SETTING_ID;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SecretManagerConfig;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.beans.Application;
import software.wings.beans.KmsConfig;
import software.wings.beans.NameValuePair;
import software.wings.beans.Service;
import software.wings.beans.ServiceVariable.Type;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TerraformInfrastructureProvisioner;
import software.wings.beans.TerraformInfrastructureProvisionerYaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.GitFileChange;
import software.wings.beans.yaml.YamlType;
import software.wings.service.impl.yaml.handler.tag.HarnessTagYamlHelper;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.InfrastructureProvisionerService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.utils.WingsTestConstants;
import software.wings.yaml.handler.YamlHandlerTestBase;

import com.google.inject.Inject;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@OwnedBy(CDP)
public class TerraformInfrastructureProvisionerYamlHandlerTest extends YamlHandlerTestBase {
  @Mock private YamlHelper mockYamlHelper;
  @Mock private InfrastructureProvisionerService mockInfrastructureProvisionerService;
  @Mock private ServiceResourceService mockServiceResourceService;
  @Mock private SettingsService mockSettingsService;
  @Mock private AppService appService;
  @Mock private HarnessTagYamlHelper harnessTagYamlHelper;
  @Mock private SecretManager secretManager;

  @InjectMocks @Inject private TerraformInfrastructureProvisionerYamlHandler handler;
  private String validYamlFilePath = "Setup/Applications/APP_NAME/Infrastructure Provisioners/TF_Name.yaml";
  private String validYamlContent = "harnessApiVersion: '1.0'\n"
      + "type: TERRAFORM\n"
      + "backendConfigs:\n"
      + "- name: access_key\n"
      + "  valueType: TEXT\n"
      + "- name: secret_key\n"
      + "  valueType: ENCRYPTED_TEXT\n"
      + "repoName: REPO_NAME\n"
      + "secretMangerName: SECRET_MANAGER\n"
      + "skipRefreshBeforeApplyingPlan: true\n"
      + "sourceRepoBranch: master\n"
      + "sourceRepoSettingName: TERRAFORM_TEST_GIT_REPO\n"
      + "variables:\n"
      + "- name: access_key\n"
      + "  valueType: TEXT\n"
      + "- name: secret_key\n"
      + "  valueType: ENCRYPTED_TEXT\n";

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testCRUDAndGet() throws IOException {
    ChangeContext<TerraformInfrastructureProvisionerYaml> changeContext = getChangeContext();
    TerraformInfrastructureProvisionerYaml yaml = (TerraformInfrastructureProvisionerYaml) getYaml(
        validYamlContent, TerraformInfrastructureProvisionerYaml.class);
    changeContext.setYaml(yaml);
    doReturn(APP_ID).when(mockYamlHelper).getAppId(anyString(), anyString());
    doReturn(null).when(mockInfrastructureProvisionerService).getByName(anyString(), anyString());
    Service service = Service.builder().name("ServiceName").uuid(SERVICE_ID).build();
    doReturn(service).when(mockServiceResourceService).get(anyString(), anyString());
    doReturn(service).when(mockServiceResourceService).getServiceByName(anyString(), anyString());
    SettingAttribute settingAttribute =
        SettingAttribute.Builder.aSettingAttribute().withUuid(SETTING_ID).withName("TERRAFORM_TEST_GIT_REPO").build();
    SecretManagerConfig secretManagerConfig = KmsConfig.builder().uuid("KMSID").name("SECRET_MANAGER").build();
    doReturn(settingAttribute).when(mockSettingsService).getSettingAttributeByName(anyString(), anyString());
    doReturn(settingAttribute).when(mockSettingsService).get(anyString(), anyString());
    doReturn(Application.Builder.anApplication().uuid(APP_ID).build()).when(appService).get(any());
    doReturn(secretManagerConfig).when(secretManager).getSecretManager(anyString(), anyString());
    doReturn(secretManagerConfig).when(secretManager).getSecretManagerByName(any(), any());
    handler.upsertFromYaml(changeContext, asList(changeContext));
    ArgumentCaptor<TerraformInfrastructureProvisioner> captor =
        ArgumentCaptor.forClass(TerraformInfrastructureProvisioner.class);
    verify(mockInfrastructureProvisionerService).save(captor.capture());
    TerraformInfrastructureProvisioner provisionerSaved = captor.getValue();
    assertThat(provisionerSaved).isNotNull();
    assertThat("TERRAFORM").isEqualTo(provisionerSaved.getInfrastructureProvisionerType());
    assertThat(APP_ID).isEqualTo(provisionerSaved.getAppId());
    assertThat(SETTING_ID).isEqualTo(provisionerSaved.getSourceRepoSettingId());
    assertThat(provisionerSaved.getRepoName()).isEqualTo("REPO_NAME");
    assertThat(provisionerSaved.getKmsId()).isEqualTo("KMSID");

    TerraformInfrastructureProvisionerYaml yamlFromObject = handler.toYaml(provisionerSaved, WingsTestConstants.APP_ID);
    String yamlContent = getYamlContent(yamlFromObject);
    assertThat(yamlContent).isEqualTo(validYamlContent);

    List<NameValuePair> variables =
        Arrays.asList(NameValuePair.builder().name("access_key").valueType(Type.TEXT.toString()).build(),
            NameValuePair.builder().name("secret_key").valueType(Type.ENCRYPTED_TEXT.toString()).build());
    TerraformInfrastructureProvisioner provisioner = TerraformInfrastructureProvisioner.builder()
                                                         .appId(APP_ID)
                                                         .uuid("UUID1")
                                                         .name("Name1")
                                                         .description("Desc1")
                                                         .sourceRepoSettingId(SETTING_ID)
                                                         .sourceRepoBranch("master")
                                                         .repoName("REPO_NAME")
                                                         .variables(variables)
                                                         .backendConfigs(variables)
                                                         .kmsId("KMSID")
                                                         .skipRefreshBeforeApplyingPlan(true)
                                                         .build();
    TerraformInfrastructureProvisionerYaml yaml1 = handler.toYaml(provisioner, APP_ID);
    assertThat(yaml1).isNotNull();
    assertThat("TERRAFORM").isEqualTo(yaml1.getType());
    assertThat("1.0").isEqualTo(yaml1.getHarnessApiVersion());
    assertThat("Desc1").isEqualTo(yaml1.getDescription());
    assertThat("master").isEqualTo(yaml1.getSourceRepoBranch());
    assertThat("access_key").isEqualTo(yaml1.getVariables().get(0).getName());
    assertThat("secret_key").isEqualTo(yaml1.getVariables().get(1).getName());
    assertThat("access_key").isEqualTo(yaml1.getBackendConfigs().get(0).getName());
    assertThat("secret_key").isEqualTo(yaml1.getBackendConfigs().get(1).getName());
    assertThat(yaml1.getVariables().size()).isEqualTo(2);
    assertThat(yaml1.getBackendConfigs().size()).isEqualTo(2);
    assertThat("SECRET_MANAGER").isEqualTo(yaml1.getSecretMangerName());
    handler.upsertFromYaml(changeContext, null);
    TerraformInfrastructureProvisioner provisioner1 = captor.getValue();
    assertThat(provisioner).isEqualToIgnoringGivenFields(provisioner1, "uuid", "name", "description");
  }

  private ChangeContext<TerraformInfrastructureProvisionerYaml> getChangeContext() {
    GitFileChange gitFileChange = GitFileChange.Builder.aGitFileChange()
                                      .withAccountId(ACCOUNT_ID)
                                      .withFilePath(validYamlFilePath)
                                      .withFileContent(validYamlContent)
                                      .build();

    ChangeContext<TerraformInfrastructureProvisionerYaml> changeContext = new ChangeContext();
    changeContext.setChange(gitFileChange);
    changeContext.setYamlType(YamlType.PROVISIONER);
    changeContext.setYamlSyncHandler(handler);
    return changeContext;
  }
}
