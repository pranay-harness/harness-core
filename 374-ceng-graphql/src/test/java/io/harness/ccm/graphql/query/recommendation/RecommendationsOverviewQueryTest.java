/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.graphql.query.recommendation;

import static io.harness.rule.OwnerRule.UTSAV;
import static io.harness.timescaledb.Tables.CE_RECOMMENDATIONS;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ccm.commons.beans.recommendation.RecommendationOverviewStats;
import io.harness.ccm.commons.beans.recommendation.ResourceType;
import io.harness.ccm.graphql.core.recommendation.RecommendationService;
import io.harness.ccm.graphql.dto.recommendation.FilterStatsDTO;
import io.harness.ccm.graphql.dto.recommendation.RecommendationItemDTO;
import io.harness.ccm.graphql.dto.recommendation.RecommendationsDTO;
import io.harness.ccm.graphql.utils.GraphQLUtils;
import io.harness.rule.Owner;

import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.List;
import org.jooq.Condition;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RecommendationsOverviewQueryTest extends CategoryTest {
  private static final String ACCOUNT_ID = "accountId";
  private static final String NAME = "name";
  private static final String CLUSTER_NAME = "clusterName";
  private static final Double MONTHLY_COST = 100D;
  private static final Double MONTHLY_SAVING = 40D;
  private static final String NAMESPACE = "namespace";
  private static final String ID = "id0";

  private ArgumentCaptor<Condition> conditionCaptor;

  @Mock private GraphQLUtils graphQLUtils;
  @Mock private RecommendationService recommendationService;
  @InjectMocks private RecommendationsOverviewQuery overviewQuery;

  @Before
  public void setUp() throws Exception {
    when(graphQLUtils.getAccountIdentifier(any())).thenReturn(ACCOUNT_ID);

    conditionCaptor = ArgumentCaptor.forClass(Condition.class);
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testGetRecommendationsOverviewQueryWithZeroItems() {
    when(recommendationService.listAll(
             eq(ACCOUNT_ID), any(Condition.class), eq(GraphQLUtils.DEFAULT_OFFSET), eq(GraphQLUtils.DEFAULT_LIMIT)))
        .thenReturn(Collections.emptyList());

    final RecommendationsDTO recommendationsDTO =
        overviewQuery.recommendations(null, null, null, null, null, null, null, 0L, 10L, null);

    assertRecommendationOverviewListResponse(recommendationsDTO);
    assertThat(recommendationsDTO.getItems()).isEmpty();
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testGetRecommendationsOverviewQueryWithIdFilter() {
    when(recommendationService.listAll(
             eq(ACCOUNT_ID), any(Condition.class), eq(GraphQLUtils.DEFAULT_OFFSET), eq(GraphQLUtils.DEFAULT_LIMIT)))
        .thenReturn(ImmutableList.of(createRecommendationItem("id0", ResourceType.WORKLOAD)));

    final RecommendationsDTO recommendationsDTO =
        overviewQuery.recommendations(ID, null, null, null, null, null, null, 0L, 10L, null);

    assertRecommendationOverviewListResponse(recommendationsDTO);
    assertThat(recommendationsDTO.getItems()).containsExactly(createRecommendationItem("id0"));
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testGetRecommendationsOverviewQueryWithNoFilter() {
    when(recommendationService.listAll(eq(ACCOUNT_ID), any(Condition.class), any(), any()))
        .thenReturn(
            ImmutableList.of(createRecommendationItem("id0"), createRecommendationItem("id1", ResourceType.NODE_POOL)));

    final RecommendationsDTO recommendationsDTO =
        overviewQuery.recommendations(null, null, null, null, null, null, null, 0L, 10L, null);

    assertRecommendationOverviewListResponse(recommendationsDTO);
    assertThat(recommendationsDTO.getItems())
        .containsExactlyInAnyOrder(
            createRecommendationItem("id0"), createRecommendationItem("id1", ResourceType.NODE_POOL));
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testGetRecommendationsOverviewQueryWithAllFiltersExceptId() {
    when(recommendationService.listAll(eq(ACCOUNT_ID), any(Condition.class), any(), any()))
        .thenReturn(
            ImmutableList.of(createRecommendationItem("id0"), createRecommendationItem("id1", ResourceType.NODE_POOL)));

    final RecommendationsDTO recommendationsDTO = overviewQuery.recommendations(
        null, NAME, NAMESPACE, CLUSTER_NAME, ResourceType.WORKLOAD, 0D, 0D, 0L, 10L, null);

    assertRecommendationOverviewListResponse(recommendationsDTO);
    assertThat(recommendationsDTO.getItems())
        .containsExactlyInAnyOrder(
            createRecommendationItem("id0"), createRecommendationItem("id1", ResourceType.NODE_POOL));
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testRecommendationFilterStats() {
    String columnName = "resourceType";
    List<String> columns = Collections.singletonList(columnName);
    List<FilterStatsDTO> actualResponse =
        ImmutableList.of(FilterStatsDTO.builder()
                             .key(columnName)
                             .values(ImmutableList.of(ResourceType.WORKLOAD.name(), ResourceType.NODE_POOL.name()))
                             .build());

    when(recommendationService.getFilterStats(eq(ACCOUNT_ID), any(), eq(columns), eq(CE_RECOMMENDATIONS)))
        .thenReturn(actualResponse);

    List<FilterStatsDTO> result =
        overviewQuery.recommendationFilterStats(columns, null, null, null, null, null, null, null);

    verify(recommendationService, times(1)).getFilterStats(any(), conditionCaptor.capture(), any(), any());

    assertThat(result).isNotNull().hasSize(1);
    assertThat(result.get(0).getKey()).isEqualTo(columnName);
    assertThat(result.get(0).getValues())
        .containsExactlyInAnyOrder(ResourceType.WORKLOAD.name(), ResourceType.NODE_POOL.name());

    assertCommonCondition(conditionCaptor.getValue());
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testRecommendationFilterStatsWithPreselectedFilters() {
    String columnName = "resourceType";
    List<String> columns = Collections.singletonList(columnName);
    List<FilterStatsDTO> actualResponse = ImmutableList.of(
        FilterStatsDTO.builder().key(columnName).values(ImmutableList.of(ResourceType.WORKLOAD.name())).build());

    when(recommendationService.getFilterStats(eq(ACCOUNT_ID), any(), eq(columns), eq(CE_RECOMMENDATIONS)))
        .thenReturn(actualResponse);

    List<FilterStatsDTO> result = overviewQuery.recommendationFilterStats(
        columns, "name0", "namespace0", "clusterName0", ResourceType.WORKLOAD, 200D, 100D, null);

    verify(recommendationService, times(1)).getFilterStats(any(), conditionCaptor.capture(), any(), any());

    assertThat(result).isNotNull().hasSize(1);
    assertThat(result.get(0).getKey()).isEqualTo(columnName);
    assertThat(result.get(0).getValues()).containsExactly(ResourceType.WORKLOAD.name());

    Condition condition = conditionCaptor.getValue();
    assertCommonCondition(condition);

    assertThat(condition.toString())
        .contains(CE_RECOMMENDATIONS.NAME.getQualifiedName().toString())
        .contains("name0")
        .contains(CE_RECOMMENDATIONS.NAMESPACE.getQualifiedName().toString())
        .contains("namespace0")
        .contains(CE_RECOMMENDATIONS.CLUSTERNAME.getQualifiedName().toString())
        .contains("clusterName0")
        .contains(CE_RECOMMENDATIONS.RESOURCETYPE.getQualifiedName().toString())
        .contains(ResourceType.WORKLOAD.name())
        .contains(CE_RECOMMENDATIONS.MONTHLYCOST.getQualifiedName().toString())
        .contains("100")
        .contains(CE_RECOMMENDATIONS.MONTHLYSAVING.getQualifiedName().toString())
        .contains("200");
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testRecommendationStats() {
    when(recommendationService.getStats(eq(ACCOUNT_ID), any()))
        .thenReturn(RecommendationOverviewStats.builder().totalMonthlyCost(100D).totalMonthlySaving(100D).build());

    RecommendationOverviewStats stats =
        overviewQuery.recommendationStats(null, null, null, null, null, null, null, null);

    verify(recommendationService, times(1)).getStats(any(), conditionCaptor.capture());

    assertThat(stats).isNotNull();
    assertThat(stats.getTotalMonthlyCost()).isCloseTo(100D, offset(0.5D));
    assertThat(stats.getTotalMonthlySaving()).isCloseTo(100D, offset(0.5D));

    assertCommonCondition(conditionCaptor.getValue());
  }

  private void assertCommonCondition(Condition condition) {
    assertThat(condition).isNotNull();
    assertThat(condition.toString())
        .contains(CE_RECOMMENDATIONS.ISVALID.getQualifiedName().toString())
        .contains("true")
        .contains(CE_RECOMMENDATIONS.LASTPROCESSEDAT.getQualifiedName().toString());
  }

  private void assertRecommendationOverviewListResponse(final RecommendationsDTO recommendationsDTO) {
    assertThat(recommendationsDTO).isNotNull();
    assertThat(recommendationsDTO.getLimit()).isEqualTo(GraphQLUtils.DEFAULT_LIMIT);
    assertThat(recommendationsDTO.getOffset()).isEqualTo(GraphQLUtils.DEFAULT_OFFSET);
    assertThat(recommendationsDTO.getItems()).isNotNull();
  }

  private static RecommendationItemDTO createRecommendationItem(String id) {
    return createRecommendationItem(id, ResourceType.WORKLOAD);
  }

  private static RecommendationItemDTO createRecommendationItem(String id, ResourceType resourceType) {
    return RecommendationItemDTO.builder()
        .id(id)
        .clusterName(CLUSTER_NAME)
        .monthlyCost(MONTHLY_COST)
        .monthlySaving(MONTHLY_SAVING)
        .resourceName(NAME)
        .recommendationDetails(null)
        .resourceType(resourceType)
        .build();
  }
}
