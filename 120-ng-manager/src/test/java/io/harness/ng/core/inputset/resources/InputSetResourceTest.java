package io.harness.ng.core.inputset.resources;

import static io.harness.rule.OwnerRule.NAMAN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;

import com.google.common.io.Resources;

import io.harness.NgManagerTest;
import io.harness.category.element.UnitTests;
import io.harness.cdng.inputset.beans.entities.CDInputSetEntity;
import io.harness.cdng.inputset.beans.entities.CDInputSetEntity.CDInputSetEntityKeys;
import io.harness.cdng.inputset.beans.resource.InputSetResponseDTO;
import io.harness.cdng.inputset.beans.resource.InputSetSummaryResponseDTO;
import io.harness.cdng.inputset.beans.yaml.CDInputSet;
import io.harness.cdng.inputset.mappers.CDInputSetFilterHelper;
import io.harness.cdng.inputset.services.impl.CDInputSetEntityServiceImpl;
import io.harness.rule.Owner;
import io.harness.yaml.utils.YamlPipelineUtils;
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

public class InputSetResourceTest extends NgManagerTest {
  @Mock CDInputSetEntityServiceImpl cdInputSetEntityService;
  @InjectMocks InputSetResource inputSetResource;

  InputSetResponseDTO inputSetResponseDTO;
  InputSetSummaryResponseDTO inputSetSummaryResponseDTO;
  CDInputSetEntity cdInputSetEntity;

  private final String IDENTIFIER = "identifier";
  private final String PIPELINE_IDENTIFIER = "pipeline_identifier";
  private final String ACCOUNT_ID = "account_id";
  private final String ORG_IDENTIFIER = "orgId";
  private final String PROJ_IDENTIFIER = "projId";
  private String inputSetYaml;

  @Before
  public void setUp() throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();

    String inputSetFileName = "input-set-test-file.yaml";
    inputSetYaml =
        Resources.toString(Objects.requireNonNull(classLoader.getResource(inputSetFileName)), StandardCharsets.UTF_8);

    inputSetResponseDTO = InputSetResponseDTO.builder()
                              .accountId(ACCOUNT_ID)
                              .orgIdentifier(ORG_IDENTIFIER)
                              .projectIdentifier(PROJ_IDENTIFIER)
                              .identifier(IDENTIFIER)
                              .name(IDENTIFIER)
                              .pipelineIdentifier(PIPELINE_IDENTIFIER)
                              .inputSetYaml(inputSetYaml)
                              .build();

    inputSetSummaryResponseDTO = InputSetSummaryResponseDTO.builder()
                                     .identifier(IDENTIFIER)
                                     .name(IDENTIFIER)
                                     .pipelineIdentifier(PIPELINE_IDENTIFIER)
                                     .isOverlaySet(false)
                                     .build();

    cdInputSetEntity = CDInputSetEntity.builder()
                           .accountId(ACCOUNT_ID)
                           .orgIdentifier(ORG_IDENTIFIER)
                           .projectIdentifier(PROJ_IDENTIFIER)
                           .identifier(IDENTIFIER)
                           .name(IDENTIFIER)
                           .pipelineIdentifier(PIPELINE_IDENTIFIER)
                           .inputSetYaml(inputSetYaml)
                           .cdInputSet(YamlPipelineUtils.read(inputSetYaml, CDInputSet.class))
                           .build();
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testGet() {
    doReturn(Optional.of(cdInputSetEntity))
        .when(cdInputSetEntityService)
        .get(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, IDENTIFIER, false);

    InputSetResponseDTO inputSetResponseDTO1 =
        inputSetResource.get(IDENTIFIER, ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, false)
            .getData();

    assertThat(inputSetResponseDTO1).isEqualTo(inputSetResponseDTO);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testCreate() {
    doReturn(cdInputSetEntity).when(cdInputSetEntityService).create(any());

    InputSetResponseDTO inputSetResponseDTO1 =
        inputSetResource.create(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, inputSetYaml)
            .getData();

    assertThat(inputSetResponseDTO1).isEqualTo(inputSetResponseDTO);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testDelete() {
    doReturn(true)
        .when(cdInputSetEntityService)
        .delete(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, IDENTIFIER);

    Boolean response =
        inputSetResource.delete(IDENTIFIER, ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER).getData();

    assertThat(response).isTrue();
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testUpdate() {
    doReturn(cdInputSetEntity).when(cdInputSetEntityService).update(any());

    InputSetResponseDTO inputSetResponseDTO1 =
        inputSetResource
            .update(IDENTIFIER, ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, inputSetYaml)
            .getData();

    assertThat(inputSetResponseDTO1).isEqualTo(inputSetResponseDTO);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testUpsert() {
    doReturn(cdInputSetEntity).when(cdInputSetEntityService).upsert(any());

    InputSetResponseDTO inputSetResponseDTO1 =
        inputSetResource
            .upsert(IDENTIFIER, ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, inputSetYaml)
            .getData();

    assertThat(inputSetResponseDTO1).isEqualTo(inputSetResponseDTO);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testListServicesWithDESCSort() {
    Criteria criteria = CDInputSetFilterHelper.createCriteriaForGetList("", "", "", "", false);
    Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, CDInputSetEntityKeys.createdAt));
    final Page<CDInputSetEntity> serviceList = new PageImpl<>(Collections.singletonList(cdInputSetEntity), pageable, 1);
    doReturn(serviceList).when(cdInputSetEntityService).list(criteria, pageable);

    List<InputSetSummaryResponseDTO> content =
        inputSetResource.listInputSetsForPipeline(0, 10, "", "", "", "", null).getData().getContent();

    assertThat(content).isNotNull();
    assertThat(content.size()).isEqualTo(1);
    assertThat(content.get(0)).isEqualTo(inputSetSummaryResponseDTO);
  }
}