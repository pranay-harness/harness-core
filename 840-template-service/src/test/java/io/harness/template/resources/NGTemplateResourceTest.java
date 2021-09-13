package io.harness.template.resources;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.rule.OwnerRule.ARCHIT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.doReturn;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.encryption.Scope;
import io.harness.exception.InvalidRequestException;
import io.harness.git.model.ChangeType;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.rule.Owner;
import io.harness.template.beans.TemplateEntityType;
import io.harness.template.beans.TemplateResponseDTO;
import io.harness.template.beans.TemplateSummaryResponseDTO;
import io.harness.template.entity.TemplateEntity;
import io.harness.template.entity.TemplateEntity.TemplateEntityKeys;
import io.harness.template.services.NGTemplateService;
import io.harness.template.services.NGTemplateServiceHelper;

import com.google.common.io.Resources;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@OwnedBy(CDC)
public class NGTemplateResourceTest extends CategoryTest {
  NGTemplateResource templateResource;
  @Mock NGTemplateService templateService;
  @Mock NGTemplateServiceHelper templateServiceHelper;

  private final String ACCOUNT_ID = "account_id";
  private final String ORG_IDENTIFIER = "orgId";
  private final String PROJ_IDENTIFIER = "projId";
  private final String TEMPLATE_IDENTIFIER = "template1";
  private final String TEMPLATE_VERSION_LABEL = "version1";
  private final String TEMPLATE_CHILD_TYPE = "ShellScript";
  private String yaml;

  TemplateEntity entity;
  TemplateEntity entityWithMongoVersion;

