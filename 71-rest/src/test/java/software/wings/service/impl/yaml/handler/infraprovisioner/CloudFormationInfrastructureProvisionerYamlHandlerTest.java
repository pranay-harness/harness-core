package software.wings.service.impl.yaml.handler.infraprovisioner;

import static io.harness.rule.OwnerRule.GEORGE;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static software.wings.api.DeploymentType.SSH;
import static software.wings.beans.InfrastructureMappingBlueprint.NodeFilteringType.AWS_INSTANCE_FILTER;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.beans.BlueprintProperty;
import software.wings.beans.CloudFormationInfrastructureProvisioner;
import software.wings.beans.CloudFormationInfrastructureProvisioner.Yaml;
import software.wings.beans.InfrastructureMappingBlueprint;
import software.wings.beans.InfrastructureMappingBlueprint.CloudProviderType;
import software.wings.beans.Service;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.GitFileChange;
import software.wings.beans.yaml.YamlType;
import software.wings.service.impl.yaml.handler.tag.HarnessTagYamlHelper;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.InfrastructureProvisionerService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.yaml.handler.BaseYamlHandlerTest;

import java.util.Collections;

public class CloudFormationInfrastructureProvisionerYamlHandlerTest extends BaseYamlHandlerTest {
  @Mock private YamlHelper mockYamlHelper;
  @Mock private InfrastructureProvisionerService mockInfrastructureProvisionerService;
  @Mock private ServiceResourceService mockServiceResourceService;
  @Mock private HarnessTagYamlHelper harnessTagYamlHelper;
  @Mock private AppService mockAppService;
  @Mock private FeatureFlagService mockFeatureFlagService;

  @InjectMocks @Inject private CloudFormationInfrastructureProvisionerYamlHandler handler;

  private String validYamlFilePath = "Setup/Applications/APP_NAME/Infrastructure Provisioners/CF_Name.yaml";
  private String validYamlContent = "harnessApiVersion: '1.0'\n"
      + "type: CLOUD_FORMATION\n"
      + "description: Desc\n"
      + "mappingBlueprints:\n"
      + "- cloudProviderType: AWS\n"
      + "  deploymentType: SSH\n"
      + "  nodeFilteringType: AWS_INSTANCE_FILTER\n"
      + "  properties:\n"
      + "  - name: k1\n"
      + "    value: v1\n"
      + "  serviceName: ServiceName\n"
      + "name: Name\n"
      + "sourceType: TEMPLATE_BODY\n"
      + "templateBody: Body\n";

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testCRUDAndGet() throws Exception {
    ChangeContext<Yaml> changeContext = getChangeContext();
    Yaml yaml = (Yaml) getYaml(validYamlContent, Yaml.class);
    changeContext.setYaml(yaml);
    doReturn(APP_ID).when(mockYamlHelper).getAppId(anyString(), anyString());
    doReturn(null).when(mockInfrastructureProvisionerService).getByName(anyString(), anyString());
    Service service = Service.builder().name("ServiceName").uuid(SERVICE_ID).build();
    doReturn(service).when(mockServiceResourceService).get(anyString(), anyString());
    doReturn(service).when(mockServiceResourceService).getServiceByName(anyString(), anyString());

    handler.upsertFromYaml(changeContext, asList(changeContext));
    ArgumentCaptor<CloudFormationInfrastructureProvisioner> captor =
        ArgumentCaptor.forClass(CloudFormationInfrastructureProvisioner.class);
    verify(mockInfrastructureProvisionerService).save(captor.capture());
    CloudFormationInfrastructureProvisioner provisionerSaved = captor.getValue();
    assertThat(provisionerSaved).isNotNull();
    assertThat("CLOUD_FORMATION").isEqualTo(provisionerSaved.getInfrastructureProvisionerType());
    assertThat("TEMPLATE_BODY").isEqualTo(provisionerSaved.getSourceType());
    assertThat("Body").isEqualTo(provisionerSaved.getTemplateBody());
    assertThat("Desc").isEqualTo(provisionerSaved.getDescription());
    assertThat(APP_ID).isEqualTo(provisionerSaved.getAppId());
    assertThat(1).isEqualTo(provisionerSaved.getMappingBlueprints().size());
    assertThat(SERVICE_ID).isEqualTo(provisionerSaved.getMappingBlueprints().get(0).getServiceId());
    assertThat("AWS").isEqualTo(provisionerSaved.getMappingBlueprints().get(0).getCloudProviderType().name());
    assertThat(SSH).isEqualTo(provisionerSaved.getMappingBlueprints().get(0).getDeploymentType());
    assertThat(1).isEqualTo(provisionerSaved.getMappingBlueprints().get(0).getProperties().size());
    assertThat("k1").isEqualTo(provisionerSaved.getMappingBlueprints().get(0).getProperties().get(0).getName());
    assertThat("v1").isEqualTo(provisionerSaved.getMappingBlueprints().get(0).getProperties().get(0).getValue());

    CloudFormationInfrastructureProvisioner provisioner =
        CloudFormationInfrastructureProvisioner.builder()
            .appId(APP_ID)
            .uuid("UUID1")
            .name("Name1")
            .description("Desc1")
            .sourceType("TEMPLATE_BODY")
            .templateBody("Body1")
            .mappingBlueprints(Collections.singletonList(
                InfrastructureMappingBlueprint.builder()
                    .cloudProviderType(CloudProviderType.AWS)
                    .deploymentType(SSH)
                    .nodeFilteringType(AWS_INSTANCE_FILTER)
                    .serviceId(SERVICE_ID)
                    .properties(asList(BlueprintProperty.builder().name("k2").value("v2").build()))
                    .build()))
            .build();

    Yaml yaml1 = handler.toYaml(provisioner, APP_ID);
    assertThat(yaml1).isNotNull();
    assertThat("CLOUD_FORMATION").isEqualTo(yaml1.getType());
    assertThat("1.0").isEqualTo(yaml1.getHarnessApiVersion());
    assertThat("TEMPLATE_BODY").isEqualTo(yaml1.getSourceType());
    assertThat("Body1").isEqualTo(yaml1.getTemplateBody());
    assertThat("Name1").isEqualTo(yaml1.getName());
    assertThat("Desc1").isEqualTo(yaml1.getDescription());
    assertThat(1).isEqualTo(yaml1.getMappingBlueprints().size());
    assertThat("ServiceName").isEqualTo(yaml1.getMappingBlueprints().get(0).getServiceName());
    assertThat(SSH).isEqualTo(yaml1.getMappingBlueprints().get(0).getDeploymentType());
    assertThat(CloudProviderType.AWS).isEqualTo(yaml1.getMappingBlueprints().get(0).getCloudProviderType());
    assertThat(AWS_INSTANCE_FILTER).isEqualTo(yaml1.getMappingBlueprints().get(0).getNodeFilteringType());
    assertThat(1).isEqualTo(yaml1.getMappingBlueprints().get(0).getProperties().size());
    assertThat("k2").isEqualTo(yaml1.getMappingBlueprints().get(0).getProperties().get(0).getName());
    assertThat("v2").isEqualTo(yaml1.getMappingBlueprints().get(0).getProperties().get(0).getValue());
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