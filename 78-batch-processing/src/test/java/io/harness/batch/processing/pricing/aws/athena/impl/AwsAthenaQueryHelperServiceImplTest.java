package io.harness.batch.processing.pricing.aws.athena.impl;

import static io.harness.rule.OwnerRule.HITESH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.batch.processing.pricing.data.AccountComputePricingData;
import io.harness.batch.processing.pricing.data.AccountFargatePricingData;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.services.athena.AthenaClient;
import software.amazon.awssdk.services.athena.model.Datum;
import software.amazon.awssdk.services.athena.model.GetQueryExecutionRequest;
import software.amazon.awssdk.services.athena.model.GetQueryExecutionResponse;
import software.amazon.awssdk.services.athena.model.GetQueryResultsRequest;
import software.amazon.awssdk.services.athena.model.GetQueryResultsResponse;
import software.amazon.awssdk.services.athena.model.QueryExecution;
import software.amazon.awssdk.services.athena.model.QueryExecutionState;
import software.amazon.awssdk.services.athena.model.QueryExecutionStatus;
import software.amazon.awssdk.services.athena.model.ResultSet;
import software.amazon.awssdk.services.athena.model.Row;
import software.amazon.awssdk.services.athena.model.StartQueryExecutionRequest;
import software.amazon.awssdk.services.athena.model.StartQueryExecutionResponse;
import software.amazon.awssdk.services.athena.paginators.GetQueryResultsIterable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class AwsAthenaQueryHelperServiceImplTest extends CategoryTest {
  @Spy @InjectMocks private AwsAthenaQueryHelperServiceImpl awsAthenaQueryHelperService;
  @Mock private GetQueryResultsIterable getQueryResultsIterable;
  private AthenaClient athenaClient;
  @Before
  public void setup() {
    athenaClient = mock(AthenaClient.class);
    doReturn(athenaClient).when(awsAthenaQueryHelperService).createAthenaClient();
    when(athenaClient.startQueryExecution(any(StartQueryExecutionRequest.class)))
        .thenReturn(StartQueryExecutionResponse.builder().queryExecutionId("queryExecId").build());
    when(athenaClient.getQueryResultsPaginator(any(GetQueryResultsRequest.class))).thenReturn(getQueryResultsIterable);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testAwsFargatePriceRate() throws InterruptedException {
    List<GetQueryResultsResponse> getQueryResultsResponseList = Collections.singletonList(
        GetQueryResultsResponse.builder().resultSet(ResultSet.builder().rows(getFargateResultRows()).build()).build());
    when(athenaClient.getQueryExecution(any(GetQueryExecutionRequest.class)))
        .thenReturn(getQueryExecutionResponse(QueryExecutionState.RUNNING.toString()))
        .thenReturn(getQueryExecutionResponse(QueryExecutionState.SUCCEEDED.toString()));
    when(getQueryResultsIterable.iterator()).thenReturn(getQueryResultsResponseList.iterator());
    List<AccountFargatePricingData> accountFargatePricingData =
        awsAthenaQueryHelperService.fetchEcsFargatePriceRate("billingAccountId", Instant.now());
    assertThat(accountFargatePricingData)
        .hasSize(4)
        .containsExactlyInAnyOrderElementsOf(getAccountFargatePricingDataList());
  }

  private GetQueryExecutionResponse getQueryExecutionResponse(String state) {
    return GetQueryExecutionResponse.builder()
        .queryExecution(QueryExecution.builder().status(QueryExecutionStatus.builder().state(state).build()).build())
        .build();
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testAwsComputePriceRate() throws InterruptedException {
    List<GetQueryResultsResponse> getQueryResultsResponseList = Collections.singletonList(
        GetQueryResultsResponse.builder().resultSet(ResultSet.builder().rows(getComputeResultRows()).build()).build());
    when(athenaClient.getQueryExecution(any(GetQueryExecutionRequest.class)))
        .thenReturn(getQueryExecutionResponse(QueryExecutionState.SUCCEEDED.toString()));
    when(getQueryResultsIterable.iterator()).thenReturn(getQueryResultsResponseList.iterator());
    List<AccountComputePricingData> accountComputePricingData =
        awsAthenaQueryHelperService.fetchComputePriceRate("billingAccountId", Instant.now());
    assertThat(accountComputePricingData)
        .hasSize(2)
        .containsExactlyInAnyOrderElementsOf(getAccountComputePricingData());
  }

  private List<Row> getComputeResultRows() {
    List<List<String>> dataList = new ArrayList<>();
    dataList.add(Arrays.asList("line_item_blended_rate", "line_item_blended_cost", "line_item_unblended_rate",
        "line_item_unblended_cost", "line_item_availability_zone", "product_instance_type", "product_operating_system",
        "product_region", "product_vcpu", "product_memory"));
    dataList.add(Arrays.asList("0.0052000000", "0.1248", "0.0052000000", "0.1248", "us-east-2a", "t3.nano", "Linux",
        "us-east-2", "2", "0.5 GiB"));
    dataList.add(Arrays.asList("0.0320000000", "0.768", "0.0320000000", "0.768", "us-east-2c", "t2.small", "Windows",
        "us-east-2", "1", "2 GiB"));
    return getRowList(dataList);
  }

  private List<AccountFargatePricingData> getAccountFargatePricingDataList() {
    List<AccountFargatePricingData> accountFargatePricingDataList = new ArrayList<>();
    accountFargatePricingDataList.add(
        accountFargatePricingData(0.0404800000, 3.4941663159, 0.0404800000, 3.4941663159, "us-east-1", true, false));
    accountFargatePricingDataList.add(
        accountFargatePricingData(0.0044450000, 1.6840864498, 0.0044450000, 1.6840864498, "us-east-1", false, true));
    accountFargatePricingDataList.add(
        accountFargatePricingData(0.0404800000, 0.48576, 0.0404800000, 0.48576, "us-east-2", true, false));
    accountFargatePricingDataList.add(
        accountFargatePricingData(0.0044450000, 0.10668, 0.0044450000, 0.10668, "us-east-2", false, true));
    return accountFargatePricingDataList;
  }

  private List<AccountComputePricingData> getAccountComputePricingData() {
    List<AccountComputePricingData> accountComputePricingDataList = new ArrayList<>();
    accountComputePricingDataList.add(accountComputePricingData(
        0.0052000000, 0.1248, 0.0052000000, 0.1248, "us-east-2a", "t3.nano", "Linux", "us-east-2", 2, 0.5));
    accountComputePricingDataList.add(accountComputePricingData(
        0.0320000000, 0.768, 0.0320000000, 0.768, "us-east-2c", "t2.small", "Windows", "us-east-2", 1, 2));
    return accountComputePricingDataList;
  }

  private AccountFargatePricingData accountFargatePricingData(double blendedRate, double blendedCost,
      double unBlendedRate, double unBlendedCost, String region, boolean cpuPriceType, boolean memoryPriceType) {
    return AccountFargatePricingData.builder()
        .blendedRate(blendedRate)
        .blendedCost(blendedCost)
        .unBlendedRate(unBlendedRate)
        .unBlendedCost(unBlendedCost)
        .region(region)
        .cpuPriceType(cpuPriceType)
        .memoryPriceType(memoryPriceType)
        .build();
  }

  private AccountComputePricingData accountComputePricingData(double blendedRate, double blendedCost,
      double unBlendedRate, double unBlendedCost, String availabilityZone, String instanceType, String operatingSystem,
      String region, double cpusPerVm, double memPerVm) {
    return AccountComputePricingData.builder()
        .blendedRate(blendedRate)
        .blendedCost(blendedCost)
        .unBlendedRate(unBlendedRate)
        .unBlendedCost(unBlendedCost)
        .availabilityZone(availabilityZone)
        .instanceType(instanceType)
        .operatingSystem(operatingSystem)
        .region(region)
        .cpusPerVm(cpusPerVm)
        .memPerVm(memPerVm)
        .build();
  }

  private List<Row> getFargateResultRows() {
    List<List<String>> dataList = new ArrayList<>();
    dataList.add(Arrays.asList("line_item_blended_rate", "line_item_blended_cost", "line_item_unblended_rate",
        "line_item_unblended_cost", "product_region", "product_cputype", "product_memorytype"));
    dataList.add(
        Arrays.asList("0.0404800000", "3.4941663159", "0.0404800000", "3.4941663159", "us-east-1", "perCPU", ""));
    dataList.add(
        Arrays.asList("0.0044450000", "1.6840864498", "0.0044450000", "1.6840864498", "us-east-1", "", "perGB"));
    dataList.add(Arrays.asList("0.0404800000", "0.48576", "0.0404800000", "0.48576", "us-east-2", "perCPU", ""));
    dataList.add(Arrays.asList("0.0044450000", "0.10668", "0.0044450000", "0.10668", "us-east-2", "", "perGB"));
    return getRowList(dataList);
  }

  private List<Row> getRowList(List<List<String>> dataList) {
    return dataList.stream().map(this ::getRow).collect(Collectors.toList());
  }

  private Row getRow(List<String> dataList) {
    return Row.builder().data(getDatumList(dataList)).build();
  }

  private List<Datum> getDatumList(List<String> dataList) {
    return dataList.stream().map(this ::datum).collect(Collectors.toList());
  }

  private Datum datum(String dataValue) {
    return Datum.builder().varCharValue(dataValue).build();
  }
}
