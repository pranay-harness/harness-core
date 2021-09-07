package io.harness.cvng.core.services.impl.monitoredService;

import static io.harness.rule.OwnerRule.SOWMYA;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.BuilderFactory;
import io.harness.cvng.BuilderFactory.Context;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceDTO.MonitoredServiceRef;
import io.harness.cvng.core.entities.ServiceDependency;
import io.harness.cvng.core.services.api.monitoredService.ServiceDependencyService;
import io.harness.rule.Owner;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ServiceDependencyServiceImplTest extends CvNextGenTestBase {
  @Inject private ServiceDependencyService serviceDependencyService;

  private BuilderFactory builderFactory;
  private Context context;

  @Before
  public void setup() {
    builderFactory = BuilderFactory.getDefault();
    context = builderFactory.getContext();
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testCreateOrDelete_empty() {
    createOrDeleteFromContext(context, new HashSet<>());
    Set<MonitoredServiceRef> updatedRefs = serviceDependencyService.getDependentServicesForMonitoredService(
        context.getProjectParams(), context.getServiceIdentifier());
    assertThat(updatedRefs).isEqualTo(new HashSet<>());
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testCreateOrDelete_create() {
    Set<MonitoredServiceRef> monitoredServiceRefs =
        Sets.newHashSet(MonitoredServiceRef.builder().monitoredServiceIdentifier(randomAlphanumeric(20)).build(),
            MonitoredServiceRef.builder().monitoredServiceIdentifier(randomAlphanumeric(20)).build(),
            MonitoredServiceRef.builder().monitoredServiceIdentifier(randomAlphanumeric(20)).build());
    createOrDeleteFromContext(context, monitoredServiceRefs);
    Set<MonitoredServiceRef> newRefs = serviceDependencyService.getDependentServicesForMonitoredService(
        context.getProjectParams(), context.getServiceIdentifier());
    assertThat(newRefs).isEqualTo(monitoredServiceRefs);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testCreateOrDelete_existingDependencies() {
    List<MonitoredServiceRef> monitoredServiceRefs = generateRandomRefs(4);
    createOrDeleteFromContext(context, new HashSet<>(monitoredServiceRefs));
    Set<MonitoredServiceRef> newRefs = serviceDependencyService.getDependentServicesForMonitoredService(
        context.getProjectParams(), context.getServiceIdentifier());
    assertThat(newRefs).isEqualTo(new HashSet<>(monitoredServiceRefs));

    monitoredServiceRefs =
        Lists.newArrayList(MonitoredServiceRef.builder()
                               .monitoredServiceIdentifier(monitoredServiceRefs.get(0).getMonitoredServiceIdentifier())
                               .build(),
            MonitoredServiceRef.builder().monitoredServiceIdentifier(randomAlphanumeric(20)).build(),
            MonitoredServiceRef.builder().monitoredServiceIdentifier(randomAlphanumeric(20)).build());
    createOrDeleteFromContext(context, new HashSet<>(monitoredServiceRefs));
    Set<MonitoredServiceRef> updatedRefs = serviceDependencyService.getDependentServicesForMonitoredService(
        context.getProjectParams(), context.getServiceIdentifier());
    assertThat(updatedRefs).isEqualTo(new HashSet<>(monitoredServiceRefs));
    assertThat(updatedRefs).isNotEqualTo(newRefs);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testDeleteDependenciesForService() {
    Set<MonitoredServiceRef> monitoredServiceRefs = new HashSet<>(generateRandomRefs(3));
    createOrDeleteFromContext(context, monitoredServiceRefs);
    Set<MonitoredServiceRef> newRefs = serviceDependencyService.getDependentServicesForMonitoredService(
        context.getProjectParams(), context.getServiceIdentifier());
    assertThat(newRefs).isEqualTo(monitoredServiceRefs);

    serviceDependencyService.deleteDependenciesForService(context.getProjectParams(), context.getServiceIdentifier());
    Set<MonitoredServiceRef> updatedRefs = serviceDependencyService.getDependentServicesForMonitoredService(
        context.getProjectParams(), context.getServiceIdentifier());
    assertThat(updatedRefs).isEqualTo(new HashSet<>());
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testGetServiceDependencies() {
    Set<MonitoredServiceRef> monitoredServiceRefs = new HashSet<>(generateRandomRefs(3));
    createOrDeleteFromContext(context, monitoredServiceRefs);

    List<ServiceDependency> serviceDependencies = serviceDependencyService.getServiceDependencies(
        context.getProjectParams(), Lists.newArrayList(context.getServiceIdentifier()));
    assertThat(serviceDependencies.size()).isEqualTo(3);

    serviceDependencies = serviceDependencyService.getServiceDependencies(
        context.getProjectParams(), Lists.newArrayList(context.getServiceIdentifier()));
    assertThat(serviceDependencies.size()).isEqualTo(3);

    serviceDependencies = serviceDependencyService.getServiceDependencies(
        context.getProjectParams(), Lists.newArrayList(context.getServiceIdentifier()));
    assertThat(serviceDependencies.size()).isEqualTo(3);

    serviceDependencies = serviceDependencyService.getServiceDependencies(
        context.getProjectParams(), Lists.newArrayList(context.getServiceIdentifier()));
    assertThat(serviceDependencies.size()).isEqualTo(3);
  }

  private void createOrDeleteFromContext(Context context, Set<MonitoredServiceRef> monitoredServiceRefs) {
    serviceDependencyService.updateDependencies(
        context.getProjectParams(), context.getServiceIdentifier(), monitoredServiceRefs);
  }

  private List<MonitoredServiceRef> generateRandomRefs(int num) {
    List<MonitoredServiceRef> random = new ArrayList<>();
    for (int i = 0; i < num; i++) {
      random.add(MonitoredServiceRef.builder().monitoredServiceIdentifier(randomAlphanumeric(20)).build());
    }
    return random;
  }
}
