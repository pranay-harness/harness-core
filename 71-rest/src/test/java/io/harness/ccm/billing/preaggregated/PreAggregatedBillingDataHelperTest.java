package io.harness.ccm.billing.preaggregated;

import static com.google.cloud.bigquery.FieldValue.Attribute.PRIMITIVE;
import static io.harness.ccm.billing.preaggregated.PreAggregateConstants.entityConstantAwsBlendedCost;
import static io.harness.ccm.billing.preaggregated.PreAggregateConstants.entityConstantAwsInstanceType;
import static io.harness.ccm.billing.preaggregated.PreAggregateConstants.entityConstantAwsLinkedAccount;
import static io.harness.ccm.billing.preaggregated.PreAggregateConstants.entityConstantAwsRegion;
import static io.harness.ccm.billing.preaggregated.PreAggregateConstants.entityConstantAwsService;
import static io.harness.ccm.billing.preaggregated.PreAggregateConstants.entityConstantAwsUnBlendedCost;
import static io.harness.ccm.billing.preaggregated.PreAggregateConstants.entityConstantAwsUsageType;
import static io.harness.ccm.billing.preaggregated.PreAggregateConstants.maxPreAggStartTimeConstant;
import static io.harness.ccm.billing.preaggregated.PreAggregateConstants.minPreAggStartTimeConstant;
import static io.harness.ccm.billing.preaggregated.PreAggregateConstants.nullStringValueConstant;
import static io.harness.rule.OwnerRule.ROHIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.google.cloud.Timestamp;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldList;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.StandardSQLTypeName;
import com.google.cloud.bigquery.TableResult;

import com.healthmarketscience.sqlbuilder.BinaryCondition;
import com.healthmarketscience.sqlbuilder.Condition;
import com.healthmarketscience.sqlbuilder.FunctionCall;
import com.healthmarketscience.sqlbuilder.SqlObject;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ccm.billing.graphql.BillingIdFilter;
import io.harness.ccm.billing.graphql.BillingTimeFilter;
import io.harness.ccm.billing.graphql.CloudBillingFilter;
import io.harness.ccm.billing.preaggregated.PreAggregatedCostData.PreAggregatedCostDataBuilder;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.wings.graphql.datafetcher.billing.BillingDataHelper;
import software.wings.graphql.schema.type.aggregation.QLIdOperator;
import software.wings.graphql.schema.type.aggregation.QLTimeOperator;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingStatsInfo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;

public class PreAggregatedBillingDataHelperTest extends CategoryTest {
  @Mock FieldValueList row;
  @Mock TableResult tableResult;
  @Mock BillingDataHelper billingDataHelper;
  @InjectMocks PreAggregatedBillingDataHelper dataHelper;
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  private static Calendar calendar;
  List<Condition> conditions = new ArrayList<>();
  List<SqlObject> aggregates = new ArrayList<>();
  private String ENTRY_POINT = "entryPoint";
  private static final String CLOUD_PROVIDER = "AWS";
  private static final String BLENDED_COST_LABEL = "blended Cost";
  private static final long currentMillis = Instant.now().toEpochMilli();
  private static final long MIN_START_TIME = 0L;
  private static final long MAX_START_TIME = currentMillis;
  private static final Double COST = 1.4433;

