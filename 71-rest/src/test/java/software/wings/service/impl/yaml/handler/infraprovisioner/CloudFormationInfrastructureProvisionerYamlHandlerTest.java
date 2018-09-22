package software.wings.service.impl.yaml.handler.infraprovisioner;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static software.wings.api.DeploymentType.SSH;
import static software.wings.beans.InfrastructureMappingBlueprint.NodeFilteringType.AWS_INSTANCE_FILTER;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;

import com.google.inject.Inject;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.beans.CloudFormationInfrastructureProvisioner;
import software.wings.beans.CloudFormationInfrastructureProvisioner.Yaml;
import software.wings.beans.InfrastructureMappingBlueprint;
import software.wings.beans.InfrastructureMappingBlueprint.CloudProviderType;
import software.wings.beans.NameValuePair;
import software.wings.beans.Service;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.GitFileChange;
import software.wings.beans.yaml.YamlType;
import software.wings.exception.HarnessException;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.InfrastructureProvisionerService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.yaml.handler.BaseYamlHandlerTest;

import java.io.IOException;
import java.util.Collections;

public class CloudFormationInfrastructureProvisionerYamlHandlerTest extends BaseYamlHandlerTest {
  @Mock private YamlHelper mockYamlHelper;
  @Mock private InfrastructureProvisionerService mockInfrastructureProvisionerService;
  @Mock private ServiceResourceService mockServiceResourceService;
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
  public void testCRUDAndGet() throws HarnessException, IOException {
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
    assertNotNull(provisionerSaved);
    assertEquals(provisionerSaved.getInfrastructureProvisionerType(), "CLOUD_FORMATION");
    assertEquals(provisionerSaved.getSourceType(), "TEMPLATE_BODY");
    assertEquals(provisionerSaved.getTemplateBody(), "Body");
    assertEquals(provisionerSaved.getDescription(), "Desc");
    assertEquals(provisionerSaved.getAppId(), APP_ID);
    assertEquals(provisionerSaved.getMappingBlueprints().size(), 1);
    assertEquals(provisionerSaved.getMappingBlueprints().get(0).getServiceId(), SERVICE_ID);
    assertEquals(provisionerSaved.getMappingBlueprints().get(0).getCloudProviderType().name(), "AWS");
    assertEquals(provisionerSaved.getMappingBlueprints().get(0).getDeploymentType(), SSH);
    assertEquals(provisionerSaved.getMappingBlueprints().get(0).getProperties().size(), 1);
    assertEquals(provisionerSaved.getMappingBlueprints().get(0).getProperties().get(0).getName(), "k1");
    assertEquals(provisionerSaved.getMappingBlueprints().get(0).getProperties().get(0).getValue(), "v1");

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
                    .properties(asList(NameValuePair.builder().name("k2").value("v2").build()))
                    .build()))
            .build();

    Yaml yaml1 = handler.toYaml(provisioner, APP_ID);
    assertNotNull(yaml1);
    assertEquals(yaml1.getType(), "CLOUD_FORMATION");
    assertEquals(yaml1.getHarnessApiVersion(), "1.0");
    assertEquals(yaml1.getSourceType(), "TEMPLATE_BODY");
    assertEquals(yaml1.getTemplateBody(), "Body1");
    assertEquals(yaml1.getName(), "Name1");
    assertEquals(yaml1.getDescription(), "Desc1");
    assertEquals(yaml1.getMappingBlueprints().size(), 1);
    assertEquals(yaml1.getMappingBlueprints().get(0).getServiceName(), "ServiceName");
    assertEquals(yaml1.getMappingBlueprints().get(0).getDeploymentType(), SSH);
    assertEquals(yaml1.getMappingBlueprints().get(0).getCloudProviderType(), CloudProviderType.AWS);
    assertEquals(yaml1.getMappingBlueprints().get(0).getNodeFilteringType(), AWS_INSTANCE_FILTER);
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