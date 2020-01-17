package software.wings.sm.states;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.RAGHU;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.service.impl.newrelic.NewRelicMetricDataRecord.DEFAULT_GROUP_NAME;
import static software.wings.sm.ExecutionContextImpl.PHASE_PARAM;
import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.context.ContextElementType;
import io.harness.rule.Owner;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.WingsBaseTest;
import software.wings.api.DeploymentType;
import software.wings.api.PhaseElement;
import software.wings.api.ServiceElement;
import software.wings.beans.AwsConfig;
import software.wings.beans.EcsInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Service;
import software.wings.beans.SyncTaskContext;
import software.wings.cloudprovider.ContainerInfo;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.ContainerMetadata;
import software.wings.service.impl.ContainerServiceParams;
import software.wings.service.impl.instance.ContainerInstanceHandler;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ContainerService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.StateExecutionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ExecutionContextImpl;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class EcsVerificationStateTest extends WingsBaseTest {
  private String accountId;
  private String appId;
  private String stateExecutionId;
  private InfrastructureMapping infrastructureMapping;
  @Mock private ExecutionContextImpl executionContext;
  @Mock private DelegateProxyFactory delegateProxyFactory;
  @Mock private ContainerService containerService;
  @Mock private ContainerInstanceHandler containerInstanceHandler;
  private AbstractAnalysisState abstractAnalysisState;

  @Inject private WingsPersistence wingsPersistence;
  @Inject private SettingsService settingsService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private InfrastructureMappingService infraMappingService;
  @Inject private StateExecutionService stateExecutionService;
  @Inject private SecretManager secretManager;
  @Inject private AppService appService;

  @Before
  public void setup() throws IllegalAccessException, IOException {
    MockitoAnnotations.initMocks(this);
    accountId = generateUuid();
    appId = wingsPersistence.save(anApplication().accountId(accountId).name(generateUuid()).build());
    stateExecutionId = generateUuid();

    final String serviceId =
        wingsPersistence.save(Service.builder().deploymentType(DeploymentType.ECS).isK8sV2(false).appId(appId).build());
    final String configId = wingsPersistence.save(aSettingAttribute()
                                                      .withAccountId(accountId)
                                                      .withAppId(appId)
                                                      .withValue(AwsConfig.builder()
                                                                     .accessKey(generateUuid())
                                                                     .secretKey(generateUuid().toCharArray())
                                                                     .accountId(accountId)
                                                                     .build())
                                                      .build());
    final String infraMappingId = wingsPersistence.save(EcsInfrastructureMapping.Builder.anEcsInfrastructureMapping()
                                                            .withClusterName(generateUuid())
                                                            .withRegion(generateUuid())
                                                            .withUuid(generateUuid())
                                                            .withComputeProviderSettingId(configId)
                                                            .withAppId(appId)
                                                            .withAccountId(accountId)
                                                            .build());

    abstractAnalysisState = mock(AbstractAnalysisState.class, CALLS_REAL_METHODS);
    when(executionContext.getContextElement(ContextElementType.PARAM, PHASE_PARAM))
        .thenReturn(PhaseElement.builder().serviceElement(ServiceElement.builder().uuid(serviceId).build()).build());
    when(executionContext.fetchInfraMappingId()).thenReturn(infraMappingId);
    when(executionContext.getAppId()).thenReturn(appId);
    when(executionContext.getStateExecutionInstance())
        .thenReturn(aStateExecutionInstance().appId(appId).uuid(generateUuid()).build());
    when(abstractAnalysisState.getLogger()).thenReturn(logger);
    when(containerInstanceHandler.getContainerServiceNames(
             executionContext, serviceId, infraMappingId, Optional.empty()))
        .thenReturn(Sets.newHashSet(ContainerMetadata.builder().namespace(generateUuid()).build()));
    when(containerInstanceHandler.isContainerDeployment(any())).thenReturn(true);
    when(delegateProxyFactory.get(eq(ContainerService.class), any(SyncTaskContext.class))).thenReturn(containerService);
    FieldUtils.writeField(abstractAnalysisState, "wingsPersistence", wingsPersistence, true);
    FieldUtils.writeField(abstractAnalysisState, "serviceResourceService", serviceResourceService, true);
    FieldUtils.writeField(abstractAnalysisState, "settingsService", settingsService, true);
    FieldUtils.writeField(abstractAnalysisState, "infraMappingService", infraMappingService, true);
    FieldUtils.writeField(abstractAnalysisState, "stateExecutionService", stateExecutionService, true);
    FieldUtils.writeField(abstractAnalysisState, "containerInstanceHandler", containerInstanceHandler, true);
    FieldUtils.writeField(abstractAnalysisState, "secretManager", secretManager, true);
    FieldUtils.writeField(abstractAnalysisState, "appService", appService, true);
    FieldUtils.writeField(abstractAnalysisState, "delegateProxyFactory", delegateProxyFactory, true);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testNoContainerIdFound() {
    when(containerService.fetchContainerInfos(any(ContainerServiceParams.class)))
        .thenReturn(Lists.newArrayList(ContainerInfo.builder().containerTasksReachable(false).build()));
    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> abstractAnalysisState.getLastExecutionNodes(executionContext));
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testContainerIdFound() {
    final ContainerInfo containerInfo = ContainerInfo.builder()
                                            .hostName(generateUuid())
                                            .containerId(generateUuid())
                                            .containerTasksReachable(true)
                                            .build();
    when(containerService.fetchContainerInfos(any(ContainerServiceParams.class)))
        .thenReturn(Lists.newArrayList(containerInfo));

    final Map<String, String> lastExecutionNodes = abstractAnalysisState.getLastExecutionNodes(executionContext);
    assertThat(lastExecutionNodes.size()).isEqualTo(1);
    assertThat(lastExecutionNodes.get(containerInfo.getContainerId())).isEqualTo(DEFAULT_GROUP_NAME);
  }
}
