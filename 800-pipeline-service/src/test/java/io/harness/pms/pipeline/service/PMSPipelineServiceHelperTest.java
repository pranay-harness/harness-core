/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

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
    Criteria criteria = PMSPipelineServiceHelper.getPipelineEqualityCriteria(
        accountIdentifier, orgIdentifier, projectIdentifier, pipelineIdentifier, false, null);
    assertThat(criteria).isNotNull();
    Document criteriaObject = criteria.getCriteriaObject();
    assertThat(criteriaObject.get(PipelineEntityKeys.accountId)).isEqualTo(accountIdentifier);
    assertThat(criteriaObject.get(PipelineEntityKeys.orgIdentifier)).isEqualTo(orgIdentifier);
    assertThat(criteriaObject.get(PipelineEntityKeys.projectIdentifier)).isEqualTo(projectIdentifier);
    assertThat(criteriaObject.get(PipelineEntityKeys.identifier)).isEqualTo(pipelineIdentifier);
    assertThat(criteriaObject.get(PipelineEntityKeys.deleted)).isEqualTo(false);
    assertThat(criteriaObject.get(PipelineEntityKeys.version)).isNull();
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
    PipelineEntity updatedEntity = pmsPipelineServiceHelper.updatePipelineInfo(entity);
    assertThat(updatedEntity.getStageCount()).isEqualTo(1);
    assertThat(updatedEntity.getStageNames().size()).isEqualTo(1);
    assertThat(updatedEntity.getStageNames().contains("stage-1")).isTrue();
    assertThat(updatedEntity.getFilters().size()).isEqualTo(1);
    assertThat(updatedEntity.getFilters().containsKey("whatKey?")).isTrue();
    assertThat(updatedEntity.getFilters().containsValue(Document.parse("{\"some\" : \"value\"}"))).isTrue();
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
