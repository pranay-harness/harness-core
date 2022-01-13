/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.views.service.impl;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.ccm.commons.utils.BigQueryHelper.UNIFIED_TABLE;
import static io.harness.ccm.views.entities.ViewFieldIdentifier.CLUSTER;
import static io.harness.ccm.views.graphql.QLCEViewTimeFilterOperator.AFTER;
import static io.harness.ccm.views.graphql.QLCEViewTimeFilterOperator.BEFORE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.bigQuery.BigQueryService;
import io.harness.ccm.budget.utils.BudgetUtils;
import io.harness.ccm.commons.utils.BigQueryHelper;
import io.harness.ccm.views.dao.CEReportScheduleDao;
import io.harness.ccm.views.dao.CEViewDao;
import io.harness.ccm.views.dto.DefaultViewIdDto;
import io.harness.ccm.views.dto.ViewTimeRangeDto;
import io.harness.ccm.views.entities.CEReportSchedule;
import io.harness.ccm.views.entities.CEView;
import io.harness.ccm.views.entities.ViewChartType;
import io.harness.ccm.views.entities.ViewCondition;
import io.harness.ccm.views.entities.ViewField;
import io.harness.ccm.views.entities.ViewFieldIdentifier;
import io.harness.ccm.views.entities.ViewIdCondition;
import io.harness.ccm.views.entities.ViewIdOperator;
import io.harness.ccm.views.entities.ViewRule;
import io.harness.ccm.views.entities.ViewState;
import io.harness.ccm.views.entities.ViewTimeGranularity;
import io.harness.ccm.views.entities.ViewTimeRange;
import io.harness.ccm.views.entities.ViewTimeRangeType;
import io.harness.ccm.views.entities.ViewType;
import io.harness.ccm.views.entities.ViewVisualization;
import io.harness.ccm.views.graphql.QLCEView;
import io.harness.ccm.views.graphql.QLCEViewAggregateOperation;
import io.harness.ccm.views.graphql.QLCEViewAggregation;
import io.harness.ccm.views.graphql.QLCEViewField;
import io.harness.ccm.views.graphql.QLCEViewFilterWrapper;
import io.harness.ccm.views.graphql.QLCEViewMetadataFilter;
import io.harness.ccm.views.graphql.QLCEViewTimeFilterOperator;
import io.harness.ccm.views.graphql.QLCEViewTrendInfo;
import io.harness.ccm.views.graphql.ViewCostData;
import io.harness.ccm.views.graphql.ViewsQueryHelper;
import io.harness.ccm.views.helper.ViewFilterBuilderHelper;
import io.harness.ccm.views.helper.ViewTimeRangeHelper;
import io.harness.ccm.views.service.CEViewService;
import io.harness.ccm.views.service.ViewCustomFieldService;
import io.harness.ccm.views.service.ViewsBillingService;
import io.harness.exception.InvalidRequestException;

import com.google.cloud.bigquery.BigQuery;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(CE)
public class CEViewServiceImpl implements CEViewService {
  @Inject private CEViewDao ceViewDao;
  @Inject private CEReportScheduleDao ceReportScheduleDao;
  @Inject private ViewsBillingService viewsBillingService;
  @Inject private ViewCustomFieldService viewCustomFieldService;
  @Inject private ViewTimeRangeHelper viewTimeRangeHelper;
  @Inject private ViewFilterBuilderHelper viewFilterBuilderHelper;
  @Inject private ViewsQueryHelper viewsQueryHelper;
  @Inject private BigQueryHelper bigQueryHelper;
  @Inject private BigQueryService bigQueryService;

  private static final String VIEW_NAME_DUPLICATE_EXCEPTION = "View with given name already exists";
  private static final String VIEW_LIMIT_REACHED_EXCEPTION = "Maximum allowed custom views limit(100) has been reached";
  private static final String DEFAULT_AZURE_VIEW_NAME = "Azure";
  private static final String DEFAULT_AZURE_FIELD_ID = "azureSubscriptionGuid";
  private static final String DEFAULT_AZURE_FIELD_NAME = "Subscription id";

  private static final String DEFAULT_AWS_VIEW_NAME = "Aws";
  private static final String DEFAULT_AWS_FIELD_ID = "awsUsageAccountId";
  private static final String DEFAULT_AWS_FIELD_NAME = "Account";

