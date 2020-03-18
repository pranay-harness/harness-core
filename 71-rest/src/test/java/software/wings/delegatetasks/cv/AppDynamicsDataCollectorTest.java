package software.wings.delegatetasks.cv;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.SOWMYA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.Spy;
import software.wings.WingsBaseTest;
import software.wings.delegatetasks.DataCollectionExecutorService;
import software.wings.helpers.ext.appdynamics.AppdynamicsRestClient;
import software.wings.service.impl.analysis.MetricElement;
import software.wings.service.impl.appdynamics.AppDynamicsDataCollectionInfoV2;
import software.wings.service.impl.appdynamics.AppdynamicsMetric;
import software.wings.service.impl.appdynamics.AppdynamicsMetric.AppdynamicsMetricType;
import software.wings.service.impl.appdynamics.AppdynamicsMetricData;
import software.wings.service.impl.appdynamics.AppdynamicsMetricDataValue;
import software.wings.service.impl.appdynamics.AppdynamicsTier;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

public class AppDynamicsDataCollectorTest extends WingsBaseTest {
  @Spy private AppDynamicsDataCollector appDynamicsDataCollector;
  @Inject private DataCollectionExecutorService dataCollectionService;

  private AppDynamicsDataCollectionInfoV2 dataCollectionInfo;
  private DataCollectionExecutionContext dataCollectionExecutionContext;
  private AppdynamicsTier appdynamicsTier;
  private List<AppdynamicsMetric> tierMetrics;
  private AppdynamicsMetricData response;
  private AppdynamicsRestClient appdynamicsRestClient;

  @Before
  public void setUp() throws IllegalAccessException {
    dataCollectionExecutionContext = Mockito.mock(DataCollectionExecutionContext.class);
    appdynamicsRestClient = Mockito.mock(AppdynamicsRestClient.class);
    dataCollectionInfo = AppDynamicsDataCollectionInfoV2.builder()
                             .accountId(generateUuid())
                             .appDynamicsTierId(10L)
                             .appDynamicsApplicationId(9L)
                             .hosts(new HashSet<>(Collections.singletonList("host")))
                             .startTime(Instant.now())
                             .endTime(Instant.now())
                             .build();
    response =
        AppdynamicsMetricData.builder()
            .metricId(4L)
            .metricName("/todolist")
            .metricPath(
                "Business Transaction Performance|Business Transactions|tier|/todolist|Individual Nodes|host|Calls per Minute")
            .metricValues(Collections.singletonList(AppdynamicsMetricDataValue.builder().value(0.2).build()))
            .build();
    appdynamicsTier = AppdynamicsTier.builder().id(10L).name("tier").build();
    tierMetrics = Collections.singletonList(
        AppdynamicsMetric.builder().name("Calls per Minute").type(AppdynamicsMetricType.leaf).build());
    doReturn(appdynamicsTier).when(appDynamicsDataCollector).getAppDynamicsTier();
    doReturn(tierMetrics).when(appDynamicsDataCollector).getTierBusinessTransactionMetrics();
    doReturn(appdynamicsRestClient).when(appDynamicsDataCollector).getAppDynamicsRestClient();
    doReturn("").when(appDynamicsDataCollector).getHeaderWithCredentials();

    appDynamicsDataCollector.init(dataCollectionExecutionContext, dataCollectionInfo);
    FieldUtils.writeField(appDynamicsDataCollector, "dataCollectionService", dataCollectionService, true);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testInit() {
    verify(appDynamicsDataCollector, times(1)).getAppDynamicsTier();
    verify(appDynamicsDataCollector, times(1)).getTierBusinessTransactionMetrics();
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testFetchMetrics_withHost() {
    ArgumentCaptor<String> titleCaptor = ArgumentCaptor.forClass(String.class);
    appDynamicsDataCollector.fetchMetrics(new ArrayList<>(dataCollectionInfo.getHosts()));
    verify(dataCollectionExecutionContext, times(1)).executeRequest(titleCaptor.capture(), any());

    String title = titleCaptor.getValue();
    assertThat(title).isEqualTo(
        "Fetching data for metric path: Business Transaction Performance|Business Transactions|tier|Calls per Minute|Individual Nodes|host|*");
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testFetchMetrics_withoutHost() {
    ArgumentCaptor<String> titleCaptor = ArgumentCaptor.forClass(String.class);
    appDynamicsDataCollector.fetchMetrics();
    verify(dataCollectionExecutionContext, times(1)).executeRequest(titleCaptor.capture(), any());

    String title = titleCaptor.getValue();
    assertThat(title).isEqualTo(
        "Fetching data for metric path: Business Transaction Performance|Business Transactions|tier|Calls per Minute|*");
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testParseAppDynamicsResponse() {
    List<Optional<List<AppdynamicsMetricData>>> appDynamicsMetricDataOptionalList = new ArrayList<>();
    appDynamicsMetricDataOptionalList.add(Optional.of(Collections.singletonList(response)));

    List<MetricElement> metricElements =
        appDynamicsDataCollector.parseAppDynamicsResponse(appDynamicsMetricDataOptionalList, "host");

    assertThat(metricElements.size()).isEqualTo(1);
    assertThat(metricElements.get(0).getName()).isEqualTo("/todolist");
    assertThat(metricElements.get(0).getGroupName()).isEqualTo("tier");
    assertThat(metricElements.get(0).getValues().keySet())
        .isEqualTo(new HashSet<>(Collections.singletonList("Calls per Minute")));
  }
}