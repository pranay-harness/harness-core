package io.harness.ccm.views.service.impl;

import static io.harness.rule.OwnerRule.ROHIT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ccm.views.entities.ViewFieldIdentifier;
import io.harness.ccm.views.graphql.QLCEViewFieldInput;
import io.harness.ccm.views.graphql.QLCEViewFilter;
import io.harness.ccm.views.graphql.QLCEViewFilterWrapper;
import io.harness.ccm.views.graphql.ViewsMetaDataFields;
import io.harness.ccm.views.graphql.ViewsQueryBuilder;
import io.harness.rule.Owner;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.TableResult;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

public class ViewsBillingServiceImplTest extends CategoryTest {
  public static final String CLUSTER_ID = "clusterId";
  public static final String CLUSTER = "cluster";
  public static final String LABEL_KEY = "labelKey";
  public static final String LABEL_KEY_NAME = "labelKeyName";
  public static final String LABEL_VALUE = "labelValue";
  @InjectMocks @Spy private ViewsBillingServiceImpl viewsBillingService;
  @Mock private ViewsQueryBuilder viewsQueryBuilder;
  @Mock BigQuery bigQuery;
  @Mock TableResult resultSet;

  private static QLCEViewFieldInput clusterId;
  private static QLCEViewFieldInput labelKey;
  private static QLCEViewFieldInput labelValue;
  private static String cloudProviderTable = "cloudProviderTable";

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    doCallRealMethod().when(viewsQueryBuilder).getAliasFromField(any());
    doCallRealMethod()
        .when(viewsQueryBuilder)
        .getFilterValuesQuery(any(), any(), any(), anyString(), anyInt(), anyInt());
    doReturn(resultSet).when(bigQuery).query(any());

    clusterId = QLCEViewFieldInput.builder()
                    .fieldId(CLUSTER_ID)
                    .fieldName(CLUSTER)
                    .identifier(ViewFieldIdentifier.CLUSTER)
                    .identifierName(ViewFieldIdentifier.CLUSTER.getDisplayName())
                    .build();

    labelKey = QLCEViewFieldInput.builder()
                   .fieldId(ViewsMetaDataFields.LABEL_KEY.getFieldName())
                   .identifier(ViewFieldIdentifier.LABEL)
                   .identifierName(ViewFieldIdentifier.LABEL.getDisplayName())
                   .build();
    doReturn(Collections.singletonList(LABEL_KEY))
        .when(viewsBillingService)
        .convertToFilterValuesData(resultSet, Collections.singletonList(labelKey));

    labelValue = QLCEViewFieldInput.builder()
                     .fieldId(ViewsMetaDataFields.LABEL_VALUE.getFieldName())
                     .fieldName(LABEL_KEY_NAME)
                     .identifier(ViewFieldIdentifier.LABEL)
                     .identifierName(ViewFieldIdentifier.LABEL.getDisplayName())
                     .build();
    doReturn(Collections.singletonList(LABEL_VALUE))
        .when(viewsBillingService)
        .convertToFilterValuesData(resultSet, Collections.singletonList(labelValue));
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void getFilterValueStats() {
    doReturn(Collections.singletonList(CLUSTER))
        .when(viewsBillingService)
        .convertToFilterValuesData(resultSet, Collections.singletonList(clusterId));
    List<QLCEViewFilterWrapper> filters = new ArrayList<>();
    filters.add(QLCEViewFilterWrapper.builder()
                    .idFilter(QLCEViewFilter.builder().field(clusterId).values(new String[] {""}).build())
                    .build());
    List<String> filterValueStats =
        viewsBillingService.getFilterValueStats(bigQuery, filters, cloudProviderTable, 10, 0);
    assertThat(filterValueStats.get(0)).isEqualTo(CLUSTER);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void getFilterValueStatsQuery() {
    doReturn(Arrays.asList(CLUSTER)).when(viewsBillingService).convertToFilterValuesData(any(), any());
    List<QLCEViewFilterWrapper> filters = new ArrayList<>();
    filters.add(QLCEViewFilterWrapper.builder()
                    .idFilter(QLCEViewFilter.builder().field(clusterId).values(new String[] {CLUSTER}).build())
                    .build());
    List<String> filterValueStats =
        viewsBillingService.getFilterValueStats(bigQuery, filters, cloudProviderTable, 10, 0);
    assertThat(filterValueStats.get(0)).isEqualTo(CLUSTER);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void getFilterValueStatsLabelKey() {
    List<QLCEViewFilterWrapper> filters = new ArrayList<>();
    filters.add(QLCEViewFilterWrapper.builder()
                    .idFilter(QLCEViewFilter.builder().field(labelKey).values(new String[] {""}).build())
                    .build());
    List<String> filterValueStats =
        viewsBillingService.getFilterValueStats(bigQuery, filters, cloudProviderTable, 10, 0);
    assertThat(filterValueStats.get(0)).isEqualTo(LABEL_KEY);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void getFilterValueStatsLabelValue() {
    List<QLCEViewFilterWrapper> filters = new ArrayList<>();
    filters.add(QLCEViewFilterWrapper.builder()
                    .idFilter(QLCEViewFilter.builder().field(labelValue).values(new String[] {""}).build())
                    .build());
    List<String> filterValueStats =
        viewsBillingService.getFilterValueStats(bigQuery, filters, cloudProviderTable, 10, 0);
    assertThat(filterValueStats.get(0)).isEqualTo(LABEL_VALUE);
  }
}