  @Before
  public void setUp() throws IOException {
    MockitoAnnotations.initMocks(this);
    templateResource = new NGTemplateResource(templateService, templateServiceHelper);
    ClassLoader classLoader = this.getClass().getClassLoader();
    String filename = "template.yaml";
    yaml = Resources.toString(Objects.requireNonNull(classLoader.getResource(filename)), StandardCharsets.UTF_8);

    entity = TemplateEntity.builder()
                 .accountId(ACCOUNT_ID)
                 .orgIdentifier(ORG_IDENTIFIER)
                 .projectIdentifier(PROJ_IDENTIFIER)
                 .identifier(TEMPLATE_IDENTIFIER)
                 .name(TEMPLATE_IDENTIFIER)
                 .versionLabel(TEMPLATE_VERSION_LABEL)
                 .yaml(yaml)
                 .templateEntityType(TemplateEntityType.STEP_TEMPLATE)
                 .childType(TEMPLATE_CHILD_TYPE)
                 .fullyQualifiedIdentifier("account_id/orgId/projId/template1/version1/")
                 .templateScope(Scope.PROJECT)
                 .build();

    entityWithMongoVersion = TemplateEntity.builder()
                                 .accountId(ACCOUNT_ID)
                                 .orgIdentifier(ORG_IDENTIFIER)
                                 .projectIdentifier(PROJ_IDENTIFIER)
                                 .identifier(TEMPLATE_IDENTIFIER)
                                 .name(TEMPLATE_IDENTIFIER)
                                 .versionLabel(TEMPLATE_VERSION_LABEL)
                                 .yaml(yaml)
                                 .templateEntityType(TemplateEntityType.STEP_TEMPLATE)
                                 .childType(TEMPLATE_CHILD_TYPE)
                                 .fullyQualifiedIdentifier("account_id/orgId/projId/template1/version1/")
                                 .templateScope(Scope.PROJECT)
                                 .version(1L)
                                 .build();
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testCreateTemplate() throws IOException {
    doReturn(entityWithMongoVersion).when(templateService).create(entity);
    ResponseDTO<TemplateResponseDTO> responseDTO =
        templateResource.create(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, null, yaml);
    assertThat(responseDTO.getData()).isNotNull();
    assertThat(responseDTO.getData().getVersion()).isEqualTo(1L);
    assertThat(responseDTO.getData().getIdentifier()).isEqualTo(TEMPLATE_IDENTIFIER);
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testGetTemplate() {
    doReturn(Optional.of(entityWithMongoVersion))
        .when(templateService)
        .get(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, TEMPLATE_IDENTIFIER, TEMPLATE_VERSION_LABEL, false);
    ResponseDTO<TemplateResponseDTO> responseDTO = templateResource.get(
        ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, TEMPLATE_IDENTIFIER, TEMPLATE_VERSION_LABEL, false, null);
    assertThat(responseDTO.getData()).isNotNull();
    assertThat(responseDTO.getData().getVersion()).isEqualTo(1L);
    assertThat(responseDTO.getData().getIdentifier()).isEqualTo(TEMPLATE_IDENTIFIER);
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testGetTemplateWithInvalidTemplateIdentifier() {
    String incorrectPipelineIdentifier = "notTheIdentifierWeNeed";
    doReturn(Optional.empty())
        .when(templateService)
        .get(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, incorrectPipelineIdentifier, TEMPLATE_VERSION_LABEL, false);
    assertThatThrownBy(()
                           -> templateResource.get(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER,
                               incorrectPipelineIdentifier, TEMPLATE_VERSION_LABEL, false, null))
        .isInstanceOf(InvalidRequestException.class);
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testUpdateTemplate() {
    doReturn(entityWithMongoVersion).when(templateService).updateTemplateEntity(entity, ChangeType.MODIFY);
    ResponseDTO<TemplateResponseDTO> responseDTO = templateResource.updateExistingTemplateLabel(
        "", ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, TEMPLATE_IDENTIFIER, TEMPLATE_VERSION_LABEL, null, yaml);
    assertThat(responseDTO.getData()).isNotNull();
    assertThat(responseDTO.getData().getIdentifier()).isEqualTo(TEMPLATE_IDENTIFIER);
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testUpdateStableTemplate() {
    doReturn(entityWithMongoVersion)
        .when(templateService)
        .updateStableTemplateVersion(
            ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, TEMPLATE_IDENTIFIER, TEMPLATE_VERSION_LABEL, null);
    ResponseDTO<String> responseDTO = templateResource.updateStableTemplate(
        ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, TEMPLATE_IDENTIFIER, TEMPLATE_VERSION_LABEL, null);
    assertThat(responseDTO.getData()).isNotNull();
    assertThat(responseDTO.getData()).isEqualTo(TEMPLATE_VERSION_LABEL);
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testUpdateTemplateWithWrongIdentifier() {
    String incorrectPipelineIdentifier = "notTheIdentifierWeNeed";
    assertThatThrownBy(()
                           -> templateResource.updateExistingTemplateLabel("", ACCOUNT_ID, ORG_IDENTIFIER,
                               PROJ_IDENTIFIER, incorrectPipelineIdentifier, TEMPLATE_VERSION_LABEL, null, yaml))
        .isInstanceOf(InvalidRequestException.class);
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testDeleteTemplate() {
    doReturn(true)
        .when(templateService)
        .delete(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, TEMPLATE_IDENTIFIER, TEMPLATE_VERSION_LABEL, null);
    ResponseDTO<Boolean> responseDTO = templateResource.deleteTemplate(
        "", ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, TEMPLATE_IDENTIFIER, TEMPLATE_VERSION_LABEL, null);
    assertThat(responseDTO.getData()).isEqualTo(true);
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testGetListOfTemplates() {
    Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, TemplateEntityKeys.createdAt));
    PageImpl<TemplateEntity> templateEntities =
        new PageImpl<>(Collections.singletonList(entityWithMongoVersion), pageable, 1);
    doReturn(templateEntities).when(templateService).list(any(), any(), any(), any(), any(), anyBoolean());
    List<TemplateSummaryResponseDTO> content =
        templateResource
            .listTemplates(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, 0, 25, null, null, null, null, null, null)
            .getData()
            .getContent();
    assertThat(content).isNotEmpty();
    assertThat(content.size()).isEqualTo(1);

    TemplateSummaryResponseDTO responseDTO = content.get(0);
    assertThat(responseDTO.getIdentifier()).isEqualTo(TEMPLATE_IDENTIFIER);
    assertThat(responseDTO.getName()).isEqualTo(TEMPLATE_IDENTIFIER);
    assertThat(responseDTO.getVersion()).isEqualTo(1L);
  }
}