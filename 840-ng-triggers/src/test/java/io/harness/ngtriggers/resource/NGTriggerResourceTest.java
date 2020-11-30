package io.harness.ngtriggers.resource;

import static io.harness.rule.OwnerRule.NAMAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ngtriggers.beans.config.NGTriggerConfig;
import io.harness.ngtriggers.beans.dto.NGTriggerResponseDTO;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity.NGTriggerEntityKeys;
import io.harness.ngtriggers.beans.entity.metadata.NGTriggerMetadata;
import io.harness.ngtriggers.beans.entity.metadata.WebhookMetadata;
import io.harness.ngtriggers.beans.source.NGTriggerType;
import io.harness.ngtriggers.beans.source.webhook.WebhookTriggerConfig;
import io.harness.ngtriggers.beans.target.TargetType;
import io.harness.ngtriggers.mapper.TriggerFilterHelper;
import io.harness.ngtriggers.service.NGTriggerService;
import io.harness.rule.Owner;
import io.harness.yaml.utils.YamlPipelineUtils;

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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.query.Criteria;

public class NGTriggerResourceTest extends CategoryTest {
  @Mock NGTriggerService ngTriggerService;
  @InjectMocks NGTriggerResource ngTriggerResource;

  private final String IDENTIFIER = "first_trigger";
  private final String NAME = "first trigger";
  private final String PIPELINE_IDENTIFIER = "myPipeline";
  private final String ACCOUNT_ID = "account_id";
  private final String ORG_IDENTIFIER = "orgId";
  private final String PROJ_IDENTIFIER = "projId";
  private String ngTriggerYaml;

  private NGTriggerResponseDTO ngTriggerResponseDTO;
  private NGTriggerEntity ngTriggerEntity;
  private NGTriggerConfig ngTriggerConfig;

  @Before
  public void setUp() throws IOException {
    MockitoAnnotations.initMocks(this);

    ClassLoader classLoader = getClass().getClassLoader();
    String filename = "ng-trigger.yaml";
    ngTriggerYaml =
        Resources.toString(Objects.requireNonNull(classLoader.getResource(filename)), StandardCharsets.UTF_8);

    ngTriggerConfig = YamlPipelineUtils.read(ngTriggerYaml, NGTriggerConfig.class);
    WebhookTriggerConfig webhookTriggerConfig = (WebhookTriggerConfig) ngTriggerConfig.getSource().getSpec();
    WebhookMetadata metadata = WebhookMetadata.builder()
                                   .type(webhookTriggerConfig.getType())
                                   .repoURL(webhookTriggerConfig.getSpec().getRepoUrl())
                                   .build();
    NGTriggerMetadata ngTriggerMetadata = NGTriggerMetadata.builder().webhook(metadata).build();

    ngTriggerResponseDTO = NGTriggerResponseDTO.builder()
                               .accountIdentifier(ACCOUNT_ID)
                               .orgIdentifier(ORG_IDENTIFIER)
                               .projectIdentifier(PROJ_IDENTIFIER)
                               .targetIdentifier(PIPELINE_IDENTIFIER)
                               .identifier(IDENTIFIER)
                               .name(NAME)
                               .yaml(ngTriggerYaml)
                               .type(NGTriggerType.WEBHOOK)
                               .version(0L)
                               .build();

    ngTriggerEntity = NGTriggerEntity.builder()
                          .accountId(ACCOUNT_ID)
                          .orgIdentifier(ORG_IDENTIFIER)
                          .projectIdentifier(PROJ_IDENTIFIER)
                          .targetIdentifier(PIPELINE_IDENTIFIER)
                          .identifier(IDENTIFIER)
                          .name(NAME)
                          .targetType(TargetType.PIPELINE)
                          .type(NGTriggerType.WEBHOOK)
                          .metadata(ngTriggerMetadata)
                          .yaml(ngTriggerYaml)
                          .version(0L)
                          .build();
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testCreate() {
    doReturn(ngTriggerEntity).when(ngTriggerService).create(any());

    NGTriggerResponseDTO responseDTO =
        ngTriggerResource.create(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, ngTriggerYaml).getData();
    assertThat(responseDTO).isEqualTo(ngTriggerResponseDTO);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testGet() {
    doReturn(Optional.of(ngTriggerEntity))
        .when(ngTriggerService)
        .get(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, IDENTIFIER, false);
    NGTriggerResponseDTO responseDTO =
        ngTriggerResource.get(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, IDENTIFIER).getData();
    assertThat(responseDTO).isEqualTo(ngTriggerResponseDTO);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testUpdate() {
    doReturn(ngTriggerEntity).when(ngTriggerService).update(any());

    NGTriggerResponseDTO responseDTO =
        ngTriggerResource.update("0", ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, IDENTIFIER, ngTriggerYaml).getData();

    assertThat(responseDTO).isEqualTo(ngTriggerResponseDTO);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testDelete() {
    doReturn(true)
        .when(ngTriggerService)
        .delete(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, IDENTIFIER, null);

    Boolean response =
        ngTriggerResource.delete(null, ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, IDENTIFIER)
            .getData();

    assertThat(response).isTrue();
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testListServicesWithDESCSort() {
    Criteria criteria = TriggerFilterHelper.createCriteriaForGetList("", "", "", "", null, "", false);
    Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, NGTriggerEntityKeys.createdAt));
    final Page<NGTriggerEntity> serviceList = new PageImpl<>(Collections.singletonList(ngTriggerEntity), pageable, 1);
    doReturn(serviceList).when(ngTriggerService).list(criteria, pageable);

    List<NGTriggerResponseDTO> content =
        ngTriggerResource.getListForTarget("", "", "", "", "", 0, 10, null, "").getData().getContent();

    assertThat(content).isNotNull();
    assertThat(content.size()).isEqualTo(1);
    assertThat(content.get(0)).isEqualTo(ngTriggerResponseDTO);
  }
}