  private static final String DEFAULT_GCP_VIEW_NAME = "Gcp";
  private static final String DEFAULT_GCP_FIELD_ID = "gcpProjectId";
  private static final String DEFAULT_GCP_FIELD_NAME = "Project";

  private static final String DEFAULT_CLUSTER_VIEW_NAME = "Cluster";
  private static final String DEFAULT_CLUSTER_FIELD_ID = "clusterName";
  private static final String DEFAULT_CLUSTER_FIELD_NAME = "Cluster Name";

  private static final int VIEW_COUNT = 100;
  @Override
  public CEView save(CEView ceView) {
    validateView(ceView);
    ceView.setViewState(ViewState.DRAFT);
    ceView.setUuid(null);
    ceViewDao.save(ceView);
    return ceView;
  }

  @Override
  public Double getLastMonthCostForPerspective(String accountId, String perspectiveId) {
    if (this.get(perspectiveId) == null) {
      throw new InvalidRequestException(BudgetUtils.INVALID_PERSPECTIVE_ID_EXCEPTION);
    }
    List<QLCEViewFilterWrapper> filters = new ArrayList<>();
    filters.add(viewsQueryHelper.getViewMetadataFilter(perspectiveId));
    filters.add(viewsQueryHelper.getPerspectiveTimeFilter(BudgetUtils.getStartOfMonth(true), AFTER));
    filters.add(viewsQueryHelper.getPerspectiveTimeFilter(
        BudgetUtils.getStartOfMonth(false) - BudgetUtils.ONE_DAY_MILLIS, BEFORE));
    return getCostForPerspective(accountId, filters);
  }

  private double getCostForPerspective(String accountId, List<QLCEViewFilterWrapper> filters) {
    String cloudProviderTable = bigQueryHelper.getCloudProviderTableName(accountId, UNIFIED_TABLE);
    ViewCostData costData = viewsBillingService.getCostData(bigQueryService.get(), filters,
        viewsQueryHelper.getPerspectiveTotalCostAggregation(), cloudProviderTable,
        viewsQueryHelper.buildQueryParams(accountId, false));
    return costData.getCost();
  }

  @Override
  public Double getForecastCostForPerspective(String accountId, String perspectiveId) {
    if (this.get(perspectiveId) == null) {
      throw new InvalidRequestException(BudgetUtils.INVALID_PERSPECTIVE_ID_EXCEPTION);
    }
    List<QLCEViewFilterWrapper> filters = new ArrayList<>();
    long startTime = BudgetUtils.getStartTimeForForecasting();
    long endTime = BudgetUtils.getEndOfMonthForCurrentBillingCycle();
    filters.add(viewsQueryHelper.getViewMetadataFilter(perspectiveId));
    filters.add(viewsQueryHelper.getPerspectiveTimeFilter(startTime, AFTER));
    filters.add(viewsQueryHelper.getPerspectiveTimeFilter(endTime, BEFORE));
    String cloudProviderTable = bigQueryHelper.getCloudProviderTableName(accountId, UNIFIED_TABLE);
    ViewCostData costDataForForecast =
        ViewCostData.builder()
            .cost(
                viewsBillingService
                    .getCostData(bigQueryService.get(), filters, viewsQueryHelper.getPerspectiveTotalCostAggregation(),
                        cloudProviderTable, viewsQueryHelper.buildQueryParams(accountId, false))
                    .getCost())
            .minStartTime(1000 * startTime)
            .maxStartTime(1000 * BudgetUtils.getStartOfCurrentDay() - BudgetUtils.ONE_DAY_MILLIS)
            .build();
    double costTillNow = getActualCostForPerspectiveBudget(accountId, perspectiveId);
    return viewsQueryHelper.getRoundedDoubleValue(
        costTillNow + viewsQueryHelper.getForecastCost(costDataForForecast, Instant.ofEpochMilli(endTime)));
  }

  @Override
  public double getActualCostForPerspectiveBudget(String accountId, String perspectiveId) {
    List<QLCEViewFilterWrapper> filters = new ArrayList<>();
    filters.add(viewsQueryHelper.getViewMetadataFilter(perspectiveId));
    filters.add(viewsQueryHelper.getPerspectiveTimeFilter(BudgetUtils.getStartOfMonthForCurrentBillingCycle(), AFTER));
    filters.add(viewsQueryHelper.getPerspectiveTimeFilter(BudgetUtils.getEndOfMonthForCurrentBillingCycle(), BEFORE));
    return getCostForPerspective(accountId, filters);
  }

