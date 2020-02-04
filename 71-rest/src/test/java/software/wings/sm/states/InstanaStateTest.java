package software.wings.sm.states;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KAMAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.beans.AccountType;
import software.wings.beans.Application;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.instana.InstanaDataCollectionInfo;
import software.wings.service.impl.instana.InstanaTagFilter;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.MetricDataAnalysisService;
import software.wings.service.intfc.verification.CVActivityLogService;
import software.wings.sm.StateType;
import software.wings.verification.VerificationStateAnalysisExecutionData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InstanaStateTest extends APMStateVerificationTestBase {
  private InstanaState instanaState;
  private List<InstanaTagFilter> instanaTagFilters;
  @Mock private CVActivityLogService.Logger activityLogger;
  @Mock private MetricDataAnalysisService metricAnalysisService;

  @Before
  public void setup() throws IllegalAccessException {
    setupCommon();
    MockitoAnnotations.initMocks(this);
    setupCommonMocks();

    AppService appService = mock(AppService.class);
    when(appService.getAccountIdByAppId(anyString())).thenReturn(generateUuid());
    when(appService.get(anyString()))
        .thenReturn(Application.Builder.anApplication().name(generateUuid()).accountId(accountId).build());

    AccountService accountService = mock(AccountService.class);
    when(accountService.getAccountType(anyString())).thenReturn(Optional.of(AccountType.PAID));

    instanaState = new InstanaState("InstanaState");
    instanaState.setQuery("entity.kubernetes.pod.name:${host.hostName}");
    instanaState.setTimeDuration("15");
    instanaState.setMetrics(Lists.newArrayList("cpu.total_usage", "memory.usage"));
    instanaState.setHostTagFilter("kubernetes.pod.name");
    instanaTagFilters = new ArrayList<>();
    instanaTagFilters.add(InstanaTagFilter.builder()
                              .name("kubernetes.cluster.name")
                              .operator(InstanaTagFilter.Operator.EQUALS)
                              .value("harness-test")
                              .build());
    instanaState.setTagFilters(instanaTagFilters);
    setupCommonFields(instanaState);
    FieldUtils.writeField(instanaState, "accountService", accountService, true);
    FieldUtils.writeField(instanaState, "metricAnalysisService", metricAnalysisService, true);
    when(cvActivityLogService.getLoggerByStateExecutionId(anyString())).thenReturn(activityLogger);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testCreateDataCollectionInfo() {
    Map<String, String> hosts = new HashMap<>();
    hosts.put("host1", "default");
    InstanaDataCollectionInfo instanaDataCollectionInfo =
        (InstanaDataCollectionInfo) instanaState.createDataCollectionInfo(executionContext, hosts);
    assertThat(StateType.INSTANA).isEqualTo(instanaDataCollectionInfo.getStateType());
    assertThat(instanaDataCollectionInfo.getInstanaConfig()).isNull();
    assertThat(instanaDataCollectionInfo.getStateExecutionId())
        .isEqualTo(executionContext.getStateExecutionInstanceId());
    assertThat(instanaDataCollectionInfo.getQuery()).isEqualTo(instanaState.getQuery());
    assertThat(instanaDataCollectionInfo.getMetrics()).isEqualTo(instanaState.getMetrics());
    assertThat(instanaDataCollectionInfo.getConnectorId()).isEqualTo(instanaState.getAnalysisServerConfigId());
    assertThat(instanaDataCollectionInfo.getHosts()).isEqualTo(Sets.newHashSet("host1"));
    assertThat(instanaDataCollectionInfo.getHostsToGroupNameMap()).isEqualTo(hosts);
    assertThat(instanaDataCollectionInfo.getTagFilters()).isEqualTo(instanaTagFilters);
    assertThat(instanaDataCollectionInfo.getHostTagFilter()).isEqualTo("kubernetes.pod.name");
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testIsCVTaskEnqueuingEnabled() {
    assertThat(instanaState.isCVTaskEnqueuingEnabled(generateUuid())).isTrue();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testTriggerAnalysisDataCollection() {
    Map<String, String> hosts = new HashMap<>();
    hosts.put("host1", "default");
    assertThatThrownBy(
        ()
            -> instanaState.triggerAnalysisDataCollection(executionContext, AnalysisContext.builder().build(),
                mock(VerificationStateAnalysisExecutionData.class), hosts))
        .isInstanceOf(UnsupportedOperationException.class);
  }
}