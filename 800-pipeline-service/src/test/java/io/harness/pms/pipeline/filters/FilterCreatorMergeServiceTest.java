package io.harness.pms.pipeline.filters;

import static io.harness.rule.OwnerRule.SAHIL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.harness.PipelineServiceTestBase;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.plan.FilterCreationBlobResponse;
import io.harness.pms.plan.PlanCreationServiceGrpc;
import io.harness.pms.plan.YamlFieldBlob;
import io.harness.pms.plan.creation.PlanCreatorServiceInfo;
import io.harness.pms.sdk.PmsSdkInstanceService;
import io.harness.pms.sdk.core.FilterCreatorMergeService;
import io.harness.pms.sdk.core.FilterCreatorMergeServiceResponse;
import io.harness.rule.Owner;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

public class FilterCreatorMergeServiceTest extends PipelineServiceTestBase {
  private static final String pipelineYaml = "pipeline:\n"
      + "  identifier: p1\n"
      + "  name: pipeline1\n"
      + "  stages:\n"
      + "    - stage:\n"
      + "        identifier: managerDeployment\n"
      + "        type: deployment\n"
      + "        name: managerDeployment\n"
      + "        spec:\n"
      + "          service:\n"
      + "            identifier: manager\n"
      + "            name: manager\n"
      + "            serviceDefinition:\n"
      + "              type: k8s\n"
      + "              spec:\n"
      + "                field11: value1\n"
      + "                field12: value2\n"
      + "          infrastructure:\n"
      + "            environment:\n"
      + "              identifier: stagingInfra\n"
      + "              type: preProduction\n"
      + "              name: staging\n"
      + "            infrastructureDefinition:\n"
      + "              type: k8sDirect\n"
      + "              spec:\n"
      + "                connectorRef: pEIkEiNPSgSUsbWDDyjNKw\n"
      + "                namespace: harness\n"
      + "                releaseName: testingqa\n"
      + "          execution:\n"
      + "            steps:\n"
      + "              - step:\n"
      + "                  identifier: managerCanary\n"
      + "                  type: k8sCanary\n"
      + "                  spec:\n"
      + "                    field11: value1\n"
      + "                    field12: value2\n"
      + "              - step:\n"
      + "                  identifier: managerVerify\n"
      + "                  type: appdVerify\n"
      + "                  spec:\n"
      + "                    field21: value1\n"
      + "                    field22: value2\n"
      + "              - step:\n"
      + "                  identifier: managerRolling\n"
      + "                  type: k8sRolling\n"
      + "                  spec:\n"
      + "                    field31: value1\n"
      + "                    field32: value2";

  PlanCreationServiceGrpc.PlanCreationServiceBlockingStub planCreationServiceBlockingStub;
  @Mock PmsSdkInstanceService pmsSdkInstanceService;
  FilterCreatorMergeService filterCreatorMergeService;

  @Before
  public void init() {
    Map<String, PlanCreationServiceGrpc.PlanCreationServiceBlockingStub> map = new HashMap<>();
    filterCreatorMergeService = spy(new FilterCreatorMergeService(map, pmsSdkInstanceService));
  }

  @After
  public void verifyInteractions() {
    verifyNoMoreInteractions(pmsSdkInstanceService);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testGetPipelineInfo() throws IOException {
    Map<String, Set<String>> stepToSupportedTypes = new HashMap<>();
    stepToSupportedTypes.put("pipeline", Collections.singleton("__any__"));
    Map<String, Map<String, Set<String>>> sdkInstances = new HashMap<>();
    sdkInstances.put("cd", stepToSupportedTypes);
    when(pmsSdkInstanceService.getInstanceNameToSupportedTypes()).thenReturn(sdkInstances);

    doReturn(FilterCreationBlobResponse.newBuilder().build())
        .when(filterCreatorMergeService)
        .obtainFiltersRecursively(any(), any(), any());

    FilterCreatorMergeServiceResponse filterCreatorMergeServiceResponse =
        filterCreatorMergeService.getPipelineInfo(pipelineYaml);

    assertThat(filterCreatorMergeServiceResponse)
        .isEqualTo(FilterCreatorMergeServiceResponse.builder()
                       .filters(new HashMap<>())
                       .layoutNodeMap(new HashMap<>())
                       .startingNodeId("")
                       .build());
    verify(pmsSdkInstanceService).getInstanceNameToSupportedTypes();
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testValidateFilterCreationBlobResponse() {
    Map<String, YamlFieldBlob> dependencies = new HashMap<>();
    dependencies.put("pipeline", YamlFieldBlob.newBuilder().build());
    FilterCreationBlobResponse filterCreationBlobResponse =
        FilterCreationBlobResponse.newBuilder().putAllDependencies(dependencies).build();
    assertThatThrownBy(() -> filterCreatorMergeService.validateFilterCreationBlobResponse(filterCreationBlobResponse))
        .isInstanceOf(InvalidRequestException.class);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testObtainFiltersRecursivelyNoDependencies() {
    Map<String, PlanCreatorServiceInfo> services = new HashMap<>();
    services.put("cd", new PlanCreatorServiceInfo(new HashMap<>(), null));

    FilterCreationBlobResponse response =
        filterCreatorMergeService.obtainFiltersRecursively(services, new HashMap<>(), new HashMap<>());

    assertThat(response).isEqualTo(FilterCreationBlobResponse.newBuilder().build());
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testObtainFiltersRecursivelyNoServices() {
    Map<String, PlanCreatorServiceInfo> services = new HashMap<>();
    FilterCreationBlobResponse response =
        filterCreatorMergeService.obtainFiltersRecursively(services, new HashMap<>(), new HashMap<>());
    assertThat(response).isEqualTo(FilterCreationBlobResponse.newBuilder().build());
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testObtainFiltersRecursively() {
    Map<String, PlanCreatorServiceInfo> services = new HashMap<>();
    services.put("cd", new PlanCreatorServiceInfo(new HashMap<>(), null));
    Map<String, YamlFieldBlob> dependencies = new HashMap<>();
    dependencies.put("pipeline", YamlFieldBlob.newBuilder().build());

    doReturn(FilterCreationBlobResponse.newBuilder().putAllResolvedDependencies(dependencies).build())
        .when(filterCreatorMergeService)
        .obtainFiltersPerIteration(services, dependencies, new HashMap<>());

    FilterCreationBlobResponse response =
        filterCreatorMergeService.obtainFiltersRecursively(services, dependencies, new HashMap<>());

    assertThat(response).isEqualTo(
        FilterCreationBlobResponse.newBuilder().putAllResolvedDependencies(dependencies).build());

    verify(filterCreatorMergeService).obtainFiltersPerIteration(any(), any(), any());
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testObtainFiltersRecursivelyWithUnresolvedDependencies() {
    Map<String, PlanCreatorServiceInfo> services = new HashMap<>();
    services.put("cd", new PlanCreatorServiceInfo(new HashMap<>(), null));
    Map<String, YamlFieldBlob> dependencies = new HashMap<>();
    dependencies.put("pipeline", YamlFieldBlob.newBuilder().build());

    doReturn(FilterCreationBlobResponse.newBuilder().build())
        .when(filterCreatorMergeService)
        .obtainFiltersPerIteration(services, dependencies, new HashMap<>());

    assertThatThrownBy(
        () -> filterCreatorMergeService.obtainFiltersRecursively(services, dependencies, new HashMap<>()))
        .isInstanceOf(InvalidRequestException.class);

    verify(filterCreatorMergeService).obtainFiltersPerIteration(any(), any(), any());
  }
}