/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.yaml.handler.inframappings;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.GEORGE;

import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.AwsInfrastructureMapping.Yaml;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.yaml.handler.inframapping.AwsInfraMappingYamlHandler;
import software.wings.service.intfc.InfrastructureMappingService;

import com.google.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;

@OwnedBy(CDP)
public class AwsInfraMappingYamlHandlerTest extends BaseInfraMappingYamlHandlerTestBase {
  private String validYamlContent1 = "harnessApiVersion: '1.0'\n"
      + "type: AWS_SSH\n"
      + "computeProviderName: aws\n"
      + "computeProviderType: AWS\n"
      + "connectionType: Wings Key\n"
      + "deploymentType: SSH\n"
      + "desiredCapacity: 0\n"
      + "hostConnectionType: PUBLIC_DNS\n"
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
      + "hostConnectionType: PUBLIC_DNS\n"
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

  public void testCRUDAndGet(String validYamlContent) throws Exception {
    ChangeContext<Yaml> changeContext = getChangeContext(validYamlContent, validYamlFilePath, yamlHandler);

    Yaml yamlObject = (Yaml) getYaml(validYamlContent, Yaml.class);
    changeContext.setYaml(yamlObject);

    AwsInfrastructureMapping infrastructureMapping = yamlHandler.upsertFromYaml(changeContext, asList(changeContext));
    assertThat(infrastructureMapping).isNotNull();
    assertThat(infraMappingName).isEqualTo(infrastructureMapping.getName());

    Yaml yaml = yamlHandler.toYaml(infrastructureMapping, APP_ID);
    assertThat(yaml).isNotNull();
    assertThat(yaml.getType()).isEqualTo(InfrastructureMappingType.AWS_SSH.name());

    String yamlContent = getYamlContent(yaml);
    assertThat(yamlContent).isNotNull();
    yamlContent = yamlContent.substring(0, yamlContent.length() - 1);
    assertThat(yamlContent).isEqualTo(validYamlContent);

    InfrastructureMapping infrastructureMapping2 =
        infrastructureMappingService.getInfraMappingByName(APP_ID, ENV_ID, infraMappingName);
    // TODO find out why this couldn't be called
    //    Workflow savedWorkflow = yamlHandler.get(ACCOUNT_ID, validYamlFilePath);
    assertThat(infrastructureMapping2).isNotNull().hasFieldOrPropertyWithValue("name", infraMappingName);

    yamlHandler.delete(changeContext);

    AwsInfrastructureMapping afterDelete = yamlHandler.get(ACCOUNT_ID, validYamlFilePath);
    assertThat(afterDelete).isNull();
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testCRUDAndGet1() throws Exception {
    testCRUDAndGet(validYamlContent1);
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testCRUDAndGet2() throws Exception {
    testCRUDAndGet(validYamlContent2);
  }
}