  public boolean validateView(CEView ceView) {
    CEView savedCEView = ceViewDao.findByName(ceView.getAccountId(), ceView.getName());
    if (null != savedCEView) {
      throw new InvalidRequestException(VIEW_NAME_DUPLICATE_EXCEPTION);
    }
    List<CEView> views = ceViewDao.findByAccountId(ceView.getAccountId());
    if (views.size() >= VIEW_COUNT) {
      throw new InvalidRequestException(VIEW_LIMIT_REACHED_EXCEPTION);
    }
    modifyCEViewAndSetDefaults(ceView);
    return true;
  }

  private void modifyCEViewAndSetDefaults(CEView ceView) {
    if (ceView.getViewVisualization() == null) {
      ceView.setViewVisualization(ViewVisualization.builder()
                                      .granularity(ViewTimeGranularity.DAY)
                                      .chartType(ViewChartType.STACKED_TIME_SERIES)
                                      .groupBy(ViewField.builder()
                                                   .fieldId("product")
                                                   .fieldName("Product")
                                                   .identifier(ViewFieldIdentifier.COMMON)
                                                   .identifierName(ViewFieldIdentifier.COMMON.getDisplayName())
                                                   .build())
                                      .build());
    }

    if (ceView.getViewTimeRange() == null) {
      ceView.setViewTimeRange(ViewTimeRange.builder().viewTimeRangeType(ViewTimeRangeType.LAST_7).build());
    }

    Set<ViewFieldIdentifier> viewFieldIdentifierSet = new HashSet<>();
    if (ceView.getViewRules() != null) {
      for (ViewRule rule : ceView.getViewRules()) {
        for (ViewCondition condition : rule.getViewConditions()) {
          if (((ViewIdCondition) condition).getViewField().getIdentifier() == CLUSTER) {
            viewFieldIdentifierSet.add(CLUSTER);
          }
          if (((ViewIdCondition) condition).getViewField().getIdentifier() == ViewFieldIdentifier.AWS) {
            viewFieldIdentifierSet.add(ViewFieldIdentifier.AWS);
          }
          if (((ViewIdCondition) condition).getViewField().getIdentifier() == ViewFieldIdentifier.GCP) {
            viewFieldIdentifierSet.add(ViewFieldIdentifier.GCP);
          }
          if (((ViewIdCondition) condition).getViewField().getIdentifier() == ViewFieldIdentifier.AZURE) {
            viewFieldIdentifierSet.add(ViewFieldIdentifier.AZURE);
          }
          if (((ViewIdCondition) condition).getViewField().getIdentifier() == ViewFieldIdentifier.CUSTOM) {
            String viewId = ((ViewIdCondition) condition).getViewField().getFieldId();
            List<ViewField> customFieldViewFields = viewCustomFieldService.get(viewId).getViewFields();
            for (ViewField field : customFieldViewFields) {
              viewFieldIdentifierSet.add(field.getIdentifier());
            }
            viewFieldIdentifierSet.add(ViewFieldIdentifier.CUSTOM);
          }
        }
      }
    }

    List<ViewFieldIdentifier> viewFieldIdentifierList = new ArrayList<>();
    viewFieldIdentifierList.addAll(viewFieldIdentifierSet);
    ceView.setDataSources(viewFieldIdentifierList);
  }

  @Override
  public CEView get(String uuid) {
    CEView view = ceViewDao.get(uuid);
    if (view != null && view.getViewRules() == null) {
      view.setViewRules(Collections.emptyList());
    }
    return view;
  }

  @Override
  public CEView update(CEView ceView) {
    modifyCEViewAndSetDefaults(ceView);
    return ceViewDao.update(ceView);
  }

