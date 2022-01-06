/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.template;

import static io.harness.rule.OwnerRule.AADITI;

import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.CGConstants.GLOBAL_APP_ID;
import static software.wings.beans.Variable.VariableBuilder.aVariable;
import static software.wings.beans.VariableType.TEXT;
import static software.wings.common.TemplateConstants.HARNESS_GALLERY;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.exception.WingsException;
import io.harness.rule.Owner;

import software.wings.beans.template.Template;
import software.wings.beans.template.TemplateFolder;
import software.wings.beans.template.TemplateGallery;
import software.wings.beans.template.artifactsource.ArtifactSourceTemplate;
import software.wings.beans.template.artifactsource.CustomArtifactSourceTemplate;
import software.wings.beans.template.artifactsource.CustomRepositoryMapping;
import software.wings.beans.template.artifactsource.CustomRepositoryMapping.AttributeMapping;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class CustomArtifactSourceTemplateTest extends TemplateBaseTestHelper {
  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void shouldSaveCustomArtifactSourceTemplate() {
    Template template = constructCustomArtifactTemplateEntity();
    Template savedTemplate = templateService.save(template);
    assertSavedTemplate(template, savedTemplate);
    ArtifactSourceTemplate savedCustomArtifactStreamTemplate =
        (ArtifactSourceTemplate) savedTemplate.getTemplateObject();
    assertThat(savedCustomArtifactStreamTemplate).isNotNull();
    CustomArtifactSourceTemplate customArtifactSourceTemplate1 =
        (CustomArtifactSourceTemplate) savedCustomArtifactStreamTemplate.getArtifactSource();
    assertThat(customArtifactSourceTemplate1.getScript()).isEqualTo("echo \"hello world\"");
    assertThat(customArtifactSourceTemplate1.getTimeoutSeconds()).isEqualTo("60");
    assertThat(customArtifactSourceTemplate1.getCustomRepositoryMapping()).isNotNull();
  }

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void shouldUpdateCustomArtifactSourceTemplate() {
    Template template = constructCustomArtifactTemplateEntity();
    Template savedTemplate = templateService.save(template);
    assertSavedTemplate(template, savedTemplate);

    ArtifactSourceTemplate savedCustomArtifactStreamTemplate =
        (ArtifactSourceTemplate) savedTemplate.getTemplateObject();
    assertThat(savedCustomArtifactStreamTemplate).isNotNull();

    List<AttributeMapping> attributeMapping = new ArrayList<>();
    attributeMapping.add(
        AttributeMapping.builder().relativePath("assets.downloadUrl").mappedAttribute("metadata.downloadUrl").build());
    CustomRepositoryMapping mapping = CustomRepositoryMapping.builder()
                                          .artifactRoot("$.items")
                                          .buildNoPath("version")
                                          .artifactAttributes(attributeMapping)
                                          .build();
    CustomArtifactSourceTemplate customArtifactSourceTemplate = CustomArtifactSourceTemplate.builder()
                                                                    .script("echo \"hi\"")
                                                                    .timeoutSeconds("100")
                                                                    .customRepositoryMapping(mapping)
                                                                    .build();
    savedTemplate.setTemplateObject(
        ArtifactSourceTemplate.builder().artifactSource(customArtifactSourceTemplate).build());
    Template updatedTemplate = templateService.update(savedTemplate);

    ArtifactSourceTemplate updatedArtifactSourceTemplate = (ArtifactSourceTemplate) updatedTemplate.getTemplateObject();
    assertThat(updatedArtifactSourceTemplate).isNotNull();
    assertThat(updatedTemplate).isNotNull();
    assertThat(updatedTemplate.getAppId()).isNotNull().isEqualTo(GLOBAL_APP_ID);
    assertThat(updatedTemplate.getVersion()).isEqualTo(2L);
    assertThat(updatedTemplate.getTemplateObject()).isNotNull();

    assertThat(((CustomArtifactSourceTemplate) ((ArtifactSourceTemplate) updatedTemplate.getTemplateObject())
                       .getArtifactSource())
                   .getScript())
        .isEqualTo("echo \"hi\"");
    assertThat(((CustomArtifactSourceTemplate) ((ArtifactSourceTemplate) updatedTemplate.getTemplateObject())
                       .getArtifactSource())
                   .getTimeoutSeconds())
        .isEqualTo("100");
    CustomRepositoryMapping customRepositoryMapping =
        ((CustomArtifactSourceTemplate) ((ArtifactSourceTemplate) updatedTemplate.getTemplateObject())
                .getArtifactSource())
            .getCustomRepositoryMapping();
    assertThat(customRepositoryMapping).isNotNull();
    assertThat(customRepositoryMapping.getBuildNoPath()).isEqualTo("version");
    assertThat(customRepositoryMapping.getArtifactAttributes().size()).isEqualTo(1);
    assertThat(customRepositoryMapping.getArtifactAttributes())
        .extracting("relativePath")
        .contains("assets.downloadUrl");
    assertThat(updatedTemplate.getVariables()).extracting("name").contains("var1");
  }

  @Test(expected = WingsException.class)
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void shouldDeleteTemplate() {
    Template template = constructCustomArtifactTemplateEntity();
    Template savedTemplate = templateService.save(template);
    assertSavedTemplate(template, savedTemplate);

    templateService.delete(GLOBAL_ACCOUNT_ID, savedTemplate.getUuid());
    templateService.get(savedTemplate.getUuid());
  }

  private void assertSavedTemplate(Template template, Template savedTemplate) {
    assertThat(savedTemplate).isNotNull();
    assertThat(savedTemplate.getAppId()).isNotNull().isEqualTo(GLOBAL_APP_ID);
    assertThat(savedTemplate.getKeywords()).isNotEmpty();
    assertThat(savedTemplate.getKeywords()).contains(template.getName().toLowerCase());
    assertThat(savedTemplate.getVersion()).isEqualTo(1);
    assertThat(savedTemplate.getVariables()).isNotEmpty();
    assertThat(savedTemplate.getVariables()).extracting("name").contains("var1");
    assertThat(((CustomArtifactSourceTemplate) ((ArtifactSourceTemplate) savedTemplate.getTemplateObject())
                       .getArtifactSource())
                   .getScript())
        .isEqualTo("echo \"hello world\"");
    assertThat(((CustomArtifactSourceTemplate) ((ArtifactSourceTemplate) savedTemplate.getTemplateObject())
                       .getArtifactSource())
                   .getTimeoutSeconds())
        .isEqualTo("60");
    CustomRepositoryMapping customRepositoryMapping =
        ((CustomArtifactSourceTemplate) ((ArtifactSourceTemplate) savedTemplate.getTemplateObject())
                .getArtifactSource())
            .getCustomRepositoryMapping();
    assertThat(customRepositoryMapping.getArtifactAttributes().size()).isEqualTo(1);
    assertThat(customRepositoryMapping.getArtifactAttributes())
        .extracting("relativePath")
        .contains("assets.downloadUrl");
  }

  private Template constructCustomArtifactTemplateEntity() {
    TemplateGallery templateGallery =
        templateGalleryService.getByAccount(GLOBAL_ACCOUNT_ID, templateGalleryService.getAccountGalleryKey());
    TemplateFolder parentFolder =
        templateFolderService.getByFolderPath(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY, templateGallery.getUuid());
    List<AttributeMapping> attributeMapping = new ArrayList<>();
    attributeMapping.add(
        AttributeMapping.builder().relativePath("assets.downloadUrl").mappedAttribute("metadata.downloadUrl").build());
    CustomRepositoryMapping mapping = CustomRepositoryMapping.builder()
                                          .artifactRoot("$.items")
                                          .buildNoPath("name")
                                          .artifactAttributes(attributeMapping)
                                          .build();
    CustomArtifactSourceTemplate customArtifactSourceTemplate =
        CustomArtifactSourceTemplate.builder().script("echo \"hello world\"").customRepositoryMapping(mapping).build();
    return Template.builder()
        .type("ARTIFACT_SOURCE")
        .templateObject(ArtifactSourceTemplate.builder().artifactSource(customArtifactSourceTemplate).build())
        .folderId(parentFolder.getUuid())
        .appId(GLOBAL_APP_ID)
        .accountId(GLOBAL_ACCOUNT_ID)
        .name("Sample Script")
        .variables(asList(aVariable().type(TEXT).name("var1").mandatory(true).build()))
        .build();
  }
}
