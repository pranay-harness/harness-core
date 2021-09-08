package io.harness.template.services;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.rule.OwnerRule.ARCHIT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.joor.Reflect.on;

import io.harness.TemplateServiceTestBase;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.encryption.Scope;
import io.harness.exception.InvalidRequestException;
import io.harness.filter.service.FilterService;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.persistance.GitSyncSdkService;
import io.harness.outbox.OutboxEvent;
import io.harness.repositories.NGTemplateRepository;
import io.harness.rule.Owner;
import io.harness.springdata.TransactionHelper;
import io.harness.template.beans.TemplateEntityType;
import io.harness.template.entity.TemplateEntity;
import io.harness.template.entity.TemplateEntity.TemplateEntityKeys;

import com.google.common.io.Resources;
import com.google.inject.Inject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(CDC)
public class NGTemplateServiceImplTest extends TemplateServiceTestBase {
  @Mock FilterService filterService;
  @InjectMocks private NGTemplateServiceHelper templateServiceHelper;
  @Inject private GitSyncSdkService gitSyncSdkService;
  @Inject private NGTemplateRepository templateRepository;
  @Inject private TransactionHelper transactionHelper;

  @InjectMocks NGTemplateServiceImpl templateService;

  private final String ACCOUNT_ID = RandomStringUtils.randomAlphanumeric(6);
  private final String ORG_IDENTIFIER = "orgId";
  private final String PROJ_IDENTIFIER = "projId";
  private final String TEMPLATE_IDENTIFIER = "template1";
  private final String TEMPLATE_VERSION_LABEL = "version1";
  private final String TEMPLATE_CHILD_TYPE = "ShellScript";

  private String yaml;

  TemplateEntity entity;
  TemplateEntity entityWithMongoVersion;
  OutboxEvent outboxEvent = OutboxEvent.builder().build();