  @Override
  public CEView updateTotalCost(CEView ceView, BigQuery bigQuery, String cloudProviderTableName) {
    if (ceView.getViewState() != null && ceView.getViewState() == ViewState.COMPLETED) {
      List<QLCEViewAggregation> totalCostAggregationFunction = Collections.singletonList(
          QLCEViewAggregation.builder().columnName("cost").operationType(QLCEViewAggregateOperation.SUM).build());
      List<QLCEViewFilterWrapper> filters = new ArrayList<>();
      filters.add(
          QLCEViewFilterWrapper.builder()
              .viewMetadataFilter(QLCEViewMetadataFilter.builder().viewId(ceView.getUuid()).isPreview(false).build())
              .build());
      ViewTimeRange viewTimeRange = ceView.getViewTimeRange();
      ViewTimeRangeDto startEndTime = viewTimeRangeHelper.getStartEndTime(viewTimeRange);
      filters.add(
          viewFilterBuilderHelper.getViewTimeFilter(startEndTime.getStartTime(), QLCEViewTimeFilterOperator.AFTER));
      filters.add(
          viewFilterBuilderHelper.getViewTimeFilter(startEndTime.getEndTime(), QLCEViewTimeFilterOperator.BEFORE));

      QLCEViewTrendInfo trendData = viewsBillingService.getTrendStatsData(
          bigQuery, filters, totalCostAggregationFunction, cloudProviderTableName);
      double totalCost = trendData.getValue().doubleValue();
      log.info("Total cost of view {}", totalCost);
      return ceViewDao.updateTotalCost(ceView.getUuid(), ceView.getAccountId(), totalCost);
    }
    return ceView;
  }

  @Override
  public boolean delete(String uuid, String accountId) {
    return ceViewDao.delete(uuid, accountId);
  }

  @Override
  public List<QLCEView> getAllViews(String accountId, boolean includeDefault) {
    List<CEView> viewList = ceViewDao.findByAccountId(accountId);
    if (!includeDefault) {
      viewList = viewList.stream()
                     .filter(view -> ImmutableSet.of(ViewType.SAMPLE, ViewType.CUSTOMER).contains(view.getViewType()))
                     .collect(Collectors.toList());
    }
    List<QLCEView> graphQLViewObjList = new ArrayList<>();
    for (CEView view : viewList) {
      List<CEReportSchedule> reportSchedules =
          ceReportScheduleDao.getReportSettingByView(view.getUuid(), view.getAccountId());
      ViewChartType vChartType = null;
      if (view.getViewVisualization() != null) {
        // For DRAFT support, no visualizations have been set at this point
        vChartType = view.getViewVisualization().getChartType();
      }
      ViewField groupBy = view.getViewVisualization().getGroupBy();
      graphQLViewObjList.add(QLCEView.builder()
                                 .id(view.getUuid())
                                 .name(view.getName())
                                 .totalCost(view.getTotalCost())
                                 .createdBy(null != view.getCreatedBy() ? view.getCreatedBy().getEmail() : "")
                                 .createdAt(view.getCreatedAt())
                                 .lastUpdatedAt(view.getLastUpdatedAt())
                                 .chartType(vChartType)
                                 .viewType(view.getViewType())
                                 .viewState(view.getViewState())
                                 .groupBy(QLCEViewField.builder()
                                              .fieldId(groupBy.getFieldId())
                                              .fieldName(groupBy.getFieldName())
                                              .identifier(groupBy.getIdentifier())
                                              .identifierName(groupBy.getIdentifier().getDisplayName())
                                              .build())
                                 .timeRange(view.getViewTimeRange().getViewTimeRangeType())
                                 .dataSources(view.getDataSources())
                                 .isReportScheduledConfigured(!reportSchedules.isEmpty())
                                 .build());
    }
    return graphQLViewObjList;
  }

  @Override
  public List<CEView> getViewByState(String accountId, ViewState viewState) {
    return ceViewDao.findByAccountIdAndState(accountId, viewState);
  }

  private ViewIdCondition getDefaultViewIdCondition(String fieldId, String fieldName, ViewFieldIdentifier identifier) {
    return ViewIdCondition.builder()
        .viewField(ViewField.builder().fieldId(fieldId).fieldName(fieldName).identifier(identifier).build())
        .viewOperator(ViewIdOperator.NOT_NULL)
        .values(Collections.singletonList(""))
        .build();
  }

  private ViewVisualization getDefaultViewVisualization(
      String fieldId, String fieldName, ViewFieldIdentifier identifier) {
    return ViewVisualization.builder()
        .granularity(ViewTimeGranularity.DAY)
        .chartType(ViewChartType.STACKED_TIME_SERIES)
        .groupBy(ViewField.builder()
                     .fieldId(fieldId)
                     .fieldName(fieldName)
                     .identifier(identifier)
                     .identifierName(identifier.getDisplayName())
                     .build())
        .build();
  }

