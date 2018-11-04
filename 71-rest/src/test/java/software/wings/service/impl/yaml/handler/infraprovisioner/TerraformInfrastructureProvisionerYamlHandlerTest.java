package software.wings.service.impl.yaml.handler.infraprovisioner;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.beans.Application;
import software.wings.beans.InfrastructureMappingBlueprint;
import software.wings.beans.InfrastructureMappingBlueprint.CloudProviderType;
import software.wings.beans.NameValuePair;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TerraformInfrastructureProvisioner;
import software.wings.beans.TerraformInfrastructureProvisioner.Yaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.GitFileChange;
import software.wings.beans.yaml.YamlType;
import software.wings.exception.HarnessException;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.InfrastructureProvisionerService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.yaml.handler.BaseYamlHandlerTest;

import java.io.IOException;
import java.util.Collections;

public class TerraformInfrastructureProvisionerYamlHandlerTest extends BaseYamlHandlerTest {
  @Mock private YamlHelper mockYamlHelper;
  @Mock private InfrastructureProvisionerService mockInfrastructureProvisionerService;
  @Mock private ServiceResourceService mockServiceResourceService;
  @Mock private SettingsService mockSettingsService;
  @Mock private AppService appService;

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
      + "sourceRepoBranch: master";

  @Test
  public void testCRUDAndGet() throws HarnessException, IOException {
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
    doReturn(Application.Builder.anApplication().withUuid(APP_ID).build()).when(appService).get(any());
    handler.upsertFromYaml(changeContext, asList(changeContext));
    ArgumentCaptor<TerraformInfrastructureProvisioner> captor =
        ArgumentCaptor.forClass(TerraformInfrastructureProvisioner.class);
    verify(mockInfrastructureProvisionerService).save(captor.capture());
    TerraformInfrastructureProvisioner provisionerSaved = captor.getValue();
    assertNotNull(provisionerSaved);
    assertEquals(provisionerSaved.getInfrastructureProvisionerType(), "TERRAFORM");
    assertEquals(provisionerSaved.getAppId(), APP_ID);
    assertEquals(provisionerSaved.getSourceRepoSettingId(), SETTING_ID);
    assertEquals(provisionerSaved.getMappingBlueprints().size(), 1);

    TerraformInfrastructureProvisioner provisioner =
        TerraformInfrastructureProvisioner.builder()
            .appId(APP_ID)
            .uuid("UUID1")
            .name("Name1")
            .description("Desc1")
            .sourceRepoSettingId(SETTING_ID)
            .sourceRepoBranch("master")
            .mappingBlueprints(Collections.singletonList(
                InfrastructureMappingBlueprint.builder()
                    .cloudProviderType(CloudProviderType.AWS)
                    .deploymentType(SSH)
                    .serviceId(SERVICE_ID)
                    .properties(asList(NameValuePair.builder().name("k2").value("v2").build()))
                    .build()))
            .build();
    TerraformInfrastructureProvisioner.Yaml yaml1 = handler.toYaml(provisioner, APP_ID);
    assertNotNull(yaml1);
    assertEquals(yaml1.getType(), "TERRAFORM");
    assertEquals(yaml1.getHarnessApiVersion(), "1.0");
    assertEquals(yaml1.getName(), "Name1");
    assertEquals(yaml1.getDescription(), "Desc1");
    assertEquals(yaml1.getSourceRepoBranch(), "master");
    assertEquals(yaml1.getMappingBlueprints().size(), 1);
    assertEquals(yaml1.getMappingBlueprints().get(0).getServiceName(), "ServiceName");
    assertEquals(yaml1.getMappingBlueprints().get(0).getDeploymentType(), SSH);
    assertEquals(yaml1.getMappingBlueprints().get(0).getCloudProviderType(), CloudProviderType.AWS);
    assertEquals(yaml1.getMappingBlueprints().get(0).getProperties().size(), 1);
    assertEquals(yaml1.getMappingBlueprints().get(0).getProperties().get(0).getName(), "k2");
    assertEquals(yaml1.getMappingBlueprints().get(0).getProperties().get(0).getValue(), "v2");
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
