/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.templatelibrary;

import static io.harness.rule.OwnerRule.ABHINAV;

import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.CGConstants.GLOBAL_APP_ID;
import static software.wings.common.TemplateConstants.ARTIFACT_SOURCE;
import static software.wings.service.impl.yaml.handler.templatelibrary.TemplateLibaryYamlConstants.ARTIFACT_TEMPLATE_VALID_YAML_FILE_PATH;
import static software.wings.service.impl.yaml.handler.templatelibrary.TemplateLibaryYamlConstants.INVALID_ARTIFACT_TEMPLATE_WITH_VARIABLE;
import static software.wings.service.impl.yaml.handler.templatelibrary.TemplateLibaryYamlConstants.INVALID_HTTP_TEMPLATE_VALID_YAML_FILE_PATH;
import static software.wings.service.impl.yaml.handler.templatelibrary.TemplateLibaryYamlConstants.VALID_ARTIFACT_TEMPLATE_WITH_VARIABLE;
import static software.wings.service.impl.yaml.handler.templatelibrary.TemplateLibaryYamlConstants.artifactTemplateForSetup;
import static software.wings.service.impl.yaml.handler.templatelibrary.TemplateLibaryYamlConstants.artifactTemplateName;
import static software.wings.service.impl.yaml.handler.templatelibrary.TemplateLibaryYamlConstants.expectedArtifactTemplate;
import static software.wings.service.impl.yaml.handler.templatelibrary.TemplateLibaryYamlConstants.rootTemplateFolder;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.beans.template.Template;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.intfc.template.TemplateService;
import software.wings.yaml.templatelibrary.ArtifactSourceTemplateYaml;

import com.google.inject.Inject;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ArtifactSourceTemplateYamlHandlerTest extends TemplateLibraryYamlHandlerTestBase {
  @InjectMocks @Inject private ArtifactSourceTemplateYamlHandler yamlHandler;
  @Mock TemplateService templateService;

  @Before
  public void runBeforeTest() {
    MockitoAnnotations.initMocks(this);
    setup(ARTIFACT_TEMPLATE_VALID_YAML_FILE_PATH, artifactTemplateName);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testCRUDAndGetForTemplate() throws IOException {
    when(yamlHelper.ensureTemplateFolder(
             GLOBAL_ACCOUNT_ID, ARTIFACT_TEMPLATE_VALID_YAML_FILE_PATH, GLOBAL_APP_ID, TEMPLATE_GALLERY_UUID))
        .thenReturn(rootTemplateFolder);
    when(templateService.findByFolder(rootTemplateFolder, artifactTemplateName, GLOBAL_APP_ID))
        .thenReturn(artifactTemplateForSetup);
    when(templateService.update(expectedArtifactTemplate)).thenReturn(expectedArtifactTemplate);

    ChangeContext<ArtifactSourceTemplateYaml> changeContext =
        getChangeContext(VALID_ARTIFACT_TEMPLATE_WITH_VARIABLE, ARTIFACT_TEMPLATE_VALID_YAML_FILE_PATH, yamlHandler);
    ArtifactSourceTemplateYaml yamlObject =
        (ArtifactSourceTemplateYaml) getYaml(VALID_ARTIFACT_TEMPLATE_WITH_VARIABLE, ArtifactSourceTemplateYaml.class);
    changeContext.setYaml(yamlObject);
    changeContext.getChange().setAccountId(GLOBAL_ACCOUNT_ID);
    Template template = yamlHandler.upsertFromYaml(changeContext, asList(changeContext));

    assertThat(template).isNotNull();
    assertThat(template.getTemplateObject()).isNotNull();
    assertThat(template.getName()).isEqualTo(artifactTemplateName);

    ArtifactSourceTemplateYaml yamlString = (ArtifactSourceTemplateYaml) yamlHandler.toYaml(template, GLOBAL_APP_ID);
    assertThat(yamlString).isNotNull();
    assertThat(yamlString.getType()).isEqualTo(ARTIFACT_SOURCE);

    String yamlContent = getYamlContent(yamlString);
    assertThat(yamlContent).isNotNull();
    assertThat(yamlContent).isEqualTo(VALID_ARTIFACT_TEMPLATE_WITH_VARIABLE);

    Template savedTemplate = (Template) yamlHandler.get(GLOBAL_ACCOUNT_ID, ARTIFACT_TEMPLATE_VALID_YAML_FILE_PATH);
    assertThat(savedTemplate).isNotNull();
    assertThat(savedTemplate.getName()).isEqualTo(artifactTemplateName);

    yamlHandler.delete(changeContext);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testFailures() throws IOException {
    testFailures(VALID_ARTIFACT_TEMPLATE_WITH_VARIABLE, ARTIFACT_TEMPLATE_VALID_YAML_FILE_PATH,
        INVALID_ARTIFACT_TEMPLATE_WITH_VARIABLE, INVALID_HTTP_TEMPLATE_VALID_YAML_FILE_PATH, yamlHandler,
        ArtifactSourceTemplateYaml.class);
  }
}
