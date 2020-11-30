package io.harness.ngtriggers.service.impl;

import static io.harness.rule.OwnerRule.NAMAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.category.element.UnitTests;
import io.harness.cdng.CDNGBaseTest;
import io.harness.exception.InvalidRequestException;
import io.harness.ngtriggers.beans.config.NGTriggerConfig;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;
import io.harness.ngtriggers.beans.entity.metadata.NGTriggerMetadata;
import io.harness.ngtriggers.beans.entity.metadata.WebhookMetadata;
import io.harness.ngtriggers.beans.source.webhook.WebhookTriggerConfig;
import io.harness.ngtriggers.mapper.TriggerFilterHelper;
import io.harness.rule.Owner;
import io.harness.utils.PageUtils;
import io.harness.yaml.utils.YamlPipelineUtils;

import com.google.common.io.Resources;
import com.google.inject.Inject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

public class NGTriggerServiceImplTest extends CDNGBaseTest {
  @Inject NGTriggerServiceImpl ngTriggerService;

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testServiceLayer() throws IOException {
    String ACCOUNT_ID = "account_id";
    String ORG_IDENTIFIER = "orgId";
    String PROJ_IDENTIFIER = "projId";
    String PIPELINE_IDENTIFIER = "myPipeline";
    String IDENTIFIER = "first_trigger";
    String NAME = "first trigger";

    ClassLoader classLoader = getClass().getClassLoader();
    String filename = "ng-trigger.yaml";
    String ngTriggerYaml =
        Resources.toString(Objects.requireNonNull(classLoader.getResource(filename)), StandardCharsets.UTF_8);

    NGTriggerConfig ngTriggerConfig = YamlPipelineUtils.read(ngTriggerYaml, NGTriggerConfig.class);
    WebhookTriggerConfig webhookTriggerConfig = (WebhookTriggerConfig) ngTriggerConfig.getSource().getSpec();
    WebhookMetadata metadata = WebhookMetadata.builder()
                                   .type(webhookTriggerConfig.getType())
                                   .repoURL(webhookTriggerConfig.getSpec().getRepoUrl())
                                   .build();
    NGTriggerMetadata ngTriggerMetadata = NGTriggerMetadata.builder().webhook(metadata).build();

    // Create
    NGTriggerEntity ngTriggerEntity = NGTriggerEntity.builder().build();
    ngTriggerEntity.setAccountId(ACCOUNT_ID);
    ngTriggerEntity.setOrgIdentifier(ORG_IDENTIFIER);
    ngTriggerEntity.setProjectIdentifier(PROJ_IDENTIFIER);
    ngTriggerEntity.setTargetIdentifier(PIPELINE_IDENTIFIER);
    ngTriggerEntity.setIdentifier(IDENTIFIER);
    ngTriggerEntity.setYaml(ngTriggerYaml);
    ngTriggerEntity.setMetadata(ngTriggerMetadata);
    ngTriggerEntity.setName(NAME);

    NGTriggerEntity createdEntity = ngTriggerService.create(ngTriggerEntity);
    assertThat(createdEntity).isNotNull();
    assertThat(createdEntity.getAccountId()).isEqualTo(ngTriggerEntity.getAccountId());
    assertThat(createdEntity.getOrgIdentifier()).isEqualTo(ngTriggerEntity.getOrgIdentifier());
    assertThat(createdEntity.getProjectIdentifier()).isEqualTo(ngTriggerEntity.getProjectIdentifier());
    assertThat(createdEntity.getIdentifier()).isEqualTo(ngTriggerEntity.getIdentifier());
    assertThat(createdEntity.getName()).isEqualTo(ngTriggerEntity.getName());
    assertThat(createdEntity.getVersion()).isEqualTo(0L);

    // Get
    Optional<NGTriggerEntity> getTriggerEntity =
        ngTriggerService.get(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, IDENTIFIER, false);
    assertThat(getTriggerEntity).isPresent();
    assertThat(getTriggerEntity.get().getAccountId()).isEqualTo(createdEntity.getAccountId());
    assertThat(getTriggerEntity.get().getOrgIdentifier()).isEqualTo(createdEntity.getOrgIdentifier());
    assertThat(getTriggerEntity.get().getProjectIdentifier()).isEqualTo(createdEntity.getProjectIdentifier());
    assertThat(getTriggerEntity.get().getIdentifier()).isEqualTo(createdEntity.getIdentifier());
    assertThat(getTriggerEntity.get().getName()).isEqualTo(createdEntity.getName());
    assertThat(getTriggerEntity.get().getVersion()).isEqualTo(0L);

    // Update
    NGTriggerEntity newEntity = NGTriggerEntity.builder().build();
    newEntity.setAccountId(ACCOUNT_ID);
    newEntity.setOrgIdentifier(ORG_IDENTIFIER);
    newEntity.setProjectIdentifier(PROJ_IDENTIFIER);
    newEntity.setTargetIdentifier(PIPELINE_IDENTIFIER);
    newEntity.setIdentifier(IDENTIFIER);
    newEntity.setYaml(ngTriggerYaml);
    newEntity.setName(NAME);
    ngTriggerEntity.setMetadata(ngTriggerMetadata);
    newEntity.setDescription("Added a description");

    NGTriggerEntity updatedEntity = ngTriggerService.update(newEntity);
    assertThat(updatedEntity.getAccountId()).isEqualTo(newEntity.getAccountId());
    assertThat(updatedEntity.getOrgIdentifier()).isEqualTo(newEntity.getOrgIdentifier());
    assertThat(updatedEntity.getProjectIdentifier()).isEqualTo(newEntity.getProjectIdentifier());
    assertThat(updatedEntity.getIdentifier()).isEqualTo(newEntity.getIdentifier());
    assertThat(updatedEntity.getName()).isEqualTo(newEntity.getName());
    assertThat(updatedEntity.getDescription()).isEqualTo(newEntity.getDescription());
    assertThat(updatedEntity.getVersion()).isEqualTo(1L);

    // Update non existing entity
    newEntity.setAccountId("newAccountID");
    assertThatThrownBy(() -> ngTriggerService.update(newEntity)).isInstanceOf(InvalidRequestException.class);

    newEntity.setAccountId(ACCOUNT_ID);

    // List for target
    Criteria criteriaFromFilter = TriggerFilterHelper.createCriteriaForGetList(
        ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, null, "", false);
    Pageable pageRequest = PageUtils.getPageRequest(0, 10, null);
    Page<NGTriggerEntity> list = ngTriggerService.list(criteriaFromFilter, pageRequest);

    assertThat(list.getContent()).isNotNull();
    assertThat(list.getContent().size()).isEqualTo(1);

    // Delete with wrong version
    assertThatThrownBy(
        () -> ngTriggerService.delete(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, IDENTIFIER, 0L))
        .isInstanceOf(InvalidRequestException.class);

    // Delete
    boolean delete =
        ngTriggerService.delete(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, IDENTIFIER, 1L);
    assertThat(delete).isTrue();
    getTriggerEntity =
        ngTriggerService.get(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, IDENTIFIER, false);
    assertThat(getTriggerEntity.isPresent()).isFalse();
  }
}