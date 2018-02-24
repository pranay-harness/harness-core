package software.wings.yaml.handler.inframappings;

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
import software.wings.beans.AwsAmiInfrastructureMapping;
import software.wings.beans.AwsAmiInfrastructureMapping.Yaml;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.service.impl.yaml.handler.inframapping.AwsAmiInfraMappingYamlHandler;
import software.wings.service.intfc.InfrastructureMappingService;

import java.io.IOException;
import java.util.Arrays;

public class AwsAmiInfraMappingYamlHandlerTest extends BaseInfraMappingYamlHandlerTest {
  private String validYamlContent = "region: us-east-1\n"
      + "autoScalingGroupName: Abaris__AMI__Test__deploy__v2__QA__Env__1\n"
      + "classicLoadBalancers:\n"
      + "  - todolist-lb\n"
      + "computeProviderType: AWS\n"
      + "computeProviderName: aws\n"
      + "serviceName: SERVICE_NAME\n"
      + "infraMappingType: AWS_AMI\n"
      + "deploymentType: AMI\n"
      + "harnessApiVersion: '1.0'\n"
      + "type: AWS_AMI";

  @InjectMocks @Inject private AwsAmiInfraMappingYamlHandler yamlHandler;

  @InjectMocks @Inject private InfrastructureMappingService infrastructureMappingService;

  private String validYamlFilePath =
      "Setup/Applications/APP_NAME/Environments/ENV_NAME/Service Infrastructure/aws-ami.yaml";
  private String infraMappingName = "aws-ami";

  @Before
  public void runBeforeTest() {
    setup(validYamlFilePath, infraMappingName);
  }

  @Test
  public void testCRUDAndGet() throws HarnessException, IOException {
    ChangeContext<Yaml> changeContext = getChangeContext(validYamlContent, validYamlFilePath, yamlHandler);

    Yaml yamlObject = (Yaml) getYaml(validYamlContent, Yaml.class, false);
    changeContext.setYaml(yamlObject);

    AwsAmiInfrastructureMapping infrastructureMapping =
        yamlHandler.upsertFromYaml(changeContext, Arrays.asList(changeContext));
    assertNotNull(infrastructureMapping);
    assertEquals(infrastructureMapping.getName(), infraMappingName);

    Yaml yaml = yamlHandler.toYaml(infrastructureMapping, APP_ID);
    assertNotNull(yaml);
    assertEquals(InfrastructureMappingType.AWS_AMI.name(), yaml.getType());

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

    AwsAmiInfrastructureMapping afterDelete = yamlHandler.get(ACCOUNT_ID, validYamlFilePath);
    assertNull(afterDelete);
  }
}