  @Before
  public void setUp() throws IOException {
    ClassLoader classLoader = this.getClass().getClassLoader();
    String filename = "template.yaml";
    yaml = Resources.toString(Objects.requireNonNull(classLoader.getResource(filename)), StandardCharsets.UTF_8);
    on(templateService).set("templateRepository", templateRepository);
    on(templateService).set("gitSyncSdkService", gitSyncSdkService);
    on(templateService).set("transactionHelper", transactionHelper);
    on(templateService).set("templateServiceHelper", templateServiceHelper);
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

    entityWithMongoVersion = entity.withVersion(1L);
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testServiceLayer() {
    TemplateEntity createdEntity = templateService.create(entity);
    assertThat(createdEntity).isNotNull();
    assertThat(createdEntity.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(createdEntity.getOrgIdentifier()).isEqualTo(ORG_IDENTIFIER);
    assertThat(createdEntity.getProjectIdentifier()).isEqualTo(PROJ_IDENTIFIER);
    assertThat(createdEntity.getIdentifier()).isEqualTo(TEMPLATE_IDENTIFIER);
    assertThat(createdEntity.getVersion()).isEqualTo(0L);

    Optional<TemplateEntity> optionalTemplateEntity = templateService.get(
        ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, TEMPLATE_IDENTIFIER, TEMPLATE_VERSION_LABEL, false);
    assertThat(optionalTemplateEntity).isPresent();
    assertThat(optionalTemplateEntity.get().getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(optionalTemplateEntity.get().getOrgIdentifier()).isEqualTo(ORG_IDENTIFIER);
    assertThat(optionalTemplateEntity.get().getProjectIdentifier()).isEqualTo(PROJ_IDENTIFIER);
    assertThat(optionalTemplateEntity.get().getIdentifier()).isEqualTo(TEMPLATE_IDENTIFIER);
    assertThat(optionalTemplateEntity.get().getVersionLabel()).isEqualTo(TEMPLATE_VERSION_LABEL);
    assertThat(optionalTemplateEntity.get().getVersion()).isEqualTo(0L);

    String description = "Updated Description";
    TemplateEntity updateTemplate = entity.withDescription(description);
    TemplateEntity updatedTemplateEntity = templateService.updateTemplateEntity(updateTemplate, ChangeType.MODIFY);
    assertThat(updatedTemplateEntity).isNotNull();
    assertThat(updatedTemplateEntity.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(updatedTemplateEntity.getOrgIdentifier()).isEqualTo(ORG_IDENTIFIER);
    assertThat(updatedTemplateEntity.getProjectIdentifier()).isEqualTo(PROJ_IDENTIFIER);
    assertThat(updatedTemplateEntity.getIdentifier()).isEqualTo(TEMPLATE_IDENTIFIER);
    assertThat(updatedTemplateEntity.getVersionLabel()).isEqualTo(TEMPLATE_VERSION_LABEL);
    assertThat(updatedTemplateEntity.getVersion()).isEqualTo(1L);
    assertThat(updatedTemplateEntity.getDescription()).isEqualTo(description);

    TemplateEntity incorrectTemplate = entity.withVersionLabel("incorrect version");
    assertThatThrownBy(() -> templateService.updateTemplateEntity(incorrectTemplate, ChangeType.MODIFY))
        .isInstanceOf(InvalidRequestException.class);

    // Test template list
    Criteria criteria =
        templateServiceHelper.formCriteria(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, null, null, false, "");
    Pageable pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, TemplateEntityKeys.lastUpdatedAt));
    Page<TemplateEntity> templateEntities =
        templateService.list(criteria, pageRequest, ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, false);
    assertThat(templateEntities.getContent()).isNotNull();
    assertThat(templateEntities.getContent().size()).isEqualTo(1);
    assertThat(templateEntities.getContent().get(0).getIdentifier()).isEqualTo(TEMPLATE_IDENTIFIER);

    // Add 1 more entry to template db
    TemplateEntity version2 = entity.withVersionLabel("version2");
    templateService.create(version2);

    templateEntities = templateService.list(criteria, pageRequest, ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, false);
    assertThat(templateEntities.getContent()).isNotNull();
    assertThat(templateEntities.getContent().size()).isEqualTo(2);
    assertThat(templateEntities.getContent().get(0).getIdentifier()).isEqualTo(TEMPLATE_IDENTIFIER);
    assertThat(templateEntities.getContent().get(0).getVersionLabel()).isEqualTo(TEMPLATE_VERSION_LABEL);
    assertThat(templateEntities.getContent().get(1).getVersionLabel()).isEqualTo("version2");

    // Template list with search term
    criteria =
        templateServiceHelper.formCriteria(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, null, null, false, "version2");
    templateEntities = templateService.list(criteria, pageRequest, ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, false);
    assertThat(templateEntities.getContent()).isNotNull();
    assertThat(templateEntities.getContent().size()).isEqualTo(1);
    assertThat(templateEntities.getContent().get(0).getIdentifier()).isEqualTo(TEMPLATE_IDENTIFIER);
    assertThat(templateEntities.getContent().get(0).getVersionLabel()).isEqualTo("version2");

    // Update stable template
    TemplateEntity updateStableTemplateVersion = templateService.updateStableTemplateVersion(
        ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, TEMPLATE_IDENTIFIER, "version2", null);
    assertThat(updateStableTemplateVersion).isNotNull();
    assertThat(updateStableTemplateVersion.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(updateStableTemplateVersion.getOrgIdentifier()).isEqualTo(ORG_IDENTIFIER);
    assertThat(updateStableTemplateVersion.getProjectIdentifier()).isEqualTo(PROJ_IDENTIFIER);
    assertThat(updateStableTemplateVersion.getIdentifier()).isEqualTo(TEMPLATE_IDENTIFIER);
    assertThat(updateStableTemplateVersion.getVersionLabel()).isEqualTo("version2");
    assertThat(updateStableTemplateVersion.getVersion()).isEqualTo(1L);
    assertThat(updateStableTemplateVersion.isStableTemplate()).isTrue();

    // delete template stable template
    assertThatThrownBy(
        () -> templateService.delete(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, TEMPLATE_IDENTIFIER, "version2", 1L))
        .isInstanceOf(InvalidRequestException.class);

    boolean delete = templateService.delete(
        ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, TEMPLATE_IDENTIFIER, TEMPLATE_VERSION_LABEL, null);
    assertThat(delete).isTrue();
  }
}