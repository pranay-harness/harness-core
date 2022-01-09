/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.infraprovisioner;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.GEORGE;

import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.ff.FeatureFlagService;
import io.harness.rule.Owner;

import software.wings.beans.CloudFormationInfrastructureProvisioner;
import software.wings.beans.CloudFormationInfrastructureProvisioner.Yaml;
import software.wings.beans.Service;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.GitFileChange;
import software.wings.beans.yaml.YamlType;
import software.wings.service.impl.yaml.handler.tag.HarnessTagYamlHelper;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.InfrastructureProvisionerService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.utils.WingsTestConstants;
import software.wings.yaml.handler.YamlHandlerTestBase;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@OwnedBy(CDP)
public class CloudFormationInfrastructureProvisionerYamlHandlerTest extends YamlHandlerTestBase {
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

    Yaml yamlFromObject = handler.toYaml(provisionerSaved, WingsTestConstants.APP_ID);
    String yamlContent = getYamlContent(yamlFromObject);
    assertThat(yamlContent).isEqualTo(validYamlContent);

    CloudFormationInfrastructureProvisioner provisioner = CloudFormationInfrastructureProvisioner.builder()
                                                              .appId(APP_ID)
                                                              .uuid("UUID1")
                                                              .name("Name1")
                                                              .description("Desc1")
                                                              .sourceType("TEMPLATE_BODY")
                                                              .templateBody("Body1")
                                                              .build();

    Yaml yaml1 = handler.toYaml(provisioner, APP_ID);
    assertThat(yaml1).isNotNull();
    assertThat("CLOUD_FORMATION").isEqualTo(yaml1.getType());
    assertThat("1.0").isEqualTo(yaml1.getHarnessApiVersion());
    assertThat("TEMPLATE_BODY").isEqualTo(yaml1.getSourceType());
    assertThat("Body1").isEqualTo(yaml1.getTemplateBody());
    assertThat("Desc1").isEqualTo(yaml1.getDescription());
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
