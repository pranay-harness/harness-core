package io.harness.ccm.views.graphql;

import static io.harness.rule.OwnerRule.SANDESH;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ccm.views.entities.ViewField;
import io.harness.ccm.views.entities.ViewFieldIdentifier;
import io.harness.ccm.views.entities.ViewIdCondition;
import io.harness.ccm.views.entities.ViewIdOperator;
import io.harness.ccm.views.entities.ViewRule;
import io.harness.ccm.views.graphql.anomalydetection.ADViewsQueryBuilder;
import io.harness.ccm.views.utils.ViewFieldUtils;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import com.healthmarketscience.sqlbuilder.SelectQuery;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class ADViewsQueryBuilderTest extends CategoryTest {
  @Inject @InjectMocks ADViewsQueryBuilder adViewsQueryBuilder;
  QLCEViewTimeFilter endTimeFilter;
  QLCEViewTimeFilter startTimeFilter;

  QLCEViewFilter awsAccountFilter;
  QLCEViewFilter awsServiceFilter;

  QLCEViewAggregation costAgg;
  QLCEViewAggregation maxStartTimeAgg;
  QLCEViewAggregation minStartTimeAgg;

  QLCEViewGroupBy timeDayGroupBy;
  QLCEViewGroupBy awsAccountEntityGroupBy;

  QLCEViewField awsService;
  QLCEViewField awsAccount;

  private static final String awsFilter = "awsService IS NOT NULL";

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    // time filters
    startTimeFilter = QLCEViewTimeFilter.builder()
                          .field(QLCEViewFieldInput.builder()
                                     .fieldId(ViewsMetaDataFields.START_TIME.getFieldName())
                                     .fieldName(ViewsMetaDataFields.START_TIME.getFieldName())
                                     .identifier(ViewFieldIdentifier.COMMON)
                                     .build())
                          .operator(QLCEViewTimeFilterOperator.AFTER)
                          .value(Long.valueOf(0))
                          .build();

    endTimeFilter = QLCEViewTimeFilter.builder()
                        .field(QLCEViewFieldInput.builder()
                                   .fieldId(ViewsMetaDataFields.START_TIME.getFieldName())
                                   .fieldName(ViewsMetaDataFields.START_TIME.getFieldName())
                                   .identifier(ViewFieldIdentifier.COMMON)
                                   .build())
                        .operator(QLCEViewTimeFilterOperator.BEFORE)
                        .value(Instant.now().toEpochMilli())
                        .build();

    // Aggregations
    costAgg = QLCEViewAggregation.builder()
                  .operationType(QLCEViewAggregateOperation.SUM)
                  .columnName(ViewsMetaDataFields.COST.getFieldName())
                  .build();
    maxStartTimeAgg = QLCEViewAggregation.builder()
                          .operationType(QLCEViewAggregateOperation.MAX)
                          .columnName(ViewsMetaDataFields.START_TIME.getFieldName())
                          .build();
    minStartTimeAgg = QLCEViewAggregation.builder()
                          .operationType(QLCEViewAggregateOperation.MIN)
                          .columnName(ViewsMetaDataFields.START_TIME.getFieldName())
                          .build();

    //
    List<QLCEViewField> awsFields = ViewFieldUtils.getAwsFields();
    awsService = awsFields.get(0);
    awsAccount = awsFields.get(1);

    // groupbys
    awsAccountEntityGroupBy = QLCEViewGroupBy.builder()
                                  .entityGroupBy(QLCEViewFieldInput.builder()
                                                     .fieldId(awsAccount.getFieldId())
                                                     .fieldName(awsAccount.getFieldName())
                                                     .identifier(ViewFieldIdentifier.AWS)
                                                     .identifierName(ViewFieldIdentifier.AWS.getDisplayName())
                                                     .build())
                                  .build();

    timeDayGroupBy =
        QLCEViewGroupBy.builder()
            .timeTruncGroupBy(QLCEViewTimeTruncGroupBy.builder().resolution(QLCEViewTimeGroupType.DAY).build())
            .build();

    // Filters
    awsAccountFilter = QLCEViewFilter.builder()
                           .field(QLCEViewFieldInput.builder()
                                      .fieldId(awsAccount.getFieldId())
                                      .fieldName(awsAccount.getFieldName())
                                      .identifier(ViewFieldIdentifier.AWS)
                                      .identifierName(ViewFieldIdentifier.AWS.getDisplayName())
                                      .build())
                           .operator(QLCEViewFilterOperator.NOT_IN)
                           .values(new String[] {"Dummy Account"})
                           .build();
    awsServiceFilter = QLCEViewFilter.builder()
                           .field(QLCEViewFieldInput.builder()
                                      .fieldId(awsAccount.getFieldId())
                                      .fieldName(awsAccount.getFieldName())
                                      .identifier(ViewFieldIdentifier.AWS)
                                      .identifierName(ViewFieldIdentifier.AWS.getDisplayName())
                                      .build())
                           .operator(QLCEViewFilterOperator.NOT_IN)
                           .values(new String[] {"Dummy Service"})
                           .build();
  }

  @Test
  @Owner(developers = SANDESH)
  @Category(UnitTests.class)
  public void testGetQueryAwsView() {
    List<ViewRule> viewRules = Arrays.asList(
        ViewRule.builder()
            .viewConditions(Arrays.asList(ViewIdCondition.builder()
                                              .viewField(ViewField.builder()
                                                             .fieldName(awsService.getFieldName())
                                                             .fieldId(awsService.getFieldId())
                                                             .identifier(ViewFieldIdentifier.AWS)
                                                             .identifierName(ViewFieldIdentifier.AWS.getDisplayName())
                                                             .build())
                                              .viewOperator(ViewIdOperator.IN)
                                              .values(Arrays.asList("Dummy Service"))
                                              .build()))
            .build());

    List<QLCEViewFilter> filters = new ArrayList<>();
    filters.add(awsAccountFilter);

    SelectQuery selectQuery = adViewsQueryBuilder.getQuery(viewRules, filters,
        Arrays.asList(startTimeFilter, endTimeFilter), Arrays.asList(awsAccountEntityGroupBy, timeDayGroupBy));

    assertThat(selectQuery.toString()).contains("GROUP BY awsUsageAccountId");
    assertThat(selectQuery.toString()).contains("((awsServicecode IN ('service1') )");
    assertThat(selectQuery.toString()).contains("SELECT awsUsageAccountId FROM TableName");

    // SELECT awsUsageAccountId FROM TableName WHERE ((awsServicecode IN ('service1') ) AND (startTime >=
    // '1970-01-01T00:00:00Z') AND (startTime <= '2021-03-08T14:09:51.751Z')) GROUP BY awsUsageAccountId
  }
}
