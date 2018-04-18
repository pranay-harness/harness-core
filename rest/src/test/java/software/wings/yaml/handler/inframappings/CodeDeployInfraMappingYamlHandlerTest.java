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
import software.wings.beans.CodeDeployInfrastructureMapping;
import software.wings.beans.CodeDeployInfrastructureMapping.Yaml;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.service.impl.yaml.handler.inframapping.CodeDeployInfraMappingYamlHandler;
import software.wings.service.intfc.InfrastructureMappingService;

import java.io.IOException;

public class CodeDeployInfraMappingYamlHandlerTest extends BaseInfraMappingYamlHandlerTest {
  private String validYamlContent = "harnessApiVersion: '1.0'\n"
      + "type: AWS_AWS_CODEDEPLOY\n"
      + "applicationName: CustomTodolist\n"
      + "computeProviderName: aws\n"
      + "computeProviderType: AWS\n"
      + "deploymentConfig: Custom-todolistDC\n"
      + "deploymentGroup: Custom-DeploymentGroup\n"
      + "deploymentType: AWS_CODEDEPLOY\n"
      + "hostNameConvention: '''harness-'' + ${host.ec2Instance.instanceId}'\n"
      + "infraMappingType: AWS_AWS_CODEDEPLOY\n"
      + "region: us-east-1\n"
      + "serviceName: SERVICE_NAME";

  @InjectMocks @Inject private CodeDeployInfraMappingYamlHandler yamlHandler;

  @InjectMocks @Inject private InfrastructureMappingService infrastructureMappingService;

  private String validYamlFilePath =
      "Setup/Applications/APP_NAME/Environments/ENV_NAME/Service Infrastructure/code-deploy.yaml";
  private String infraMappingName = "code-deploy";

  @Before
  public void runBeforeTest() {
    setup(validYamlFilePath, infraMappingName);
  }

  @Test
  public void testCRUDAndGet() throws HarnessException, IOException {
    ChangeContext<Yaml> changeContext = getChangeContext(validYamlContent, validYamlFilePath, yamlHandler);

    Yaml yamlObject = (Yaml) getYaml(validYamlContent, Yaml.class);
    changeContext.setYaml(yamlObject);

    CodeDeployInfrastructureMapping infrastructureMapping =
        yamlHandler.upsertFromYaml(changeContext, asList(changeContext));
    assertNotNull(infrastructureMapping);
    assertEquals(infrastructureMapping.getName(), infraMappingName);

    Yaml yaml = yamlHandler.toYaml(infrastructureMapping, APP_ID);
    assertNotNull(yaml);
    assertEquals(InfrastructureMappingType.AWS_AWS_CODEDEPLOY.name(), yaml.getType());

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

    CodeDeployInfrastructureMapping afterDelete = yamlHandler.get(ACCOUNT_ID, validYamlFilePath);
    assertNull(afterDelete);
  }
}