  private CEView getDefaultView(String accountId, String viewName) {
    return CEView.builder()
        .accountId(accountId)
        .name(viewName)
        .viewVersion("v1")
        .viewType(ViewType.DEFAULT)
        .viewState(ViewState.COMPLETED)
        .build();
  }

  @Override
  public void createDefaultView(String accountId, ViewFieldIdentifier viewFieldIdentifier) {
    ViewIdCondition condition = null;
    CEView defaultView = null;
    ViewVisualization viewVisualization = null;
    switch (viewFieldIdentifier) {
      case AZURE:
        condition = getDefaultViewIdCondition(DEFAULT_AZURE_FIELD_ID, DEFAULT_AZURE_FIELD_NAME, viewFieldIdentifier);
        viewVisualization =
            getDefaultViewVisualization(DEFAULT_AZURE_FIELD_ID, DEFAULT_AZURE_FIELD_NAME, viewFieldIdentifier);
        defaultView = getDefaultView(accountId, DEFAULT_AZURE_VIEW_NAME);
        break;
      case AWS:
        condition = getDefaultViewIdCondition(DEFAULT_AWS_FIELD_ID, DEFAULT_AWS_FIELD_NAME, viewFieldIdentifier);
        viewVisualization =
            getDefaultViewVisualization(DEFAULT_AWS_FIELD_ID, DEFAULT_AWS_FIELD_NAME, viewFieldIdentifier);
        defaultView = getDefaultView(accountId, DEFAULT_AWS_VIEW_NAME);
        break;
      case GCP:
        condition = getDefaultViewIdCondition(DEFAULT_GCP_FIELD_ID, DEFAULT_GCP_FIELD_NAME, viewFieldIdentifier);
        viewVisualization =
            getDefaultViewVisualization(DEFAULT_GCP_FIELD_ID, DEFAULT_GCP_FIELD_NAME, viewFieldIdentifier);
        defaultView = getDefaultView(accountId, DEFAULT_GCP_VIEW_NAME);
        break;
      case CLUSTER:
        condition =
            getDefaultViewIdCondition(DEFAULT_CLUSTER_FIELD_ID, DEFAULT_CLUSTER_FIELD_NAME, viewFieldIdentifier);
        viewVisualization =
            getDefaultViewVisualization(DEFAULT_CLUSTER_FIELD_ID, DEFAULT_CLUSTER_FIELD_NAME, viewFieldIdentifier);
        defaultView = getDefaultView(accountId, DEFAULT_CLUSTER_VIEW_NAME);
        break;
      default:
        break;
    }

    if (null != condition && null != defaultView) {
      ViewRule rule = ViewRule.builder().viewConditions(Collections.singletonList(condition)).build();
      defaultView.setViewRules(Collections.singletonList(rule));
      defaultView.setViewVisualization(viewVisualization);
      modifyCEViewAndSetDefaults(defaultView);
      ceViewDao.save(defaultView);
    }
  }

  @Override
  public DefaultViewIdDto getDefaultViewIds(String accountId) {
    List<CEView> views = ceViewDao.findByAccountIdAndType(accountId, ViewType.DEFAULT);
    return DefaultViewIdDto.builder()
        .awsViewId(getViewId(views, ViewFieldIdentifier.AWS))
        .azureViewId(getViewId(views, ViewFieldIdentifier.AZURE))
        .gcpViewId(getViewId(views, ViewFieldIdentifier.GCP))
        .clusterViewId(getViewId(views, CLUSTER))
        .build();
  }

  @Override
  public void updateDefaultClusterViewVisualization(String viewId) {
    try {
      CEView view = ceViewDao.get(viewId);
      ViewVisualization viewVisualization =
          getDefaultViewVisualization(DEFAULT_CLUSTER_FIELD_ID, DEFAULT_CLUSTER_FIELD_NAME, CLUSTER);
      view.setViewVisualization(viewVisualization);
      ceViewDao.update(view);
    } catch (Exception e) {
      log.error("Error while updating ViewVisualization of default cluster perspective {}", e);
    }
  }

  private String getViewId(List<CEView> views, ViewFieldIdentifier viewFieldIdentifier) {
    if (views != null && views.size() > 0) {
      Optional<CEView> view =
          views.stream().filter(ceView -> ceView.getDataSources().contains(viewFieldIdentifier)).findFirst();
      if (view.isPresent()) {
        return view.get().getUuid();
      }
    }
    return null;
  }
}
