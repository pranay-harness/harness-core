package software.wings.service.impl.instana;

import static io.harness.rule.OwnerRule.KAMAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.InstanaConfig;
import software.wings.beans.SettingAttribute;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.delegatetasks.cv.DataCollectionException;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.instana.InstanaDelegateService;
import software.wings.service.intfc.instana.InstanaService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class InstanaServiceImplTest extends WingsBaseTest {
  private String accountId = UUID.randomUUID().toString();
  @Mock private SettingsService settingsService;
  @Mock private DelegateProxyFactory delegateProxyFactory;
  @Mock private InstanaDelegateService instanaDelegateService;
  @Inject InstanaService instanaService;
  private InstanaConfig instanaConfig;
  private SettingAttribute settingAttribute;

  @Before
  public void setup() throws IllegalAccessException, IOException {
    initMocks(this);
    FieldUtils.writeField(instanaService, "delegateProxyFactory", delegateProxyFactory, true);
    FieldUtils.writeField(instanaService, "settingsService", settingsService, true);
    instanaConfig = InstanaConfig.builder()
                        .instanaUrl("https://instana-example.com/")
                        .accountId(accountId)
                        .apiToken(UUID.randomUUID().toString().toCharArray())
                        .build();
    settingAttribute = aSettingAttribute()
                           .withCategory(SettingAttribute.SettingCategory.CONNECTOR)
                           .withName("Instana" + System.currentTimeMillis())
                           .withAccountId(accountId)
                           .withValue(instanaConfig)
                           .build();

    when(settingsService.get(any())).thenReturn(settingAttribute);
    when(delegateProxyFactory.get(any(), any())).thenReturn(instanaDelegateService);
  }
  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetMetricsWithDataForNode_IfNoMetricsReturned() {
    InstanaInfraMetrics instanaInfraMetrics = InstanaInfraMetrics.builder().items(Collections.emptyList()).build();
    when(instanaDelegateService.getInfraMetrics(any(), any(), any(), any())).thenReturn(instanaInfraMetrics);
    List<String> metrics = Lists.newArrayList("cpu.total_usage", "memory.usage");
    InstanaSetupTestNodeData instanaSetupTestNodeData = InstanaSetupTestNodeData.builder()
                                                            .settingId(settingAttribute.getUuid())
                                                            .query("entity.kubernetes.pod.name:${host.hostName}")
                                                            .metrics(metrics)
                                                            .build();
    InstanaAnalyzeMetrics instanaAnalyzeMetrics = InstanaAnalyzeMetrics.builder().build();
    instanaAnalyzeMetrics.setItems(Collections.emptyList());
    when(instanaDelegateService.getInstanaTraceMetrics(any(), any(), any(), any())).thenReturn(instanaAnalyzeMetrics);
    VerificationNodeDataSetupResponse verificationNodeDataSetupResponse =
        instanaService.getMetricsWithDataForNode(instanaSetupTestNodeData);
    assertThat(verificationNodeDataSetupResponse.isProviderReachable()).isTrue();
    assertThat(verificationNodeDataSetupResponse.getLoadResponse()).isNull();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetMetricsWithDataForNode_IfConnectionFailed() {
    when(instanaDelegateService.getInfraMetrics(any(), any(), any(), any()))
        .thenThrow(new DataCollectionException("failed to connect"));
    List<String> metrics = Lists.newArrayList("cpu.total_usage", "memory.usage");
    InstanaSetupTestNodeData instanaSetupTestNodeData = InstanaSetupTestNodeData.builder()
                                                            .settingId(settingAttribute.getUuid())
                                                            .query("entity.kubernetes.pod.name:${host.hostName}")
                                                            .metrics(metrics)
                                                            .build();

    when(instanaDelegateService.getInstanaTraceMetrics(any(), any(), any(), any()))
        .thenThrow(new DataCollectionException("failed to connect"));
    VerificationNodeDataSetupResponse verificationNodeDataSetupResponse =
        instanaService.getMetricsWithDataForNode(instanaSetupTestNodeData);
    assertThat(verificationNodeDataSetupResponse.isProviderReachable()).isFalse();
    assertThat(verificationNodeDataSetupResponse.getLoadResponse()).isNull();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetMetricsWithDataForNode_withLoad() {
    Map<String, List<List<Number>>> metricsMap = new HashMap<>();
    List<List<Number>> memoryUsage = new ArrayList<>();
    memoryUsage.add(Lists.newArrayList(Long.valueOf(1579157460000L), Double.valueOf(.5)));
    metricsMap.put("memory.usage", memoryUsage);
    InstanaMetricItem instanaMetricItem =
        InstanaMetricItem.builder()
            .label("hs-harness-todolist-hs (default/cv-pr-kube-kube-v1-service-kube-env-1-9c7c76f7c-sz7n4)")
            .from(1579157474000L)
            .to(1579161206000L)
            .metrics(metricsMap)
            .build();

    InstanaInfraMetrics instanaInfraMetrics =
        InstanaInfraMetrics.builder().items(Lists.newArrayList(instanaMetricItem)).build();
    when(instanaDelegateService.getInfraMetrics(any(), any(), any(), any())).thenReturn(instanaInfraMetrics);

    InstanaAnalyzeMetrics instanaAnalyzeMetrics = InstanaAnalyzeMetrics.builder().build();
    InstanaAnalyzeMetrics.Item item = InstanaAnalyzeMetrics.Item.builder().build();
    instanaAnalyzeMetrics.setItems(Lists.newArrayList(item));
    when(instanaDelegateService.getInstanaTraceMetrics(any(), any(), any(), any())).thenReturn(instanaAnalyzeMetrics);

    List<String> metrics = Lists.newArrayList("cpu.total_usage", "memory.usage");
    InstanaSetupTestNodeData instanaSetupTestNodeData = InstanaSetupTestNodeData.builder()
                                                            .settingId(settingAttribute.getUuid())
                                                            .query("entity.kubernetes.pod.name:${host.hostName}")
                                                            .metrics(metrics)
                                                            .build();
    VerificationNodeDataSetupResponse verificationNodeDataSetupResponse =
        instanaService.getMetricsWithDataForNode(instanaSetupTestNodeData);
    assertThat(verificationNodeDataSetupResponse.isProviderReachable()).isTrue();
    assertThat(verificationNodeDataSetupResponse.isProviderReachable()).isTrue();
    assertThat(verificationNodeDataSetupResponse.getLoadResponse()).isNotNull();
    assertThat(verificationNodeDataSetupResponse.getLoadResponse().isLoadPresent()).isTrue();
    assertThat(verificationNodeDataSetupResponse.getLoadResponse().getLoadResponse()).isNotNull();
    assertThat(verificationNodeDataSetupResponse.getLoadResponse().getLoadResponse())
        .isEqualTo(Lists.newArrayList(instanaInfraMetrics, instanaAnalyzeMetrics));
  }
}