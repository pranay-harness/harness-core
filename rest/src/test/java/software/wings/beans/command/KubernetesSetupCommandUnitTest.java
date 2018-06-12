package software.wings.beans.command;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.command.CommandExecutionContext.Builder.aCommandExecutionContext;
import static software.wings.beans.command.KubernetesSetupParams.KubernetesSetupParamsBuilder.aKubernetesSetupParams;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;

import com.google.common.collect.ImmutableMap;

import io.fabric8.kubernetes.api.model.HorizontalPodAutoscaler;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.GcpConfig;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.KubernetesSetupParams.KubernetesSetupParamsBuilder;
import software.wings.beans.container.ImageDetails;
import software.wings.beans.container.KubernetesBlueGreenConfig;
import software.wings.beans.container.KubernetesPortProtocol;
import software.wings.beans.container.KubernetesServiceSpecification;
import software.wings.beans.container.KubernetesServiceType;
import software.wings.cloudprovider.gke.GkeClusterService;
import software.wings.cloudprovider.gke.KubernetesContainerService;
import software.wings.exception.InvalidRequestException;
import software.wings.utils.KubernetesConvention;

import java.lang.reflect.InvocationTargetException;
import java.time.Clock;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KubernetesSetupCommandUnitTest extends WingsBaseTest {
  @Mock private GkeClusterService gkeClusterService;
  @Mock private KubernetesContainerService kubernetesContainerService;
  @Mock private Clock clock;

  @InjectMocks private KubernetesSetupCommandUnit kubernetesSetupCommandUnit = new KubernetesSetupCommandUnit();

  private KubernetesConfig kubernetesConfig = KubernetesConfig.builder()
                                                  .masterUrl("https://1.1.1.1/")
                                                  .username("admin")
                                                  .password("password".toCharArray())
                                                  .namespace("default")
                                                  .build();
  private KubernetesSetupParams setupParams =
      aKubernetesSetupParams()
          .withAppName(APP_NAME)
          .withEnvName(ENV_NAME)
          .withServiceName(SERVICE_NAME)
          .withImageDetails(ImageDetails.builder()
                                .registryUrl("gcr.io")
                                .sourceName("GCR")
                                .name("exploration-161417/todolist")
                                .tag("v1")
                                .build())
          .withInfraMappingId(INFRA_MAPPING_ID)
          .withPort(80)
          .withProtocol(KubernetesPortProtocol.TCP)
          .withServiceType(KubernetesServiceType.ClusterIP)
          .withTargetPort(8080)
          .withControllerNamePrefix(APP_NAME + "." + ENV_NAME + "." + SERVICE_NAME)
          .withClusterName("cluster")
          .build();
  private SettingAttribute computeProvider = aSettingAttribute().withValue(GcpConfig.builder().build()).build();
  private CommandExecutionContext context = aCommandExecutionContext()
                                                .withCloudProviderSetting(computeProvider)
                                                .withContainerSetupParams(setupParams)
                                                .withCloudProviderCredentials(emptyList())
                                                .build();

  /**
   * Set up.
   */
  @Before
  public void setup() {
    ReplicationController replicationController =
        new ReplicationControllerBuilder()
            .withApiVersion("v1")
            .withNewMetadata()
            .withName("backend-ctrl")
            .addToLabels("app", "testApp")
            .addToLabels("tier", "backend")
            .endMetadata()
            .withNewSpec()
            .withReplicas(2)
            .withNewTemplate()
            .withNewMetadata()
            .addToLabels("app", "testApp")
            .addToLabels("tier", "backend")
            .endMetadata()
            .withNewSpec()
            .addNewContainer()
            .withName("server")
            .withImage("gcr.io/exploration-161417/todolist")
            .withArgs("8080")
            .withNewResources()
            .withLimits(ImmutableMap.of("cpu", new Quantity("100m"), "memory", new Quantity("100Mi")))
            .endResources()
            .addNewPort()
            .withContainerPort(8080)
            .endPort()
            .endContainer()
            .endSpec()
            .endTemplate()
            .endSpec()
            .build();

    Secret secret = new SecretBuilder()
                        .withApiVersion("v1")
                        .withKind("Secret")
                        .withData(ImmutableMap.of(".dockercfg", "aaa"))
                        .withNewMetadata()
                        .withName("secret-name")
                        .endMetadata()
                        .build();

    io.fabric8.kubernetes.api.model.Service kubernetesService = new ServiceBuilder()
                                                                    .withApiVersion("v1")
                                                                    .withNewMetadata()
                                                                    .withName("backend-service")
                                                                    .addToLabels("app", "testApp")
                                                                    .addToLabels("tier", "backend")
                                                                    .endMetadata()
                                                                    .withNewSpec()
                                                                    .withType("LoadBalancer")
                                                                    .addNewPort()
                                                                    .withPort(80)
                                                                    .withNewTargetPort()
                                                                    .withIntVal(8080)
                                                                    .endTargetPort()
                                                                    .endPort()
                                                                    .addToSelector("app", "testApp")
                                                                    .addToSelector("tier", "backend")
                                                                    .withClusterIP("1.2.3.4")
                                                                    .endSpec()
                                                                    .withNewStatus()
                                                                    .withNewLoadBalancer()
                                                                    .addNewIngress()
                                                                    .withIp("5.6.7.8")
                                                                    .endIngress()
                                                                    .endLoadBalancer()
                                                                    .endStatus()
                                                                    .build();

    when(gkeClusterService.getCluster(any(SettingAttribute.class), eq(emptyList()), anyString(), anyString()))
        .thenReturn(kubernetesConfig);
    when(kubernetesContainerService.createOrReplaceController(
             eq(kubernetesConfig), eq(emptyList()), any(ReplicationController.class)))
        .thenReturn(replicationController);
    when(kubernetesContainerService.listControllers(kubernetesConfig, emptyList())).thenReturn(null);
    when(kubernetesContainerService.createOrReplaceService(
             eq(kubernetesConfig), eq(emptyList()), any(io.fabric8.kubernetes.api.model.Service.class)))
        .thenReturn(kubernetesService);
    when(kubernetesContainerService.createOrReplaceSecret(eq(kubernetesConfig), eq(emptyList()), any(Secret.class)))
        .thenReturn(secret);
  }

  @Test
  public void shouldExecuteWithLastService() {
    ReplicationController kubernetesReplicationController =
        new ReplicationControllerBuilder()
            .withNewMetadata()
            .withName(KubernetesConvention.getControllerName(
                KubernetesConvention.getControllerNamePrefix("app", "service", "env", false), 1, false))
            .withCreationTimestamp(new Date().toString())
            .endMetadata()
            .build();

    when(kubernetesContainerService.listControllers(kubernetesConfig, emptyList()))
        .thenReturn((List) singletonList(kubernetesReplicationController));

    CommandExecutionStatus status = kubernetesSetupCommandUnit.execute(context);
    assertThat(status).isEqualTo(CommandExecutionStatus.SUCCESS);
    verify(gkeClusterService).getCluster(any(SettingAttribute.class), eq(emptyList()), anyString(), anyString());
    verify(kubernetesContainerService)
        .createOrReplaceController(eq(kubernetesConfig), any(), any(ReplicationController.class));
  }

  @Test
  public void testCreateCustomMetricHorizontalPodAutoscalar() throws Exception {
    ExecutionLogCallback executionLogCallback = mock(ExecutionLogCallback.class);
    doNothing().when(executionLogCallback).saveExecutionLog(anyString(), any());

    Map labels = new HashMap();
    labels.put("app", "appName");
    labels.put("version", "9");

    String yamlForHPAWithCustomMetric = "apiVersion: autoscaling/v2beta1\n"
        + "kind: HorizontalPodAutoscaler\n"
        + "metadata:\n"
        + "  name: none\n"
        + "  namespace: none\n"
        + "spec:\n"
        + "  scaleTargetRef:\n"
        + "    kind: none\n"
        + "    name: none\n"
        + "  minReplicas: 3\n"
        + "  maxReplicas: 6\n"
        + "  metrics:\n"
        + "  - type: Resource\n"
        + "    resource:\n"
        + "      name: cpu\n"
        + "      targetAverageUtilization: 70\n";

    KubernetesSetupParams setupParams = KubernetesSetupParamsBuilder
                                            .aKubernetesSetupParams()
                                            // use customMetricHPA
                                            .withCustomMetricYamlConfig(yamlForHPAWithCustomMetric)
                                            .build();

    HorizontalPodAutoscaler horizontalPodAutoscaler =
        (HorizontalPodAutoscaler) MethodUtils.invokeMethod(kubernetesSetupCommandUnit, true, "createAutoscaler",
            new Object[] {"autoScalarName", "default", labels, setupParams, executionLogCallback});

    assertEquals("autoscaling/v2beta1", horizontalPodAutoscaler.getApiVersion());
    assertNotNull(horizontalPodAutoscaler.getSpec());
    assertEquals("none", horizontalPodAutoscaler.getSpec().getScaleTargetRef().getName());
    assertEquals("none", horizontalPodAutoscaler.getSpec().getScaleTargetRef().getKind());
    assertNotNull(horizontalPodAutoscaler.getMetadata());
    assertEquals("autoScalarName", horizontalPodAutoscaler.getMetadata().getName());
    assertEquals("default", horizontalPodAutoscaler.getMetadata().getNamespace());
    assertNotNull(horizontalPodAutoscaler.getMetadata().getLabels());
    assertTrue(horizontalPodAutoscaler.getMetadata().getLabels().containsKey("app"));
    assertTrue(horizontalPodAutoscaler.getMetadata().getLabels().containsKey("version"));
    assertEquals("appName", horizontalPodAutoscaler.getMetadata().getLabels().get("app"));
    assertEquals("9", horizontalPodAutoscaler.getMetadata().getLabels().get("version"));
    assertNotNull(horizontalPodAutoscaler.getSpec().getAdditionalProperties());
    assertEquals(1, horizontalPodAutoscaler.getSpec().getAdditionalProperties().size());
    assertEquals("metrics", horizontalPodAutoscaler.getSpec().getAdditionalProperties().keySet().iterator().next());
    assertEquals(Integer.valueOf(3), horizontalPodAutoscaler.getSpec().getMinReplicas());
    assertEquals(Integer.valueOf(6), horizontalPodAutoscaler.getSpec().getMaxReplicas());
  }

  @Test
  public void testBasicHorizontalPodAutoscalar() throws Exception {
    ExecutionLogCallback executionLogCallback = mock(ExecutionLogCallback.class);
    doNothing().when(executionLogCallback).saveExecutionLog(anyString(), any());

    Map labels = new HashMap();
    labels.put("app", "appName");
    labels.put("version", "9");

    KubernetesSetupParams setupParams = KubernetesSetupParamsBuilder
                                            .aKubernetesSetupParams()
                                            // null customMetricConfigYaml, so use basic HPA
                                            .withCustomMetricYamlConfig(null)
                                            .withMinAutoscaleInstances(1)
                                            .withMaxAutoscaleInstances(2)
                                            .withTargetCpuUtilizationPercentage(20)
                                            .build();

    HorizontalPodAutoscaler horizontalPodAutoscaler =
        (HorizontalPodAutoscaler) MethodUtils.invokeMethod(kubernetesSetupCommandUnit, true, "createAutoscaler",
            new Object[] {"abaris.hpanormal.prod.0", "default", labels, setupParams, executionLogCallback});

    assertEquals("autoscaling/v1", horizontalPodAutoscaler.getApiVersion());
    assertNotNull(horizontalPodAutoscaler.getSpec());
    assertEquals("none", horizontalPodAutoscaler.getSpec().getScaleTargetRef().getName());
    assertEquals("none", horizontalPodAutoscaler.getSpec().getScaleTargetRef().getKind());
    assertNotNull(horizontalPodAutoscaler.getMetadata());
    assertEquals("abaris.hpanormal.prod.0", horizontalPodAutoscaler.getMetadata().getName());
    assertEquals("default", horizontalPodAutoscaler.getMetadata().getNamespace());
    assertNotNull(horizontalPodAutoscaler.getMetadata().getLabels());
    assertTrue(horizontalPodAutoscaler.getMetadata().getLabels().containsKey("app"));
    assertTrue(horizontalPodAutoscaler.getMetadata().getLabels().containsKey("version"));
    assertEquals("appName", horizontalPodAutoscaler.getMetadata().getLabels().get("app"));
    assertEquals("9", horizontalPodAutoscaler.getMetadata().getLabels().get("version"));
    assertEquals(Integer.valueOf(1), horizontalPodAutoscaler.getSpec().getMinReplicas());
    assertEquals(Integer.valueOf(2), horizontalPodAutoscaler.getSpec().getMaxReplicas());
    assertEquals(Integer.valueOf(20), horizontalPodAutoscaler.getSpec().getTargetCPUUtilizationPercentage());

    setupParams = KubernetesSetupParamsBuilder
                      .aKubernetesSetupParams()
                      // empty customMetricConfigYaml, so use basic HPA
                      .withCustomMetricYamlConfig("")
                      .withMinAutoscaleInstances(2)
                      .withMaxAutoscaleInstances(3)
                      .withTargetCpuUtilizationPercentage(30)
                      .build();

    horizontalPodAutoscaler =
        (HorizontalPodAutoscaler) MethodUtils.invokeMethod(kubernetesSetupCommandUnit, true, "createAutoscaler",
            new Object[] {"abaris.hpanormal.prod.0", "default", labels, setupParams, executionLogCallback});

    assertEquals("autoscaling/v1", horizontalPodAutoscaler.getApiVersion());
    assertNotNull(horizontalPodAutoscaler.getSpec());
    assertEquals("none", horizontalPodAutoscaler.getSpec().getScaleTargetRef().getName());
    assertEquals("none", horizontalPodAutoscaler.getSpec().getScaleTargetRef().getKind());
    assertNotNull(horizontalPodAutoscaler.getMetadata());
    assertEquals("abaris.hpanormal.prod.0", horizontalPodAutoscaler.getMetadata().getName());
    assertEquals("default", horizontalPodAutoscaler.getMetadata().getNamespace());
    assertNotNull(horizontalPodAutoscaler.getMetadata().getLabels());
    assertTrue(horizontalPodAutoscaler.getMetadata().getLabels().containsKey("app"));
    assertTrue(horizontalPodAutoscaler.getMetadata().getLabels().containsKey("version"));
    assertEquals("appName", horizontalPodAutoscaler.getMetadata().getLabels().get("app"));
    assertEquals("9", horizontalPodAutoscaler.getMetadata().getLabels().get("version"));
    assertEquals(Integer.valueOf(2), horizontalPodAutoscaler.getSpec().getMinReplicas());
    assertEquals(Integer.valueOf(3), horizontalPodAutoscaler.getSpec().getMaxReplicas());
    assertEquals(Integer.valueOf(30), horizontalPodAutoscaler.getSpec().getTargetCPUUtilizationPercentage());
  }

  @Test
  public void testBlueGreenValidationServiceNotAllowedWithBlueGreen() throws Exception {
    ExecutionLogCallback executionLogCallback = mock(ExecutionLogCallback.class);
    doNothing().when(executionLogCallback).saveExecutionLog(anyString(), any());

    KubernetesSetupParams setupParams = KubernetesSetupParamsBuilder.aKubernetesSetupParams()
                                            .withBlueGreen(true)
                                            .withServiceType(KubernetesServiceType.ClusterIP)
                                            .build();

    try {
      MethodUtils.invokeMethod(kubernetesSetupCommandUnit, true, "validateBlueGreenConfig", new Object[] {setupParams});
      fail("Exception expected");
    } catch (Exception e) {
      assertTrue(((InvocationTargetException) e).getTargetException() instanceof InvalidRequestException);
    }
  }

  @Test
  public void testBlueGreenValidationEmptyBlueGreenConfig() throws Exception {
    ExecutionLogCallback executionLogCallback = mock(ExecutionLogCallback.class);
    doNothing().when(executionLogCallback).saveExecutionLog(anyString(), any());

    KubernetesBlueGreenConfig blueGreenConfig = new KubernetesBlueGreenConfig();
    setupParams = KubernetesSetupParamsBuilder.aKubernetesSetupParams()
                      .withBlueGreen(true)
                      .withBlueGreenConfig(blueGreenConfig)
                      .build();

    try {
      MethodUtils.invokeMethod(kubernetesSetupCommandUnit, true, "validateBlueGreenConfig", new Object[] {setupParams});
      fail("Exception expected");
    } catch (Exception e) {
      assertTrue(((InvocationTargetException) e).getTargetException() instanceof InvalidRequestException);
    }
  }

  @Test
  public void testBlueGreenValidationServiceNonePrimaryService() throws Exception {
    ExecutionLogCallback executionLogCallback = mock(ExecutionLogCallback.class);
    doNothing().when(executionLogCallback).saveExecutionLog(anyString(), any());

    KubernetesServiceSpecification serviceSpec = new KubernetesServiceSpecification();
    serviceSpec.setServiceType(KubernetesServiceType.None);
    KubernetesBlueGreenConfig blueGreenConfig = new KubernetesBlueGreenConfig();
    blueGreenConfig.setPrimaryService(serviceSpec);

    setupParams = KubernetesSetupParamsBuilder.aKubernetesSetupParams()
                      .withBlueGreen(true)
                      .withBlueGreenConfig(blueGreenConfig)
                      .build();

    try {
      MethodUtils.invokeMethod(kubernetesSetupCommandUnit, true, "validateBlueGreenConfig", new Object[] {setupParams});
      fail("Exception expected");
    } catch (Exception e) {
      assertTrue(((InvocationTargetException) e).getTargetException() instanceof InvalidRequestException);
    }
  }

  @Test
  public void testBlueGreenValidationSmokeTest() throws Exception {
    ExecutionLogCallback executionLogCallback = mock(ExecutionLogCallback.class);
    doNothing().when(executionLogCallback).saveExecutionLog(anyString(), any());

    KubernetesServiceSpecification serviceSpec = new KubernetesServiceSpecification();
    serviceSpec.setServiceType(KubernetesServiceType.ClusterIP);
    KubernetesBlueGreenConfig blueGreenConfig = new KubernetesBlueGreenConfig();
    blueGreenConfig.setPrimaryService(serviceSpec);
    blueGreenConfig.setStageService(serviceSpec);

    setupParams = KubernetesSetupParamsBuilder.aKubernetesSetupParams()
                      .withBlueGreen(true)
                      .withBlueGreenConfig(blueGreenConfig)
                      .build();

    MethodUtils.invokeMethod(kubernetesSetupCommandUnit, true, "validateBlueGreenConfig", new Object[] {setupParams});
  }
}
