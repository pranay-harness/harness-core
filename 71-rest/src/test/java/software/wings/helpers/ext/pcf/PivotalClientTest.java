package software.wings.helpers.ext.pcf;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.applications.Applications;
import org.cloudfoundry.operations.organizations.OrganizationDetail;
import org.cloudfoundry.operations.organizations.OrganizationQuota;
import org.cloudfoundry.operations.organizations.OrganizationSummary;
import org.cloudfoundry.operations.organizations.Organizations;
import org.cloudfoundry.operations.routes.Route;
import org.cloudfoundry.operations.routes.Routes;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.Spy;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.wings.WingsBaseTest;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class PivotalClientTest extends WingsBaseTest {
  PcfClientImpl pcfClient = new PcfClientImpl();
  @Mock CloudFoundryOperationsWrapper wrapper;
  @Mock CloudFoundryOperations operations;
  @Mock Organizations organizations;
  @Mock Applications applications;
  @Mock Routes routes;
  @Spy PcfClientImpl client;

  @Before
  public void setupMocks() throws Exception {
    when(wrapper.getCloudFoundryOperations()).thenReturn(operations);
    doNothing().when(wrapper).close();
    when(operations.organizations()).thenReturn(organizations);
    when(operations.applications()).thenReturn(applications);
    when(operations.routes()).thenReturn(routes);
    doReturn(wrapper).when(client).getCloudFoundryOperationsWrapper(any());
  }
  @Test
  @Category(UnitTests.class)
  public void testHandlePasswordForSpecialCharacters() throws Exception {
    String password = "Ab1~!@#$%^&*()_'\"c";
    ProcessExecutor processExecutor =
        new ProcessExecutor()
            .timeout(1, TimeUnit.MINUTES)
            .command(
                "/bin/sh", "-c", new StringBuilder(128).append("echo ").append(password).append(" | cat ").toString());
    ProcessResult processResult = processExecutor.execute();
    assertThat(processResult.getExitValue()).isNotEqualTo(0);

    password = pcfClient.handlePwdForSpecialCharsForShell("Ab1~!@#$%^&*()_'\"c");
    processExecutor = new ProcessExecutor()
                          .timeout(1, TimeUnit.MINUTES)
                          .command("/bin/sh", "-c",
                              new StringBuilder(128).append("echo ").append(password).append(" | cat ").toString());
    processResult = processExecutor.execute();
    assertThat(processResult.getExitValue()).isEqualTo(0);
  }

  @Test
  @Category(UnitTests.class)
  public void testGetOrganizations() throws Exception {
    OrganizationSummary summary1 = OrganizationSummary.builder().id("1").name("org1").build();
    OrganizationSummary summary2 = OrganizationSummary.builder().id("2").name("org2").build();

    Flux<OrganizationSummary> result = Flux.create(sink -> {
      sink.next(summary1);
      sink.next(summary2);
      sink.complete();
    });
    when(organizations.list()).thenReturn(result);

    List<OrganizationSummary> organizationSummaries = client.getOrganizations(getPcfRequestConfig());
    assertThat(organizationSummaries).isNotNull();
    assertThat(organizationSummaries).containsExactly(summary1, summary2);
  }

  @Test
  @Category(UnitTests.class)
  public void testGetSpacesForOrganization() throws Exception {
    String[] spaces = new String[] {"space1", "space2", "space3"};
    when(organizations.get(any()))
        .thenReturn(Mono.just(OrganizationDetail.builder()
                                  .spaces(spaces)
                                  .id("id")
                                  .name("org")
                                  .quota(OrganizationQuota.builder()
                                             .id("1")
                                             .instanceMemoryLimit(1)
                                             .name("name")
                                             .organizationId("id")
                                             .paidServicePlans(true)
                                             .totalMemoryLimit(1)
                                             .totalRoutes(1)
                                             .totalServiceInstances(1)
                                             .build())
                                  .build()));

    List<String> spaceResult = client.getSpacesForOrganization(getPcfRequestConfig());

    assertThat(spaceResult).isNotNull();
    assertThat(spaceResult).containsExactly(spaces);
  }

  @Test
  @Category(UnitTests.class)
  public void testGetApplications() throws Exception {
    ApplicationSummary applicationSummary1 = ApplicationSummary.builder()
                                                 .id("id1")
                                                 .name("app1")
                                                 .diskQuota(1)
                                                 .instances(1)
                                                 .memoryLimit(1)
                                                 .requestedState("RUNNING")
                                                 .runningInstances(0)
                                                 .build();
    ApplicationSummary applicationSummary2 = ApplicationSummary.builder()
                                                 .id("id2")
                                                 .name("app2")
                                                 .diskQuota(1)
                                                 .instances(1)
                                                 .memoryLimit(1)
                                                 .requestedState("RUNNING")
                                                 .runningInstances(0)
                                                 .build();

    Flux<ApplicationSummary> result = Flux.create(sink -> {
      sink.next(applicationSummary1);
      sink.next(applicationSummary2);
      sink.complete();
    });
    when(applications.list()).thenReturn(result);

    List<ApplicationSummary> applicationSummaries = client.getApplications(getPcfRequestConfig());

    assertThat(applicationSummaries).isNotNull();
    assertThat(applicationSummaries).containsExactly(applicationSummary1, applicationSummary2);
  }

  @Test
  @Category(UnitTests.class)
  public void testGetApplicationByName() throws Exception {
    ApplicationDetail applicationDetail = ApplicationDetail.builder()
                                              .id("id")
                                              .name("app")
                                              .diskQuota(1)
                                              .stack("stack")
                                              .instances(1)
                                              .memoryLimit(1)
                                              .requestedState("RUNNING")
                                              .runningInstances(1)
                                              .build();
    when(applications.get(any())).thenReturn(Mono.just(applicationDetail));

    ApplicationDetail applicationDetail1 = client.getApplicationByName(getPcfRequestConfig());
    assertThat(applicationDetail1).isNotNull();
    assertThat(applicationDetail).isEqualTo(applicationDetail1);
  }

  @Test
  @Category(UnitTests.class)
  public void testGetAllRoutesForSpace() throws Exception {
    Route route1 = Route.builder().application("app").host("stage").domain("harness.io").id("1").space("space").build();
    Route route2 =
        Route.builder().application("app").host("qa").domain("harness.io").path("api").id("2").space("space").build();

    Flux<Route> result = Flux.create(sink -> {
      sink.next(route1);
      sink.next(route2);
      sink.complete();
    });

    when(routes.list(any())).thenReturn(result);

    List<String> routeMaps = client.getRoutesForSpace(getPcfRequestConfig());
    assertThat(routeMaps).isNotNull();
    assertThat(routeMaps).containsExactly("stage.harness.io", "qa.harness.io/api");
  }

  @Test
  @Category(UnitTests.class)
  public void testGetRoutesMapsByName() throws Exception {
    Route route1 = Route.builder().application("app").host("stage").domain("harness.io").id("1").space("space").build();
    Route route2 =
        Route.builder().application("app").host("qa").domain("harness.io").path("api").id("2").space("space").build();

    Flux<Route> result = Flux.create(sink -> {
      sink.next(route1);
      sink.next(route2);
      sink.complete();
    });

    when(routes.list(any())).thenReturn(result);

    List<Route> routeMaps =
        client.getRouteMapsByNames(Arrays.asList("stage.harness.io", "qa.harness.io/api"), getPcfRequestConfig());
    assertThat(routeMaps).isNotNull();
    List<String> routes = routeMaps.stream().map(route -> client.getPathFromRouteMap(route)).collect(toList());
    assertThat(routes).containsExactly("stage.harness.io", "qa.harness.io/api");
  }

  @Test
  @Category(UnitTests.class)
  public void testGetRouteMap() throws Exception {
    Route route_1 =
        Route.builder().application("app").host("stage").domain("harness.io").id("1").space("space").build();
    Route route_2 =
        Route.builder().application("app").host("qa").domain("harness.io").path("api").id("2").space("space").build();

    Flux<Route> routeResult = Flux.create(sink -> {
      sink.next(route_1);
      sink.next(route_2);
      sink.complete();
    });

    when(routes.list(any())).thenReturn(routeResult);
    Optional<Route> routeOptional = client.getRouteMap(getPcfRequestConfig(), "qa.harness.io/api");
    assertThat(routeOptional.isPresent()).isTrue();
    assertThat(client.getPathFromRouteMap(routeOptional.get())).isEqualTo("qa.harness.io/api");
  }

  @Test
  @Category(UnitTests.class)
  public void testCloudFoundryOperationsClose() throws Exception {
    PcfClientImpl pcfClient = mock(PcfClientImpl.class);
    CloudFoundryOperationsWrapper wrapperObject =
        CloudFoundryOperationsWrapper.builder().cloudFoundryOperations(null).connectionContext(null).build();
    wrapperObject = spy(wrapperObject);

    doReturn(wrapperObject).when(pcfClient).getCloudFoundryOperationsWrapper(any());
    try (CloudFoundryOperationsWrapper wrapper = client.getCloudFoundryOperationsWrapper(getPcfRequestConfig())) {
      wrapperObject = wrapper;
    }

    verify(wrapperObject, times(1)).close();
  }

  private PcfRequestConfig getPcfRequestConfig() {
    return PcfRequestConfig.builder().timeOutIntervalInMins(1).orgName("org").applicationName("app").build();
  }
}
