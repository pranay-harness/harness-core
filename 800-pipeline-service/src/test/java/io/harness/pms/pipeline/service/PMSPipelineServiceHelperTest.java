package io.harness.pms.pipeline.service;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.rule.OwnerRule.NAMAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.filter.FilterType;
import io.harness.filter.dto.FilterDTO;
import io.harness.filter.service.FilterService;
import io.harness.ng.core.common.beans.NGTag;
import io.harness.pms.filter.creation.FilterCreatorMergeService;
import io.harness.pms.filter.creation.FilterCreatorMergeServiceResponse;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.PipelineEntity.PipelineEntityKeys;
import io.harness.pms.pipeline.PipelineFilterPropertiesDto;
import io.harness.rule.Owner;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(PIPELINE)
public class PMSPipelineServiceHelperTest extends CategoryTest {
  PMSPipelineServiceHelper pmsPipelineServiceHelper;
  @Mock FilterService filterService;
  @Mock FilterCreatorMergeService filterCreatorMergeService;

  String accountIdentifier = "account";
  String orgIdentifier = "org";
  String projectIdentifier = "project";
  String pipelineIdentifier = "pipeline";

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    pmsPipelineServiceHelper = new PMSPipelineServiceHelper(filterService, filterCreatorMergeService);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testValidatePresenceOfRequiredFields() {
    assertThatThrownBy(() -> PMSPipelineServiceHelper.validatePresenceOfRequiredFields("", null, "2"))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("One of the required fields is null.");
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testGetPipelineEqualityCriteria() {
    PipelineEntity entity = PipelineEntity.builder()
                                .accountId(accountIdentifier)
                                .orgIdentifier(orgIdentifier)
                                .projectIdentifier(projectIdentifier)
                                .identifier(pipelineIdentifier)
                                .build();

    Criteria criteria1 = PMSPipelineServiceHelper.getPipelineEqualityCriteria(entity, false);
    assertThat(criteria1).isNotNull();
    Document criteria1Object = criteria1.getCriteriaObject();
    assertThat(criteria1Object.get(PipelineEntityKeys.accountId)).isEqualTo(accountIdentifier);
    assertThat(criteria1Object.get(PipelineEntityKeys.orgIdentifier)).isEqualTo(orgIdentifier);
    assertThat(criteria1Object.get(PipelineEntityKeys.projectIdentifier)).isEqualTo(projectIdentifier);
    assertThat(criteria1Object.get(PipelineEntityKeys.identifier)).isEqualTo(pipelineIdentifier);
    assertThat(criteria1Object.get(PipelineEntityKeys.deleted)).isEqualTo(false);
    assertThat(criteria1Object.get(PipelineEntityKeys.version)).isNull();

    Criteria criteria2 = PMSPipelineServiceHelper.getPipelineEqualityCriteria(
        accountIdentifier, orgIdentifier, projectIdentifier, pipelineIdentifier, false, null);
    assertThat(criteria2).isNotNull();
    Document criteria2Object = criteria1.getCriteriaObject();
    assertThat(criteria2Object.get(PipelineEntityKeys.accountId)).isEqualTo(accountIdentifier);
    assertThat(criteria2Object.get(PipelineEntityKeys.orgIdentifier)).isEqualTo(orgIdentifier);
    assertThat(criteria2Object.get(PipelineEntityKeys.projectIdentifier)).isEqualTo(projectIdentifier);
    assertThat(criteria2Object.get(PipelineEntityKeys.identifier)).isEqualTo(pipelineIdentifier);
    assertThat(criteria2Object.get(PipelineEntityKeys.deleted)).isEqualTo(false);
    assertThat(criteria2Object.get(PipelineEntityKeys.version)).isNull();
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testUpdatePipelineInfo() throws IOException {
    FilterCreatorMergeServiceResponse response =
        FilterCreatorMergeServiceResponse.builder()
            .stageCount(1)
            .stageNames(Collections.singletonList("stage-1"))
            .filters(Collections.singletonMap("whatKey?", "{\"some\" : \"value\"}"))
            .build();
    doReturn(response).when(filterCreatorMergeService).getPipelineInfo(any());
    PipelineEntity entity = PipelineEntity.builder().build();
    pmsPipelineServiceHelper.updatePipelineInfo(entity);
    assertThat(entity.getStageCount()).isEqualTo(1);
    assertThat(entity.getStageNames().size()).isEqualTo(1);
    assertThat(entity.getStageNames().contains("stage-1")).isTrue();
    assertThat(entity.getFilters().size()).isEqualTo(1);
    assertThat(entity.getFilters().containsKey("whatKey?")).isTrue();
    assertThat(entity.getFilters().containsValue(Document.parse("{\"some\" : \"value\"}"))).isTrue();
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testPopulateFilterUsingIdentifier() {
    String filterIdentifier = "filterIdentifier";
    FilterDTO filterDTO =
        FilterDTO.builder()
            .filterProperties(PipelineFilterPropertiesDto.builder()
                                  .name(pipelineIdentifier)
                                  .description("some description")
                                  .pipelineTags(Collections.singletonList(NGTag.builder().key("c").value("h").build()))
                                  .pipelineIdentifiers(Collections.singletonList(pipelineIdentifier))
                                  .build())
            .build();
    doReturn(filterDTO)
        .when(filterService)
        .get(accountIdentifier, orgIdentifier, projectIdentifier, filterIdentifier, FilterType.PIPELINESETUP);
    Criteria criteria = new Criteria();
    pmsPipelineServiceHelper.populateFilterUsingIdentifier(
        criteria, accountIdentifier, orgIdentifier, projectIdentifier, filterIdentifier);
    Document criteriaObject = criteria.getCriteriaObject();
    assertThat(criteriaObject.get(PipelineEntityKeys.name)).isEqualTo(pipelineIdentifier);
    assertThat(((List<?>) ((Map<?, ?>) criteriaObject.get(PipelineEntityKeys.identifier)).get("$in")).size())
        .isEqualTo(1);
    assertThat(((List<?>) ((Map<?, ?>) criteriaObject.get(PipelineEntityKeys.identifier)).get("$in"))
                   .contains(pipelineIdentifier))
        .isTrue();
    assertThat(((List<?>) ((Map<?, ?>) criteriaObject.get(PipelineEntityKeys.tags)).get("$in")).size()).isEqualTo(1);
    assertThat(((List<?>) ((Map<?, ?>) criteriaObject.get(PipelineEntityKeys.tags)).get("$in"))
                   .contains(NGTag.builder().key("c").value("h").build()))
        .isTrue();
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testPopulateFilter() {
    Criteria criteria = new Criteria();
    PipelineFilterPropertiesDto pipelineFilter =
        PipelineFilterPropertiesDto.builder()
            .name(pipelineIdentifier)
            .description("some description")
            .pipelineTags(Collections.singletonList(NGTag.builder().key("c").value("h").build()))
            .pipelineIdentifiers(Collections.singletonList(pipelineIdentifier))
            .build();
    PMSPipelineServiceHelper.populateFilter(criteria, pipelineFilter);
    Document criteriaObject = criteria.getCriteriaObject();
    assertThat(criteriaObject.get(PipelineEntityKeys.name)).isEqualTo(pipelineIdentifier);
    assertThat(((List<?>) ((Map<?, ?>) criteriaObject.get(PipelineEntityKeys.identifier)).get("$in")).size())
        .isEqualTo(1);
    assertThat(((List<?>) ((Map<?, ?>) criteriaObject.get(PipelineEntityKeys.identifier)).get("$in"))
                   .contains(pipelineIdentifier))
        .isTrue();
    assertThat(((List<?>) ((Map<?, ?>) criteriaObject.get(PipelineEntityKeys.tags)).get("$in")).size()).isEqualTo(1);
    assertThat(((List<?>) ((Map<?, ?>) criteriaObject.get(PipelineEntityKeys.tags)).get("$in"))
                   .contains(NGTag.builder().key("c").value("h").build()))
        .isTrue();
  }
}