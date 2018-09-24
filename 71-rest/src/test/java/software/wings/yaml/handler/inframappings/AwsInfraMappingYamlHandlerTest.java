package software.wings.yaml.handler.inframappings;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;

import com.google.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.AwsInfrastructureMapping.Yaml;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.service.impl.yaml.handler.inframapping.AwsInfraMappingYamlHandler;
import software.wings.service.intfc.InfrastructureMappingService;

import java.io.IOException;

public class AwsInfraMappingYamlHandlerTest extends BaseInfraMappingYamlHandlerTest {
  private String validYamlContent1 = "harnessApiVersion: '1.0'\n"
      + "type: AWS_SSH\n"
      + "computeProviderName: aws\n"
      + "computeProviderType: AWS\n"
      + "connectionType: Wings Key\n"
      + "deploymentType: SSH\n"
      + "desiredCapacity: 0\n"
      + "hostNameConvention: ${host.ec2Instance.privateDnsName.split('.')[0]}\n"
      + "infraMappingType: AWS_SSH\n"
      + "provisionInstances: false\n"
      + "region: us-east-1\n"
      + "serviceName: SERVICE_NAME\n"
      + "usePublicDns: true";

  private String validYamlContent2 = "harnessApiVersion: '1.0'\n"
      + "type: AWS_SSH\n"
      + "computeProviderName: aws\n"
      + "computeProviderType: AWS\n"
      + "connectionType: Wings Key\n"
      + "deploymentType: SSH\n"
      + "desiredCapacity: 0\n"
      + "hostNameConvention: ${host.ec2Instance.privateDnsName.split('.')[0]}\n"
      + "infraMappingType: AWS_SSH\n"
      + "provisionInstances: false\n"
      + "provisionerName: PROVISIONER_NAME\n"
      + "serviceName: SERVICE_NAME\n"
      + "usePublicDns: true";

  @InjectMocks @Inject private AwsInfraMappingYamlHandler yamlHandler;

  @InjectMocks @Inject private InfrastructureMappingService infrastructureMappingService;

  private String validYamlFilePath =
      "Setup/Applications/APP_NAME/Environments/ENV_NAME/Service Infrastructure/aws.yaml";
  private String infraMappingName = "aws";

  @Before
  public void runBeforeTest() {
    setup(validYamlFilePath, infraMappingName);
  }

  public void testCRUDAndGet(String validYamlContent) throws HarnessException, IOException {
    ChangeContext<Yaml> changeContext = getChangeContext(validYamlContent, validYamlFilePath, yamlHandler);

    Yaml yamlObject = (Yaml) getYaml(validYamlContent, Yaml.class);
    changeContext.setYaml(yamlObject);

    AwsInfrastructureMapping infrastructureMapping = yamlHandler.upsertFromYaml(changeContext, asList(changeContext));
    assertNotNull(infrastructureMapping);
    assertEquals(infrastructureMapping.getName(), infraMappingName);

    Yaml yaml = yamlHandler.toYaml(infrastructureMapping, APP_ID);
    assertNotNull(yaml);
    assertEquals(InfrastructureMappingType.AWS_SSH.name(), yaml.getType());

    String yamlContent = getYamlContent(yaml);
    assertNotNull(yamlContent);
    yamlContent = yamlContent.substring(0, yamlContent.length() - 1);
    assertEquals(validYamlContent, yamlContent);

    InfrastructureMapping infrastructureMapping2 =
        infrastructureMappingService.getInfraMappingByName(APP_ID, ENV_ID, infraMappingName);
    // TODO find out why this couldn't be called
    //    Workflow savedWorkflow = yamlHandler.get(ACCOUNT_ID, validYamlFilePath);
    assertThat(infrastructureMapping2).isNotNull().hasFieldOrPropertyWithValue("name", infraMappingName);

    yamlHandler.delete(changeContext);

    AwsInfrastructureMapping afterDelete = yamlHandler.get(ACCOUNT_ID, validYamlFilePath);
    assertNull(afterDelete);
  }

  @Test
  public void testCRUDAndGet1() throws HarnessException, IOException {
    testCRUDAndGet(validYamlContent1);
  }

  @Test
  public void testCRUDAndGet2() throws HarnessException, IOException {
    testCRUDAndGet(validYamlContent2);
  }
}
