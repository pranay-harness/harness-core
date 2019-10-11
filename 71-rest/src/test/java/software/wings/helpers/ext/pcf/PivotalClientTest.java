package software.wings.helpers.ext.pcf;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.wings.WingsBaseTest;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.helpers.ext.pcf.request.PcfCreateApplicationRequestData;

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
  @Spy PcfClientImpl mockedClient;

  @Before
  public void setupMocks() throws Exception {
    when(wrapper.getCloudFoundryOperations()).thenReturn(operations);
    doNothing().when(wrapper).close();
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

    when(operations.organizations()).thenReturn(organizations);
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
    when(operations.organizations()).thenReturn(organizations);

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

  @Test
  @Category(UnitTests.class)
  public void testGetCloudFoundryOperationsWrapper() throws Exception {
    PcfRequestConfig pcfRequestConfig = getPcfRequestConfig();
    pcfRequestConfig.setUserName("username");
    pcfRequestConfig.setPassword("password");
    pcfRequestConfig.setEndpointUrl("api.run.pivotal.io");

    CloudFoundryOperationsWrapper cloudFoundryOperationsWrapper =
        mockedClient.getCloudFoundryOperationsWrapper(pcfRequestConfig);

    assertThat(cloudFoundryOperationsWrapper).isNotNull();
  }

  @Test
  @Category(UnitTests.class)
  public void testGetCFOperationsWrapperForConnectionContextException() {
    PcfRequestConfig pcfRequestConfig = getPcfRequestConfig();

    try {
      mockedClient.getCloudFoundryOperationsWrapper(pcfRequestConfig);
      fail("Should not reach here.");
    } catch (PivotalClientApiException e) {
      assertThat(e.getMessage())
          .isEqualTo("Exception while creating CloudFoundryOperations: NullPointerException: apiHost");
    }
  }

  @Test
  @Category(UnitTests.class)
  public void testGetCFOperationsWrapperForTokenProviderException() {
    PcfRequestConfig pcfRequestConfig = getPcfRequestConfig();
    pcfRequestConfig.setUserName("username");
    pcfRequestConfig.setEndpointUrl("api.run.pivotal.io");

    try {
      mockedClient.getCloudFoundryOperationsWrapper(pcfRequestConfig);
      fail("Should not reach here.");
    } catch (PivotalClientApiException e) {
      assertThat(e.getMessage())
          .isEqualTo("Exception while creating CloudFoundryOperations: NullPointerException: password");
    }
  }

  @Test
  @Category(UnitTests.class)
  public void testGetOrganizationsException() {
    PcfRequestConfig pcfRequestConfig = getPcfRequestConfig();
    pcfRequestConfig.setUserName("username");
    pcfRequestConfig.setPassword("password");
    pcfRequestConfig.setEndpointUrl("api.run.pivotal.io");

    try {
      mockedClient.getOrganizations(pcfRequestConfig);
      fail("Should not reach here.");
    } catch (Exception e) {
      assertThat(e.getMessage())
          .isEqualTo("Exception occurred while fetching Organizations, Error: unauthorized: Bad credentials");
    }
  }

  @Test
  @Category(UnitTests.class)
  public void testGetApplicationByNameException() {
    PcfRequestConfig pcfRequestConfig = getPcfRequestConfig();
    pcfRequestConfig.setUserName("username");
    pcfRequestConfig.setPassword("password");
    pcfRequestConfig.setEndpointUrl("api.run.pivotal.io");

    try {
      mockedClient.getApplicationByName(pcfRequestConfig);
      fail("Should not reach here.");
    } catch (Exception e) {
      assertThat(e.getMessage())
          .isEqualTo("Exception occurred while  getting application: app, Error: No space targeted");
    }
  }

  @Test
  @Category(UnitTests.class)
  public void testGetRouteMapException() {
    PcfRequestConfig pcfRequestConfig = getPcfRequestConfig();
    pcfRequestConfig.setUserName("username");
    pcfRequestConfig.setPassword("password");
    pcfRequestConfig.setEndpointUrl("api.run.pivotal.io");

    try {
      mockedClient.getRouteMap(pcfRequestConfig, "qa.harness.io/api");
      fail("Should not reach here.");
    } catch (Exception e) {
      assertThat(e.getMessage())
          .isEqualTo(
              "Exception occurred while getting routeMaps for Application: app, Error: unauthorized: Bad credentials");
    }
  }

  @Test
  @Category(UnitTests.class)
  public void testGetApplicationsException() {
    PcfRequestConfig pcfRequestConfig = getPcfRequestConfig();
    pcfRequestConfig.setUserName("username");
    pcfRequestConfig.setPassword("password");
    pcfRequestConfig.setEndpointUrl("api.run.pivotal.io");

    try {
      mockedClient.getApplications(pcfRequestConfig);
      fail("Should not reach here.");
    } catch (Exception e) {
      assertThat(e.getMessage()).isEqualTo("Exception occurred while fetching Applications , Error: No space targeted");
    }
  }

  @Test
  @Category(UnitTests.class)
  public void testGetSpacesForOrganizationException() {
    PcfRequestConfig pcfRequestConfig = getPcfRequestConfig();
    pcfRequestConfig.setUserName("username");
    pcfRequestConfig.setPassword("password");
    pcfRequestConfig.setEndpointUrl("api.run.pivotal.io");

    try {
      mockedClient.getSpacesForOrganization(pcfRequestConfig);
      fail("Should not reach here.");
    } catch (Exception e) {
      assertThat(e.getMessage())
          .isEqualTo("Exception occurred while fetching Spaces, Error: unauthorized: Bad credentials");
    }
  }

  @Test
  @Category(UnitTests.class)
  public void testDeleteApplicationException() {
    PcfRequestConfig pcfRequestConfig = getPcfRequestConfig();
    pcfRequestConfig.setUserName("username");
    pcfRequestConfig.setPassword("password");
    pcfRequestConfig.setEndpointUrl("api.run.pivotal.io");

    try {
      mockedClient.deleteApplication(pcfRequestConfig);
      fail("Should not reach here.");
    } catch (Exception e) {
      assertThat(e.getMessage())
          .isEqualTo("Exception occurred while deleting application: app, Error: No space targeted");
    }
  }

  @Test
  @Category(UnitTests.class)
  public void testStopApplicationException() {
    PcfRequestConfig pcfRequestConfig = getPcfRequestConfig();
    pcfRequestConfig.setUserName("username");
    pcfRequestConfig.setPassword("password");
    pcfRequestConfig.setEndpointUrl("api.run.pivotal.io");

    try {
      mockedClient.stopApplication(pcfRequestConfig);
      fail("Should not reach here.");
    } catch (Exception e) {
      assertThat(e.getMessage())
          .isEqualTo("Exception occurred while stopping Application: app, Error: No space targeted");
    }
  }

  @Test
  @Category(UnitTests.class)
  public void testGetTasksException() {
    PcfRequestConfig pcfRequestConfig = getPcfRequestConfig();
    pcfRequestConfig.setUserName("username");
    pcfRequestConfig.setPassword("password");
    pcfRequestConfig.setEndpointUrl("api.run.pivotal.io");

    try {
      mockedClient.getTasks(pcfRequestConfig);
      fail("Should not reach here.");
    } catch (Exception e) {
      assertThat(e.getMessage())
          .isEqualTo("Exception occurred while getting Tasks for Application: app, Error: No space targeted");
    }
  }

  @Test
  @Category(UnitTests.class)
  public void testScaleApplicationsException() {
    PcfRequestConfig pcfRequestConfig = getPcfRequestConfig();
    pcfRequestConfig.setUserName("username");
    pcfRequestConfig.setPassword("password");
    pcfRequestConfig.setEndpointUrl("api.run.pivotal.io");

    try {
      mockedClient.scaleApplications(pcfRequestConfig);
      fail("Should not reach here.");
    } catch (Exception e) {
      assertThat(e.getMessage())
          .isEqualTo("Exception occurred Scaling Applications: app, to count: 0, Error: No space targeted");
    }
  }

  @Test
  @Category(UnitTests.class)
  public void testStartApplicationException() {
    PcfRequestConfig pcfRequestConfig = getPcfRequestConfig();
    pcfRequestConfig.setUserName("username");
    pcfRequestConfig.setPassword("password");
    pcfRequestConfig.setEndpointUrl("api.run.pivotal.io");

    try {
      mockedClient.startApplication(pcfRequestConfig);
      fail("Should not reach here.");
    } catch (Exception e) {
      assertThat(e.getMessage())
          .isEqualTo("Exception occurred while starting application: app, Error: No space targeted");
    }
  }

  @Test
  @Category(UnitTests.class)
  public void testUnmapRouteMapForAppException() {
    PcfRequestConfig pcfRequestConfig = getPcfRequestConfig();
    pcfRequestConfig.setUserName("username");
    pcfRequestConfig.setPassword("password");
    pcfRequestConfig.setEndpointUrl("api.run.pivotal.io");

    Route route = Route.builder().application("app").host("stage").domain("harness.io").id("1").space("space").build();

    try {
      mockedClient.unmapRouteMapForApp(pcfRequestConfig, route);
      fail("Should not reach here.");
    } catch (Exception e) {
      assertThat(e.getMessage())
          .isEqualTo("Exception occurred while unmapping routeMap for Application: app, Error: No space targeted");
    }
  }

  @Test
  @Category(UnitTests.class)
  public void testMapRouteMapForAppException() {
    PcfRequestConfig pcfRequestConfig = getPcfRequestConfig();
    pcfRequestConfig.setUserName("username");
    pcfRequestConfig.setPassword("password");
    pcfRequestConfig.setEndpointUrl("api.run.pivotal.io");

    Route route = Route.builder().application("app").host("stage").domain("harness.io").id("1").space("space").build();

    try {
      mockedClient.mapRouteMapForApp(pcfRequestConfig, route);
      fail("Should not reach here.");
    } catch (Exception e) {
      assertThat(e.getMessage())
          .isEqualTo(
              "Exception occurred while mapping routeMap: Route{applications=[app], domain=harness.io, host=stage, id=1, path=null, port=null, service=null, space=space, type=null}, AppName: app, Error: No space targeted");
    }
  }

  @Test
  @Category(UnitTests.class)
  public void testPushApplicationUsingManifest() throws Exception {
    PcfClientImpl client = spy(PcfClientImpl.class);
    doNothing().when(client).performCfPushUsingCli(any(), any());
    doNothing().when(client).pushUsingPcfSdk(any(), any());

    ExecutionLogCallback logCallback = mock(ExecutionLogCallback.class);
    doNothing().when(logCallback).saveExecutionLog(anyString());

    PcfRequestConfig pcfRequestConfig = PcfRequestConfig.builder().useCLIForAppCreate(true).build();

    PcfCreateApplicationRequestData requestData =
        PcfCreateApplicationRequestData.builder().manifestFilePath("path").pcfRequestConfig(pcfRequestConfig).build();
    // actual call
    client.pushApplicationUsingManifest(requestData, logCallback);

    ArgumentCaptor<PcfCreateApplicationRequestData> captor =
        ArgumentCaptor.forClass(PcfCreateApplicationRequestData.class);
    verify(client).performCfPushUsingCli(captor.capture(), any());
    PcfCreateApplicationRequestData captorValue = captor.getValue();
    assertThat(captorValue).isEqualTo(requestData);

    pcfRequestConfig.setUseCLIForAppCreate(false);
    // actual call
    client.pushApplicationUsingManifest(requestData, logCallback);
    ArgumentCaptor<PcfRequestConfig> pcfRequestCaptor = ArgumentCaptor.forClass(PcfRequestConfig.class);
    verify(client).pushUsingPcfSdk(pcfRequestCaptor.capture(), any());
    PcfRequestConfig captorValueConfig = pcfRequestCaptor.getValue();
    assertThat(captorValueConfig).isEqualTo(pcfRequestConfig);
  }
}