  @Before
  public void setup() {
    calendar = new GregorianCalendar(2020, Calendar.JANUARY, 1);
    Condition condition1 =
        BinaryCondition.greaterThanOrEq(PreAggregatedTableSchema.startTime, Timestamp.of(calendar.getTime()));
    Condition condition2 = BinaryCondition.equalTo(PreAggregatedTableSchema.serviceCode, "serviceCode");
    conditions.add(condition1);
    conditions.add(condition2);

    FunctionCall aggregateFunction = FunctionCall.sum().addColumnParams(PreAggregatedTableSchema.blendedCost);
    aggregates.add(aggregateFunction);

    when(row.get(entityConstantAwsRegion)).thenReturn(FieldValue.of(PRIMITIVE, entityConstantAwsRegion));
    when(row.get(entityConstantAwsLinkedAccount)).thenReturn(FieldValue.of(PRIMITIVE, entityConstantAwsLinkedAccount));
    when(row.get(entityConstantAwsService)).thenReturn(FieldValue.of(PRIMITIVE, entityConstantAwsService));
    when(row.get(entityConstantAwsUsageType)).thenReturn(FieldValue.of(PRIMITIVE, entityConstantAwsUsageType));
    when(row.get(entityConstantAwsInstanceType)).thenReturn(FieldValue.of(PRIMITIVE, null));
    when(row.get(minPreAggStartTimeConstant)).thenReturn(FieldValue.of(PRIMITIVE, "0"));
    when(row.get(maxPreAggStartTimeConstant)).thenReturn(FieldValue.of(PRIMITIVE, "1586895998"));
    when(row.get(entityConstantAwsBlendedCost)).thenReturn(FieldValue.of(PRIMITIVE, "1.0"));
    when(row.get(entityConstantAwsUnBlendedCost)).thenReturn(FieldValue.of(PRIMITIVE, "2.0"));
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void getQuery() {
    List<Object> groupBy = Arrays.asList(PreAggregatedTableSchema.usageAccountId, PreAggregatedTableSchema.serviceCode,
        PreAggregatedTableSchema.usageType, PreAggregatedTableSchema.instanceType, PreAggregatedTableSchema.region);
    String query = dataHelper.getQuery(aggregates, groupBy, conditions, Collections.emptyList());
    assertThat(query.contains(entityConstantAwsRegion)).isTrue();
    assertThat(query.contains(entityConstantAwsLinkedAccount)).isTrue();
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void ProcessDataPointAndAppendToListTest() {
    FieldList fieldList = FieldList.of(Field.newBuilder(entityConstantAwsRegion, StandardSQLTypeName.STRING).build(),
        Field.newBuilder(entityConstantAwsLinkedAccount, StandardSQLTypeName.STRING).build(),
        Field.newBuilder(entityConstantAwsService, StandardSQLTypeName.STRING).build(),
        Field.newBuilder(entityConstantAwsUsageType, StandardSQLTypeName.STRING).build(),
        Field.newBuilder(entityConstantAwsInstanceType, StandardSQLTypeName.STRING).build(),
        Field.newBuilder(entityConstantAwsBlendedCost, StandardSQLTypeName.FLOAT64).build(),
        Field.newBuilder(entityConstantAwsUnBlendedCost, StandardSQLTypeName.FLOAT64).build());

    List<PreAggregateBillingEntityDataPoint> dataPointList = new ArrayList<>();
    dataHelper.ProcessDataPointAndAppendToList(fieldList, row, dataPointList);
    assertThat(dataPointList.size()).isEqualTo(1);
    assertThat(dataPointList.get(0).getAwsRegion()).isEqualTo(entityConstantAwsRegion);
    assertThat(dataPointList.get(0).getAwsInstanceType()).isEqualTo(nullStringValueConstant);
    assertThat(dataPointList.get(0).getAwsLinkedAccount()).isEqualTo(entityConstantAwsLinkedAccount);
    assertThat(dataPointList.get(0).getAwsUsageType()).isEqualTo(entityConstantAwsUsageType);
    assertThat(dataPointList.get(0).getAwsService()).isEqualTo(entityConstantAwsService);
    assertThat(dataPointList.get(0).getAwsUnblendedCost()).isEqualTo(2.0);
    assertThat(dataPointList.get(0).getAwsBlendedCost()).isEqualTo(1.0);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void processTrendDataAndAppendToListTest() {
    FieldList trendFieldList =
        FieldList.of(Field.newBuilder(maxPreAggStartTimeConstant, StandardSQLTypeName.STRING).build(),
            Field.newBuilder(minPreAggStartTimeConstant, StandardSQLTypeName.STRING).build(),
            Field.newBuilder(entityConstantAwsBlendedCost, StandardSQLTypeName.FLOAT64).build(),
            Field.newBuilder(entityConstantAwsUnBlendedCost, StandardSQLTypeName.FLOAT64).build());

    PreAggregatedCostDataBuilder unBlendedCostDataBuilder = PreAggregatedCostData.builder();
    PreAggregatedCostDataBuilder blendedCostDataBuilder = PreAggregatedCostData.builder();

    dataHelper.processTrendDataAndAppendToList(trendFieldList, row, blendedCostDataBuilder, unBlendedCostDataBuilder);
    PreAggregatedCostData blendedCost = blendedCostDataBuilder.build();
    PreAggregatedCostData unBlendedCost = unBlendedCostDataBuilder.build();

    assertThat(blendedCost.getCost()).isEqualTo(1.0);
    assertThat(unBlendedCost.getCost()).isEqualTo(2.0);
    assertThat(blendedCost.getMinStartTime()).isEqualTo(0L);
    assertThat(unBlendedCost.getMaxStartTime()).isEqualTo(1586895998000000L);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void getCostBillingStatsTest() {
    PreAggregatedCostData blendedCostData =
        PreAggregatedCostData.builder().cost(COST).maxStartTime(MAX_START_TIME).minStartTime(MIN_START_TIME).build();
    List<CloudBillingFilter> filters = new ArrayList<>();
    filters.addAll(Arrays.asList(getCloudProviderFilter(new String[] {CLOUD_PROVIDER}), getPreAggStartTimeFilter(0L),
        getPreAggEndTimeFilter(currentMillis)));

    when(billingDataHelper.getRoundedDoubleValue(COST)).thenReturn(1.44);
    when(billingDataHelper.getTotalCostFormattedDate(Instant.ofEpochMilli(0L))).thenReturn("01 January, 1970");
    when(billingDataHelper.getTotalCostFormattedDate(Instant.ofEpochMilli(MAX_START_TIME / 1000)))
        .thenReturn("01 January, 2020");
    QLBillingStatsInfo stats = dataHelper.getCostBillingStats(blendedCostData, filters, BLENDED_COST_LABEL);
    assertThat(stats.getStatsDescription()).isEqualTo("of 01 January, 1970 - 01 January, 2020");
    assertThat(stats.getStatsLabel()).isEqualTo("blended Cost");
    assertThat(stats.getStatsValue()).isEqualTo("$1.44");
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void PreconditionsValidationTest() {
    when(tableResult.getTotalRows()).thenReturn(10L);
    boolean preconditionsValidation = dataHelper.preconditionsValidation(tableResult, ENTRY_POINT);
    assertThat(preconditionsValidation).isFalse();
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void PreconditionsValidationNegetiveCaseTest() {
    when(tableResult.getTotalRows()).thenReturn(0L);
    boolean preconditionsValidation = dataHelper.preconditionsValidation(tableResult, ENTRY_POINT);
    assertThat(preconditionsValidation).isTrue();
  }

  private CloudBillingFilter getCloudProviderFilter(String[] cloudProvider) {
    CloudBillingFilter cloudBillingFilter = new CloudBillingFilter();
    cloudBillingFilter.setCloudProvider(
        BillingIdFilter.builder().operator(QLIdOperator.IN).values(cloudProvider).build());
    return cloudBillingFilter;
  }

  private CloudBillingFilter getPreAggStartTimeFilter(Long filterTime) {
    CloudBillingFilter cloudBillingFilter = new CloudBillingFilter();
    cloudBillingFilter.setPreAggregatedTableStartTime(
        BillingTimeFilter.builder().operator(QLTimeOperator.AFTER).value(filterTime).build());
    return cloudBillingFilter;
  }

  private CloudBillingFilter getPreAggEndTimeFilter(Long filterTime) {
    CloudBillingFilter cloudBillingFilter = new CloudBillingFilter();
    cloudBillingFilter.setPreAggregatedTableEndTime(
        BillingTimeFilter.builder().operator(QLTimeOperator.BEFORE).value(filterTime).build());
    return cloudBillingFilter;
  }
}