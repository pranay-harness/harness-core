package io.harness.ngpipeline.overlayinputset.resource;

import static io.harness.rule.OwnerRule.NAMAN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;

import com.google.common.io.Resources;

import io.harness.NGPipelineBaseTest;
import io.harness.category.element.UnitTests;
import io.harness.ngpipeline.overlayinputset.beans.entities.OverlayInputSetEntity;
import io.harness.ngpipeline.overlayinputset.beans.entities.OverlayInputSetEntity.OverlayInputSetEntityKeys;
import io.harness.ngpipeline.overlayinputset.beans.resource.OverlayInputSetResponseDTO;
import io.harness.ngpipeline.overlayinputset.mappers.OverlayInputSetFilterHelper;
import io.harness.ngpipeline.overlayinputset.services.impl.OverlayInputSetEntityServiceImpl;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class OverlayInputSetResourceTest extends NGPipelineBaseTest {
  @Mock OverlayInputSetEntityServiceImpl overlayInputSetEntityService;
  @InjectMocks OverlayInputSetResource inputSetResource;

  OverlayInputSetResponseDTO overlayInputSetResponseDTO;
  OverlayInputSetEntity overlayInputSetEntity;

  private final String IDENTIFIER = "identifier";
  private final String PIPELINE_IDENTIFIER = "pipeline_identifier";
  private final String ACCOUNT_ID = "account_id";
  private final String ORG_IDENTIFIER = "orgId";
  private final String PROJ_IDENTIFIER = "projId";
  private final String NAME = "input set name";
  private final String DESCRIPTION = "input set description";
  private String inputSetYaml;

  @Before
  public void setUp() throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();

    String inputSetFileName = "overlay-input-set-test-file.yaml";
    inputSetYaml =
        Resources.toString(Objects.requireNonNull(classLoader.getResource(inputSetFileName)), StandardCharsets.UTF_8);

    overlayInputSetResponseDTO = OverlayInputSetResponseDTO.builder()
                                     .accountId(ACCOUNT_ID)
                                     .orgIdentifier(ORG_IDENTIFIER)
                                     .projectIdentifier(PROJ_IDENTIFIER)
                                     .identifier(IDENTIFIER)
                                     .name(NAME)
                                     .description(DESCRIPTION)
                                     .pipelineIdentifier(PIPELINE_IDENTIFIER)
                                     .overlayInputSetYaml(inputSetYaml)
                                     .build();

    overlayInputSetEntity = OverlayInputSetEntity.builder()
                                .accountId(ACCOUNT_ID)
                                .orgIdentifier(ORG_IDENTIFIER)
                                .projectIdentifier(PROJ_IDENTIFIER)
                                .identifier(IDENTIFIER)
                                .pipelineIdentifier(PIPELINE_IDENTIFIER)
                                .overlayInputSetYaml(inputSetYaml)
                                .name(NAME)
                                .description(DESCRIPTION)
                                .build();
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testGet() {
    doReturn(Optional.of(overlayInputSetEntity))
        .when(overlayInputSetEntityService)
        .get(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, IDENTIFIER, false);

    OverlayInputSetResponseDTO inputSetResponseDTO1 =
        inputSetResource.get(IDENTIFIER, ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, false)
            .getData();

    assertThat(inputSetResponseDTO1).isEqualTo(overlayInputSetResponseDTO);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testCreate() {
    doReturn(overlayInputSetEntity).when(overlayInputSetEntityService).create(any());

    OverlayInputSetResponseDTO inputSetResponseDTO1 =
        inputSetResource.create(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, inputSetYaml)
            .getData();

    assertThat(inputSetResponseDTO1).isEqualTo(overlayInputSetResponseDTO);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testDelete() {
    doReturn(true)
        .when(overlayInputSetEntityService)
        .delete(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, IDENTIFIER);

    Boolean response =
        inputSetResource.delete(IDENTIFIER, ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER).getData();

    assertThat(response).isTrue();
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testUpdate() {
    doReturn(overlayInputSetEntity).when(overlayInputSetEntityService).update(any());

    OverlayInputSetResponseDTO inputSetResponseDTO1 =
        inputSetResource
            .update(IDENTIFIER, ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, inputSetYaml)
            .getData();

    assertThat(inputSetResponseDTO1).isEqualTo(overlayInputSetResponseDTO);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testUpsert() {
    doReturn(overlayInputSetEntity).when(overlayInputSetEntityService).upsert(any());

    OverlayInputSetResponseDTO inputSetResponseDTO1 =
        inputSetResource
            .upsert(IDENTIFIER, ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, inputSetYaml)
            .getData();

    assertThat(inputSetResponseDTO1).isEqualTo(overlayInputSetResponseDTO);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testListServicesWithDESCSort() {
    Criteria criteria = OverlayInputSetFilterHelper.createCriteriaForGetList("", "", "", "", false);
    Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, OverlayInputSetEntityKeys.createdAt));
    final Page<OverlayInputSetEntity> serviceList =
        new PageImpl<>(Collections.singletonList(overlayInputSetEntity), pageable, 1);
    doReturn(serviceList).when(overlayInputSetEntityService).list(criteria, pageable);

    List<OverlayInputSetResponseDTO> content =
        inputSetResource.listInputSetsForPipeline(0, 10, "", "", "", "", null).getData().getContent();

    assertThat(content).isNotNull();
    assertThat(content.size()).isEqualTo(1);
    assertThat(content.get(0)).isEqualTo(overlayInputSetResponseDTO);
  }
}