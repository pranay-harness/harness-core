package software.wings.service.impl.yaml.handler.infraprovisioner;

import static io.harness.rule.OwnerRule.GEORGE;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static software.wings.api.DeploymentType.SSH;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SETTING_ID;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.beans.Application;
import software.wings.beans.BlueprintProperty;
import software.wings.beans.InfrastructureMappingBlueprint;
import software.wings.beans.InfrastructureMappingBlueprint.CloudProviderType;
import software.wings.beans.NameValuePair;
import software.wings.beans.Service;
import software.wings.beans.ServiceVariable.Type;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TerraformInfrastructureProvisioner;
import software.wings.beans.TerraformInfrastructureProvisioner.Yaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.GitFileChange;
import software.wings.beans.yaml.YamlType;
import software.wings.service.impl.yaml.handler.tag.HarnessTagYamlHelper;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.InfrastructureProvisionerService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.yaml.handler.BaseYamlHandlerTest;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TerraformInfrastructureProvisionerYamlHandlerTest extends BaseYamlHandlerTest {
  @Mock private YamlHelper mockYamlHelper;
  @Mock private InfrastructureProvisionerService mockInfrastructureProvisionerService;
  @Mock private ServiceResourceService mockServiceResourceService;
  @Mock private SettingsService mockSettingsService;
  @Mock private AppService appService;
  @Mock private HarnessTagYamlHelper harnessTagYamlHelper;

  @InjectMocks @Inject private TerraformInfrastructureProvisionerYamlHandler handler;
  private String validYamlFilePath = "Setup/Applications/APP_NAME/Infrastructure Provisioners/TF_Name.yaml";
  private String validYamlContent = "harnessApiVersion: '1.0'\n"
      + "type: TERRAFORM\n"
      + "mappingBlueprints:\n"
      + "- cloudProviderType: AWS\n"
      + "  deploymentType: SSH\n"
      + "  properties:\n"
      + "  - name: region\n"
      + "    value: ${terraform.region}\n"
      + "  - name: securityGroups\n"
      + "    value: ${terraform.security_group}\n"
      + "  - name: tags\n"
      + "    value: ${terraform.archive_tags}\n"
      + "  serviceName: Archive\n"
      + "name: Harness Terraform Test\n"
      + "sourceRepoSettingName: TERRAFORM_TEST_GIT_REPO\n"
      + "variables:\n"
      + "  - name: access_key\n"
      + "    valueType: TEXT\n"
      + "  - name: secret_key\n"
      + "    valueType: ENCRYPTED_TEXT\n"
      + "backendConfigs:\n"
      + "  - name: access_key\n"
      + "    valueType: TEXT\n"
      + "  - name: secret_key\n"
      + "    valueType: ENCRYPTED_TEXT\n"
      + "sourceRepoBranch: master";

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testCRUDAndGet() throws IOException {
    ChangeContext<Yaml> changeContext = getChangeContext();
    Yaml yaml = (Yaml) getYaml(validYamlContent, Yaml.class);
    changeContext.setYaml(yaml);
    doReturn(APP_ID).when(mockYamlHelper).getAppId(anyString(), anyString());
    doReturn(null).when(mockInfrastructureProvisionerService).getByName(anyString(), anyString());
    Service service = Service.builder().name("ServiceName").uuid(SERVICE_ID).build();
    doReturn(service).when(mockServiceResourceService).get(anyString(), anyString());
    doReturn(service).when(mockServiceResourceService).getServiceByName(anyString(), anyString());
    SettingAttribute settingAttribute =
        SettingAttribute.Builder.aSettingAttribute().withUuid(SETTING_ID).withName("Name").build();
    doReturn(settingAttribute).when(mockSettingsService).getSettingAttributeByName(anyString(), anyString());
    doReturn(settingAttribute).when(mockSettingsService).get(anyString(), anyString());
    doReturn(Application.Builder.anApplication().uuid(APP_ID).build()).when(appService).get(any());
    handler.upsertFromYaml(changeContext, asList(changeContext));
    ArgumentCaptor<TerraformInfrastructureProvisioner> captor =
        ArgumentCaptor.forClass(TerraformInfrastructureProvisioner.class);
    verify(mockInfrastructureProvisionerService).save(captor.capture());
    TerraformInfrastructureProvisioner provisionerSaved = captor.getValue();
    assertThat(provisionerSaved).isNotNull();
    assertThat("TERRAFORM").isEqualTo(provisionerSaved.getInfrastructureProvisionerType());
    assertThat(APP_ID).isEqualTo(provisionerSaved.getAppId());
    assertThat(SETTING_ID).isEqualTo(provisionerSaved.getSourceRepoSettingId());
    assertThat(1).isEqualTo(provisionerSaved.getMappingBlueprints().size());

    List<NameValuePair> variables =
        Arrays.asList(NameValuePair.builder().name("access_key").valueType(Type.TEXT.toString()).build(),
            NameValuePair.builder().name("secret_key").valueType(Type.ENCRYPTED_TEXT.toString()).build());
    TerraformInfrastructureProvisioner provisioner =
        TerraformInfrastructureProvisioner.builder()
            .appId(APP_ID)
            .uuid("UUID1")
            .name("Name1")
            .description("Desc1")
            .sourceRepoSettingId(SETTING_ID)
            .sourceRepoBranch("master")
            .variables(variables)
            .backendConfigs(variables)
            .mappingBlueprints(Collections.singletonList(
                InfrastructureMappingBlueprint.builder()
                    .cloudProviderType(CloudProviderType.AWS)
                    .deploymentType(SSH)
                    .serviceId(SERVICE_ID)
                    .properties(asList(BlueprintProperty.builder().name("k2").value("v2").build()))
                    .build()))
            .build();
    TerraformInfrastructureProvisioner.Yaml yaml1 = handler.toYaml(provisioner, APP_ID);
    assertThat(yaml1).isNotNull();
    assertThat("TERRAFORM").isEqualTo(yaml1.getType());
    assertThat("1.0").isEqualTo(yaml1.getHarnessApiVersion());
    assertThat("Name1").isEqualTo(yaml1.getName());
    assertThat("Desc1").isEqualTo(yaml1.getDescription());
    assertThat("master").isEqualTo(yaml1.getSourceRepoBranch());
    assertThat(1).isEqualTo(yaml1.getMappingBlueprints().size());
    assertThat("ServiceName").isEqualTo(yaml1.getMappingBlueprints().get(0).getServiceName());
    assertThat(SSH).isEqualTo(yaml1.getMappingBlueprints().get(0).getDeploymentType());
    assertThat(CloudProviderType.AWS).isEqualTo(yaml1.getMappingBlueprints().get(0).getCloudProviderType());
    assertThat(1).isEqualTo(yaml1.getMappingBlueprints().get(0).getProperties().size());
    assertThat("k2").isEqualTo(yaml1.getMappingBlueprints().get(0).getProperties().get(0).getName());
    assertThat("v2").isEqualTo(yaml1.getMappingBlueprints().get(0).getProperties().get(0).getValue());
    assertThat("access_key").isEqualTo(yaml1.getVariables().get(0).getName());
    assertThat("secret_key").isEqualTo(yaml1.getVariables().get(1).getName());
    assertThat("access_key").isEqualTo(yaml1.getBackendConfigs().get(0).getName());
    assertThat("secret_key").isEqualTo(yaml1.getBackendConfigs().get(1).getName());
    assertThat(yaml1.getVariables().size()).isEqualTo(2);
    assertThat(yaml1.getBackendConfigs().size()).isEqualTo(2);

    handler.upsertFromYaml(changeContext, null);
    TerraformInfrastructureProvisioner provisioner1 = captor.getValue();
    Assertions.assertThat(provisioner)
        .isEqualToIgnoringGivenFields(provisioner1, "uuid", "mappingBlueprints", "name", "description");
  }

  private ChangeContext<Yaml> getChangeContext() {
    GitFileChange gitFileChange = GitFileChange.Builder.aGitFileChange()
                                      .withAccountId(ACCOUNT_ID)
                                      .withFilePath(validYamlFilePath)
                                      .withFileContent(validYamlContent)
                                      .build();

    ChangeContext<Yaml> changeContext = new ChangeContext();
    changeContext.setChange(gitFileChange);
    changeContext.setYamlType(YamlType.PROVISIONER);
    changeContext.setYamlSyncHandler(handler);
    return changeContext;
  }
}
