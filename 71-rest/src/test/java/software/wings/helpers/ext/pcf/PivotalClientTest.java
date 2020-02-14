package software.wings.helpers.ext.pcf;

import static io.harness.pcf.model.PcfConstants.APP_TOKEN;
import static io.harness.pcf.model.PcfConstants.CF_COMMAND_FOR_APP_LOG_TAILING;
import static io.harness.pcf.model.PcfConstants.CF_COMMAND_FOR_CHECKING_AUTOSCALAR;
import static io.harness.pcf.model.PcfConstants.CF_HOME;
import static io.harness.pcf.model.PcfConstants.CF_PLUGIN_HOME;
import static io.harness.pcf.model.PcfRouteType.PCF_ROUTE_TYPE_HTTP;
import static io.harness.pcf.model.PcfRouteType.PCF_ROUTE_TYPE_TCP;
import static io.harness.rule.OwnerRule.ADWAIT;
import static io.harness.rule.OwnerRule.ANSHUL;
import static io.harness.rule.OwnerRule.ROHIT_KUMAR;
import static io.harness.rule.OwnerRule.SATYAM;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.helpers.ext.pcf.PcfClientImpl.BIN_SH;

import io.harness.category.element.UnitTests;
import io.harness.filesystem.FileIo;
import io.harness.pcf.model.PcfRouteInfo;
import io.harness.rule.Owner;
import org.cloudfoundry.doppler.LogMessage;
import org.cloudfoundry.doppler.MessageType;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.applications.Applications;
import org.cloudfoundry.operations.applications.LogsRequest;
import org.cloudfoundry.operations.domains.Domain;
import org.cloudfoundry.operations.domains.Status;
import org.cloudfoundry.operations.organizations.OrganizationDetail;
import org.cloudfoundry.operations.organizations.OrganizationQuota;
import org.cloudfoundry.operations.organizations.OrganizationSummary;
import org.cloudfoundry.operations.organizations.Organizations;
import org.cloudfoundry.operations.routes.Route;
import org.cloudfoundry.operations.routes.Routes;
import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.Test.None;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.stubbing.Answer;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.StartedProcess;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.wings.WingsBaseTest;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.helpers.ext.pcf.request.PcfAppAutoscalarRequestData;
import software.wings.helpers.ext.pcf.request.PcfCreateApplicationRequestData;
import software.wings.helpers.ext.pcf.request.PcfRunPluginScriptRequestData;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class PivotalClientTest extends WingsBaseTest {
  @Spy PcfClientImpl pcfClient = new PcfClientImpl();
  @Mock CloudFoundryOperationsWrapper wrapper;
  @Mock CloudFoundryOperations operations;
  @Mock Organizations organizations;
  @Mock Applications applications;
  @Mock Routes routes;
  @Spy PcfClientImpl client;
  @Spy PcfClientImpl mockedClient;
  public static String APP_NAME = "APP_NAME";
  public static String PATH = "path";

  @Before
  public void setupMocks() throws Exception {
    when(wrapper.getCloudFoundryOperations()).thenReturn(operations);
    doNothing().when(wrapper).close();
    when(operations.applications()).thenReturn(applications);
    when(operations.routes()).thenReturn(routes);
    doReturn(wrapper).when(client).getCloudFoundryOperationsWrapper(any());
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testHandlePasswordForSpecialCharacters() throws Exception {
    // Single quotes cannot be part of the password due to limitations on escaping them.
    String password = "Ab1~!@#$%^&*()\"_c";
    ProcessExecutor processExecutor =
        new ProcessExecutor()
            .timeout(1, TimeUnit.MINUTES)
            .command(
                "/bin/sh", "-c", new StringBuilder(128).append("echo ").append(password).append(" | cat ").toString());
    ProcessResult processResult = processExecutor.execute();
    assertThat(processResult.getExitValue()).isNotEqualTo(0);

    password = pcfClient.handlePwdForSpecialCharsForShell("Ab1~!@#$%^&*()\"_c");
    processExecutor = new ProcessExecutor()
                          .timeout(1, TimeUnit.MINUTES)
                          .command("/bin/sh", "-c",
                              new StringBuilder(128).append("echo ").append(password).append(" | cat ").toString());
    processResult = processExecutor.execute();
    assertThat(processResult.getExitValue()).isEqualTo(0);
  }

  @Test
  @Owner(developers = ADWAIT)
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
  @Owner(developers = ADWAIT)
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
  @Owner(developers = ADWAIT)
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
  @Owner(developers = ADWAIT)
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
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testGetAllRoutesForSpace() throws Exception {
    Route route1 = Route.builder().application("app").host("stage").domain("harness.io").id("1").space("space").build();
    Route route2 =
        Route.builder().application("app").host("qa").domain("harness.io").path("/api").id("2").space("space").build();

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
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testGetRoutesMapsByName() throws Exception {
    Route route1 = Route.builder().application("app").host("stage").domain("harness.io").id("1").space("space").build();
    Route route2 =
        Route.builder().application("app").host("qa").domain("harness.io").path("/api").id("2").space("space").build();

    Flux<Route> result = Flux.create(sink -> {
      sink.next(route1);
      sink.next(route2);
      sink.complete();
    });

    when(routes.list(any())).thenReturn(result);

    List<Route> routeMaps =
        client.getRouteMapsByNames(asList("stage.harness.io", "qa.harness.io/api"), getPcfRequestConfig());
    assertThat(routeMaps).isNotNull();
    List<String> routes = routeMaps.stream().map(route -> client.getPathFromRouteMap(route)).collect(toList());
    assertThat(routes).containsExactly("stage.harness.io", "qa.harness.io/api");
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testGetRouteMap() throws Exception {
    Route route_1 =
        Route.builder().application("app").host("stage").domain("harness.io").id("1").space("space").build();
    Route route_2 =
        Route.builder().application("app").host("qa").domain("harness.io").path("/api").id("2").space("space").build();

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
  @Owner(developers = ADWAIT)
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
  @Owner(developers = ANSHUL)
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
  @Owner(developers = ANSHUL)
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
  @Owner(developers = ANSHUL)
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
  @Owner(developers = ANSHUL)
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
          .isEqualTo("Exception occurred while fetching Organizations, Error:unauthorized: Bad credentials");
    }
  }

  @Test
  @Owner(developers = ANSHUL)
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
  @Owner(developers = ANSHUL)
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
  @Owner(developers = ANSHUL)
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
  @Owner(developers = ANSHUL)
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
  @Owner(developers = ANSHUL)
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
  @Owner(developers = ANSHUL)
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
  @Owner(developers = ANSHUL)
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
  @Owner(developers = ANSHUL)
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
          .isEqualTo("Exception occurred Scaling Applications: app, to count: 0, Error:No space targeted");
    }
  }

  @Test
  @Owner(developers = ADWAIT)
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
  @Owner(developers = ADWAIT)
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
  @Owner(developers = ADWAIT)
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
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testPushApplicationUsingManifest() throws Exception {
    PcfClientImpl client = spy(PcfClientImpl.class);
    doNothing().when(client).performCfPushUsingCli(any(), any());
    doNothing().when(client).pushUsingPcfSdk(any(), any());

    ExecutionLogCallback logCallback = mock(ExecutionLogCallback.class);
    doNothing().when(logCallback).saveExecutionLog(anyString());

    PcfRequestConfig pcfRequestConfig = PcfRequestConfig.builder().useCFCLI(true).build();

    PcfCreateApplicationRequestData requestData =
        PcfCreateApplicationRequestData.builder().manifestFilePath("path").pcfRequestConfig(pcfRequestConfig).build();
    // actual call
    client.pushApplicationUsingManifest(requestData, logCallback);

    ArgumentCaptor<PcfCreateApplicationRequestData> captor =
        ArgumentCaptor.forClass(PcfCreateApplicationRequestData.class);
    verify(client).performCfPushUsingCli(captor.capture(), any());
    PcfCreateApplicationRequestData captorValue = captor.getValue();
    assertThat(captorValue).isEqualTo(requestData);

    pcfRequestConfig.setUseCFCLI(false);
    // actual call
    client.pushApplicationUsingManifest(requestData, logCallback);
    ArgumentCaptor<PcfRequestConfig> pcfRequestCaptor = ArgumentCaptor.forClass(PcfRequestConfig.class);
    verify(client).pushUsingPcfSdk(pcfRequestCaptor.capture(), any());
    PcfRequestConfig captorValueConfig = pcfRequestCaptor.getValue();
    assertThat(captorValueConfig).isEqualTo(pcfRequestConfig);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testFindRoutesNeedToBeCreated() throws Exception {
    List<String> routes = new ArrayList<>();
    String path1 = "myapp.cfapps.io";
    String path2 = "myapp.cfapps.io/path";
    String path3 = "myapp2.cfapps.io";
    String space = "space";

    routes.add(path1);
    Route route = Route.builder().id("id").host("myapp").domain("cfapps.io").space(space).build();

    List<String> routeToBeCreated = pcfClient.findRoutesNeedToBeCreated(routes, asList(route));
    assertThat(routeToBeCreated).isNotNull();
    assertThat(routeToBeCreated).isEmpty();

    routes.clear();
    routes.add(path2);
    Route route1 = Route.builder().id("id").host("myapp").domain("cfapps.io").path("/path").space(space).build();
    routeToBeCreated = pcfClient.findRoutesNeedToBeCreated(routes, asList(route1));
    assertThat(routeToBeCreated).isNotNull();
    assertThat(routeToBeCreated).isEmpty();

    routes.clear();
    routes.add(path1);
    routes.add(path2);
    routeToBeCreated = pcfClient.findRoutesNeedToBeCreated(routes, asList(route1, route));
    assertThat(routeToBeCreated).isNotNull();
    assertThat(routeToBeCreated).isEmpty();

    routes.add(path3);
    routeToBeCreated = pcfClient.findRoutesNeedToBeCreated(routes, asList(route1, route));
    assertThat(routeToBeCreated).isNotNull();
    assertThat(routeToBeCreated).isNotEmpty();
    assertThat(routeToBeCreated.size()).isEqualTo(1);
    assertThat(routeToBeCreated.get(0)).isEqualTo(path3);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testMapRoutesForApplication() throws Exception {
    PcfClientImpl pcfClient1 = spy(PcfClientImpl.class);
    String space = "space1";
    Route route = Route.builder().id("id").host("myapp").domain("cfapps.io").space(space).build();
    Route route1 = Route.builder().id("id").host("myapp").domain("cfapps.io").path("/path").space(space).build();

    doReturn(asList(route, route1)).when(pcfClient1).getRouteMapsByNames(anyList(), any());
    doNothing().when(pcfClient1).mapRouteMapForApp(any(), any());
    PcfRequestConfig pcfRequestConfig = PcfRequestConfig.builder().spaceName(space).build();

    List<String> routes = new ArrayList<>();
    String path1 = "myapp.cfapps.io";
    String path2 = "myapp.cfapps.io/path";
    String path3 = "myapp2.cfapps.io/P1";
    routes.add(path1);
    routes.add(path2);
    // All mapping routes exists
    pcfClient1.mapRoutesForApplication(pcfRequestConfig, routes);
    verify(pcfClient1, never()).getAllDomainsForSpace(any());

    routes.add(path3);
    doNothing()
        .when(pcfClient1)
        .createRouteMap(any(), anyString(), anyString(), anyString(), anyBoolean(), anyBoolean(), anyInt());
    doNothing().when(pcfClient1).mapRouteMapForApp(any(), any());
    doReturn(asList(Domain.builder().id("id1").name("cfapps.io").status(Status.SHARED).build()))
        .when(pcfClient1)
        .getAllDomainsForSpace(any());

    // 1 mapping route needs to be created
    pcfClient1.mapRoutesForApplication(pcfRequestConfig, routes);
    ArgumentCaptor<String> hostCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> domainCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
    verify(pcfClient1)
        .createRouteMap(any(), hostCaptor.capture(), domainCaptor.capture(), pathCaptor.capture(), anyBoolean(),
            anyBoolean(), anyInt());
    assertThat(hostCaptor.getValue()).isEqualTo("myapp2");
    assertThat(domainCaptor.getValue()).isEqualTo("cfapps.io");
    assertThat(pathCaptor.getValue()).isEqualTo("/P1");
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testPerformConfigureAutoscalar() throws Exception {
    PcfClientImpl pcfClient = spy(PcfClientImpl.class);
    ExecutionLogCallback logCallback = mock(ExecutionLogCallback.class);
    doNothing().when(logCallback).saveExecutionLog(anyString());
    doAnswer((Answer<Boolean>) invocation -> { return true; }).when(pcfClient).doLogin(any(), any(), anyString());

    String AUTOSCALAR_MANIFEST = "autoscalar config";

    File file = new File("./autoscalar" + System.currentTimeMillis() + ".yml");
    file.createNewFile();
    FileIo.writeFile(file.getAbsolutePath(), AUTOSCALAR_MANIFEST.getBytes());

    ProcessExecutor processExecutor = mock(ProcessExecutor.class);
    ProcessResult processResult = mock(ProcessResult.class);
    doReturn(processResult).when(processExecutor).execute();
    doReturn(0).doReturn(1).when(processResult).getExitValue();

    doReturn(processExecutor).when(pcfClient).createProccessExecutorForPcfTask(anyLong(), anyString(), anyMap(), any());
    pcfClient.performConfigureAutoscalar(PcfAppAutoscalarRequestData.builder()
                                             .autoscalarFilePath(file.getAbsolutePath())
                                             .timeoutInMins(1)
                                             .pcfRequestConfig(PcfRequestConfig.builder().build())
                                             .build(),
        logCallback);

    try {
      pcfClient.performConfigureAutoscalar(
          PcfAppAutoscalarRequestData.builder().autoscalarFilePath(file.getAbsolutePath()).timeoutInMins(1).build(),
          logCallback);
    } catch (Exception e) {
      assertThat(e instanceof PivotalClientApiException).isTrue();
    }

    doThrow(Exception.class).when(processExecutor).execute();
    try {
      pcfClient.performConfigureAutoscalar(
          PcfAppAutoscalarRequestData.builder().autoscalarFilePath(file.getAbsolutePath()).timeoutInMins(1).build(),
          logCallback);
    } catch (Exception e) {
      assertThat(e instanceof PivotalClientApiException).isTrue();
    }

    FileIo.deleteFileIfExists(file.getAbsolutePath());
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testCheckIfAppHasAutoscalarAttached() throws Exception {
    PcfClientImpl pcfClient = spy(PcfClientImpl.class);
    ExecutionLogCallback logCallback = mock(ExecutionLogCallback.class);
    doNothing().when(logCallback).saveExecutionLog(anyString());
    doAnswer((Answer<Boolean>) invocation -> { return true; }).when(pcfClient).doLogin(any(), any(), anyString());

    ProcessExecutor processExecutor = mock(ProcessExecutor.class);
    ProcessResult processResult = mock(ProcessResult.class);

    doReturn(processResult).when(processExecutor).execute();
    doReturn("asd").doReturn(null).doReturn(EMPTY).when(processResult).outputUTF8();
    doReturn(processExecutor).when(pcfClient).createProccessExecutorForPcfTask(anyLong(), anyString(), anyMap(), any());

    PcfAppAutoscalarRequestData autoscalarRequestData = PcfAppAutoscalarRequestData.builder()
                                                            .applicationName(APP_NAME)
                                                            .applicationGuid(APP_NAME)
                                                            .timeoutInMins(1)
                                                            .pcfRequestConfig(PcfRequestConfig.builder().build())
                                                            .build();
    assertThat(pcfClient.checkIfAppHasAutoscalarAttached(autoscalarRequestData, logCallback)).isTrue();

    assertThat(pcfClient.checkIfAppHasAutoscalarAttached(autoscalarRequestData, logCallback)).isFalse();
    assertThat(pcfClient.checkIfAppHasAutoscalarAttached(autoscalarRequestData, logCallback)).isFalse();
    doThrow(Exception.class).when(processExecutor).execute();
    try {
      pcfClient.checkIfAppHasAutoscalarAttached(PcfAppAutoscalarRequestData.builder().build(), logCallback);
    } catch (Exception e) {
      assertThat(e instanceof PivotalClientApiException).isTrue();
    }
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testCheckIfAppAutoscalarInstalled() throws Exception {
    PcfClientImpl pcfClient = spy(PcfClientImpl.class);
    ExecutionLogCallback logCallback = mock(ExecutionLogCallback.class);
    doNothing().when(logCallback).saveExecutionLog(anyString());

    ProcessExecutor processExecutor = mock(ProcessExecutor.class);
    ProcessResult processResult = mock(ProcessResult.class);

    doReturn(processResult).when(processExecutor).execute();
    doReturn("asd").doReturn(null).doReturn(EMPTY).when(processResult).outputUTF8();

    doReturn(processExecutor).when(pcfClient).createExecutorForAutoscalarPluginCheck(anyMap());
    assertThat(pcfClient.checkIfAppAutoscalarInstalled()).isTrue();

    assertThat(pcfClient.checkIfAppAutoscalarInstalled()).isFalse();
    assertThat(pcfClient.checkIfAppAutoscalarInstalled()).isFalse();

    doThrow(Exception.class).when(processExecutor).execute();
    try {
      pcfClient.checkIfAppAutoscalarInstalled();
    } catch (Exception e) {
      assertThat(e instanceof PivotalClientApiException).isTrue();
    }
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testChangeAutoscalarState() throws Exception {
    PcfClientImpl pcfClient = spy(PcfClientImpl.class);
    ExecutionLogCallback logCallback = mock(ExecutionLogCallback.class);
    doNothing().when(logCallback).saveExecutionLog(anyString());
    doAnswer((Answer<Boolean>) invocation -> { return true; }).when(pcfClient).doLogin(any(), any(), anyString());

    ProcessExecutor processExecutor = mock(ProcessExecutor.class);
    ProcessResult processResult = mock(ProcessResult.class);
    doReturn(processResult).when(processExecutor).execute();
    doReturn(0).doReturn(1).when(processResult).getExitValue();

    PcfAppAutoscalarRequestData pcfAppAutoscalarRequestData = PcfAppAutoscalarRequestData.builder()
                                                                  .applicationName(APP_NAME)
                                                                  .timeoutInMins(1)
                                                                  .pcfRequestConfig(PcfRequestConfig.builder().build())
                                                                  .build();
    doReturn(processExecutor).when(pcfClient).createProccessExecutorForPcfTask(anyLong(), anyString(), anyMap(), any());
    doReturn("cf").when(pcfClient).generateChangeAutoscalarStateCommand(any(), anyBoolean());
    pcfClient.changeAutoscalarState(pcfAppAutoscalarRequestData, logCallback, true);

    try {
      pcfClient.changeAutoscalarState(pcfAppAutoscalarRequestData, logCallback, false);
    } catch (Exception e) {
      assertThat(e instanceof PivotalClientApiException).isTrue();
    }

    doThrow(Exception.class).when(processExecutor).execute();
    try {
      pcfClient.changeAutoscalarState(pcfAppAutoscalarRequestData, logCallback, true);
    } catch (Exception e) {
      assertThat(e instanceof PivotalClientApiException).isTrue();
    }
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testGetAppAutoscalarEnvMapForCustomPlugin() throws Exception {
    PcfClientImpl pcfClient = spy(PcfClientImpl.class);
    Map<String, String> appAutoscalarEnvMapForCustomPlugin = pcfClient.getAppAutoscalarEnvMapForCustomPlugin(
        PcfAppAutoscalarRequestData.builder().configPathVar(PATH).build());

    assertThat(appAutoscalarEnvMapForCustomPlugin.size()).isEqualTo(2);
    assertThat(appAutoscalarEnvMapForCustomPlugin.get(CF_HOME)).isEqualTo(PATH);
    assertThat(appAutoscalarEnvMapForCustomPlugin.containsKey(CF_PLUGIN_HOME)).isTrue();
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testCreateProccessExecutorForPcfTask() throws Exception {
    Map<String, String> appAutoscalarEnvMapForCustomPlugin = pcfClient.getAppAutoscalarEnvMapForCustomPlugin(
        PcfAppAutoscalarRequestData.builder().configPathVar(PATH).build());
    ExecutionLogCallback logCallback = mock(ExecutionLogCallback.class);
    doNothing().when(logCallback).saveExecutionLog(anyString());

    PcfClientImpl pcfClient = spy(PcfClientImpl.class);
    ProcessExecutor processExecutor = pcfClient.createProccessExecutorForPcfTask(
        1, CF_COMMAND_FOR_CHECKING_AUTOSCALAR, appAutoscalarEnvMapForCustomPlugin, logCallback);

    assertThat(processExecutor.getCommand()).containsExactly("/bin/sh", "-c", "cf plugins | grep autoscaling-apps");
    assertThat(processExecutor.getEnvironment()).isEqualTo(appAutoscalarEnvMapForCustomPlugin);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testCreateExecutorForAutoscalarPluginCheck() throws Exception {
    Map<String, String> appAutoscalarEnvMapForCustomPlugin = pcfClient.getAppAutoscalarEnvMapForCustomPlugin(
        PcfAppAutoscalarRequestData.builder().configPathVar(PATH).build());
    ExecutionLogCallback logCallback = mock(ExecutionLogCallback.class);
    doNothing().when(logCallback).saveExecutionLog(anyString());

    PcfClientImpl pcfClient = spy(PcfClientImpl.class);
    ProcessExecutor processExecutor =
        pcfClient.createExecutorForAutoscalarPluginCheck(appAutoscalarEnvMapForCustomPlugin);

    assertThat(processExecutor.getCommand()).containsExactly("/bin/sh", "-c", CF_COMMAND_FOR_CHECKING_AUTOSCALAR);
    assertThat(processExecutor.getEnvironment()).isEqualTo(appAutoscalarEnvMapForCustomPlugin);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testGenerateChangeAutoscalarStateCommand() throws Exception {
    PcfClientImpl pcfClient = spy(PcfClientImpl.class);
    PcfAppAutoscalarRequestData autoscalarRequestData =
        PcfAppAutoscalarRequestData.builder().applicationName(APP_NAME).build();
    String command = pcfClient.generateChangeAutoscalarStateCommand(autoscalarRequestData, true);
    assertThat(command).isEqualTo("cf enable-autoscaling " + APP_NAME);
    command = pcfClient.generateChangeAutoscalarStateCommand(autoscalarRequestData, false);
    assertThat(command).isEqualTo("cf disable-autoscaling " + APP_NAME);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testLogInForAppAutoscalarCliCommand() throws Exception {
    PcfClientImpl pcfClient = spy(PcfClientImpl.class);
    ExecutionLogCallback logCallback = mock(ExecutionLogCallback.class);
    doNothing().when(logCallback).saveExecutionLog(anyString());

    PcfAppAutoscalarRequestData autoscalarRequestData =
        PcfAppAutoscalarRequestData.builder()
            .pcfRequestConfig(PcfRequestConfig.builder().loggedin(true).build())
            .build();
    pcfClient.logInForAppAutoscalarCliCommand(autoscalarRequestData, logCallback);
    verify(pcfClient, never()).doLogin(any(), any(), anyString());

    doReturn(true).when(pcfClient).doLogin(any(), any(), anyString());
    autoscalarRequestData.getPcfRequestConfig().setLoggedin(false);
    pcfClient.logInForAppAutoscalarCliCommand(autoscalarRequestData, logCallback);
    verify(pcfClient, times(1)).doLogin(any(), any(), anyString());
  }

  @Test(expected = None.class)
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void test_runPcfPluginScript()
      throws PivotalClientApiException, InterruptedException, TimeoutException, IOException {
    final PcfRunPluginScriptRequestData requestData =
        PcfRunPluginScriptRequestData.builder()
            .pcfRequestConfig(PcfRequestConfig.builder().timeOutIntervalInMins(5).build())
            .workingDirectory("/tmp/abc")
            .build();
    ExecutionLogCallback logCallback = mock(ExecutionLogCallback.class);
    doNothing().when(logCallback).saveExecutionLog(anyString());
    doReturn(true).when(pcfClient).doLogin(any(PcfRequestConfig.class), any(ExecutionLogCallback.class), anyString());
    doReturn(new ProcessResult(0, null)).when(pcfClient).runProcessExecutor(any(ProcessExecutor.class));
    pcfClient.runPcfPluginScript(requestData, logCallback);
  }

  @Test(expected = None.class)
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void test_getConnectionContext() throws Exception {
    PcfRequestConfig pcfRequestConfig =
        PcfRequestConfig.builder().endpointUrl("test").timeOutIntervalInMins(10).build();

    ConnectionContext connectionContext = pcfClient.getConnectionContext(pcfRequestConfig);
    assertThat(connectionContext instanceof DefaultConnectionContext).isTrue();
    Optional<Duration> connectTimeout = ((DefaultConnectionContext) connectionContext).getConnectTimeout();
    assertThat(connectTimeout.isPresent()).isTrue();
    assertThat(connectTimeout.get().getSeconds()).isEqualTo(600);

    pcfRequestConfig.setTimeOutIntervalInMins(0);
    connectionContext = pcfClient.getConnectionContext(pcfRequestConfig);
    assertThat(connectionContext instanceof DefaultConnectionContext).isTrue();
    connectTimeout = ((DefaultConnectionContext) connectionContext).getConnectTimeout();
    assertThat(connectTimeout.isPresent()).isTrue();
    assertThat(connectTimeout.get().getSeconds()).isEqualTo(300);
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void test_getRecentLogs() throws PivotalClientApiException {
    final PcfRequestConfig pcfRequestConfig = PcfRequestConfig.builder()
                                                  .applicationName("app_name")
                                                  .orgName("org_name")
                                                  .spaceName("space_name")
                                                  .timeOutIntervalInMins(5)
                                                  .build();
    final LogMessage logMessage1 =
        LogMessage.builder().message("msg1").messageType(MessageType.OUT).timestamp(1574924788770064207L).build();
    final LogMessage logMessage2 =
        LogMessage.builder().message("msg2").messageType(MessageType.OUT).timestamp(1574924788770064307L).build();
    Flux<LogMessage> result = Flux.create(sink -> {
      sink.next(logMessage1);
      sink.next(logMessage2);
      sink.complete();
    });
    when(applications.logs(any(LogsRequest.class))).thenReturn(result);
    final List<LogMessage> recentLogs = client.getRecentLogs(pcfRequestConfig, 0);
    assertThat(recentLogs).containsExactly(logMessage1, logMessage2);
    assertThat(client.getRecentLogs(pcfRequestConfig, 1574924788770064207L)).containsExactly(logMessage2);
  }

  @Test(expected = PivotalClientApiException.class)
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void test_getRecentLogs_fail() throws PivotalClientApiException {
    final PcfRequestConfig pcfRequestConfig = PcfRequestConfig.builder()
                                                  .applicationName("app_name")
                                                  .timeOutIntervalInMins(5)
                                                  .spaceName("space_name")
                                                  .orgName("org_name")
                                                  .build();
    final LogMessage logMessage1 =
        LogMessage.builder().message("msg1").messageType(MessageType.OUT).timestamp(1574924788770064207L).build();
    Flux<LogMessage> result = Flux.create(sink -> {
      sink.next(logMessage1);
      sink.error(new RuntimeException("error while fetching"));
    });
    when(applications.logs(any(LogsRequest.class))).thenReturn(result);
    client.getRecentLogs(pcfRequestConfig, 0);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void test_getProcessExecutorForLogTailing() {
    ExecutionLogCallback logCallback = mock(ExecutionLogCallback.class);
    doNothing().when(logCallback).saveExecutionLog(anyString());
    ProcessExecutor processExecutorForLogTailing = client.getProcessExecutorForLogTailing(
        PcfRequestConfig.builder().applicationName(APP_NAME).build(), logCallback);

    assertThat(processExecutorForLogTailing.getCommand())
        .containsExactly(BIN_SH, "-c", CF_COMMAND_FOR_APP_LOG_TAILING.replace(APP_TOKEN, APP_NAME));
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void test_tailLogsForPcf() throws Exception {
    reset(client);
    ExecutionLogCallback logCallback = mock(ExecutionLogCallback.class);
    doNothing().when(logCallback).saveExecutionLog(anyString());

    PcfRequestConfig pcfRequestConfig = PcfRequestConfig.builder().applicationName(APP_NAME).loggedin(true).build();
    ProcessExecutor processExecutor = mock(ProcessExecutor.class);
    StartedProcess startedProcess = mock(StartedProcess.class);
    doReturn(processExecutor).when(client).getProcessExecutorForLogTailing(any(), any());
    doReturn(startedProcess).when(processExecutor).start();

    StartedProcess startedProcessRet = client.tailLogsForPcf(pcfRequestConfig, logCallback);
    assertThat(startedProcess).isEqualTo(startedProcessRet);
    verify(client, never()).doLogin(any(), any(), any());

    pcfRequestConfig.setLoggedin(false);
    doReturn(true).when(client).doLogin(any(), any(), any());
    startedProcessRet = client.tailLogsForPcf(pcfRequestConfig, logCallback);
    assertThat(startedProcess).isEqualTo(startedProcessRet);
    verify(client, times(1)).doLogin(any(), any(), any());

    reset(client);
    doReturn(false).when(client).doLogin(any(), any(), any());
    try {
      client.tailLogsForPcf(pcfRequestConfig, logCallback);
    } catch (PivotalClientApiException e) {
      assertThat(e.getCause().getMessage()).contains("Failed to login");
    }
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void test_doLogin() throws Exception {
    PcfClientImpl client = spy(PcfClientImpl.class);
    ExecutionLogCallback mockCallback = mock(ExecutionLogCallback.class);
    doNothing().when(mockCallback).saveExecutionLog(anyString());
    doReturn(0).when(client).executeCommand(anyString(), any(), any());
    Map<String, String> env = new HashMap<>();
    env.put("CF_HOME", "CF_HOME");
    doReturn(env).when(client).getEnvironmentMapForPcfExecutor(anyString());
    PcfRequestConfig config =
        PcfRequestConfig.builder().endpointUrl("api.pivotal.io").userName("user").password("passwd").build();
    client.doLogin(config, mockCallback, "conf");
    verify(client, times(3)).executeCommand(anyString(), anyMap(), any());
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void test_extractRouteInfoFromPath() throws Exception {
    PcfClientImpl client = spy(PcfClientImpl.class);
    Set<String> domains = new HashSet<>(asList("example.com", "z.example.com"));

    PcfRouteInfo info = client.extractRouteInfoFromPath(domains, "example.com:5000");
    assertThat(info.getType()).isEqualTo(PCF_ROUTE_TYPE_TCP);
    assertThat(info.getDomain()).isEqualTo("example.com");
    assertThat(info.getPort()).isEqualTo("5000");

    info = client.extractRouteInfoFromPath(domains, "cdp-10515.z.example.com/path");
    assertThat(info.getType()).isEqualTo(PCF_ROUTE_TYPE_HTTP);
    assertThat(info.getDomain()).isEqualTo("z.example.com");
    assertThat(info.getHostName()).isEqualTo("cdp-10515");
    assertThat(info.getPath()).isEqualTo("path");

    info = client.extractRouteInfoFromPath(domains, "cdp-10515.z.example.com");
    assertThat(info.getType()).isEqualTo(PCF_ROUTE_TYPE_HTTP);
    assertThat(info.getDomain()).isEqualTo("z.example.com");
    assertThat(info.getHostName()).isEqualTo("cdp-10515");
    assertThat(info.getPath()).isNullOrEmpty();

    info = client.extractRouteInfoFromPath(domains, "z.example.com");
    assertThat(info.getType()).isEqualTo(PCF_ROUTE_TYPE_HTTP);
    assertThat(info.getDomain()).isEqualTo("z.example.com");
    assertThat(info.getHostName()).isNullOrEmpty();
    assertThat(info.getPath()).isNullOrEmpty();
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void test_executeRoutesOperationForApplicationUsingCli() throws Exception {
    PcfClientImpl client = spy(PcfClientImpl.class);
    ExecutionLogCallback mockCallback = mock(ExecutionLogCallback.class);
    doNothing().when(mockCallback).saveExecutionLog(anyString());
    PcfRequestConfig requestConfig = PcfRequestConfig.builder()
                                         .useCFCLI(true)
                                         .loggedin(false)
                                         .cfHomeDirPath("/cf/home")
                                         .applicationName("App_BG_00")
                                         .build();
    doReturn(true).when(client).doLogin(any(), any(), anyString());
    List<Domain> domains = singletonList(Domain.builder().name("example.com").id("id").status(Status.OWNED).build());
    doReturn(domains).when(client).getAllDomainsForSpace(any());
    Map<String, String> envMap = new HashMap<>();
    envMap.put("CF_HOME", "/cf/home");
    PcfRouteInfo info = PcfRouteInfo.builder()
                            .type(PCF_ROUTE_TYPE_HTTP)
                            .domain("example.com")
                            .hostName("cdp-10515")
                            .path("path")
                            .build();
    doReturn(info).when(client).extractRouteInfoFromPath(any(), anyString());
    doReturn(0).when(client).executeCommand(anyString(), any(), any());
    client.executeRoutesOperationForApplicationUsingCli(
        "cf map-route", requestConfig, singletonList("cdp-10515.z.example.com/path"), mockCallback);
    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(client).executeCommand(captor.capture(), any(), any());
    String value = captor.getValue();
    assertThat(value).isEqualTo("cf map-route App_BG_00 example.com --hostname cdp-10515  --path path ");
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void test_createRouteFromPath() throws Exception {
    PcfClientImpl clientImpl = spy(PcfClientImpl.class);
    ExecutionLogCallback logger = mock(ExecutionLogCallback.class);
    doNothing().when(logger).saveExecutionLog(anyString());

    PcfRequestConfig pcfRequestConfig = PcfRequestConfig.builder().build();
    Set<String> domains = new HashSet<>();
    domains.add("apps.io");
    domains.add("harness.io");
    domains.add("z.harness.io");

    try {
      clientImpl.createRouteFromPath("app1.cfapps1.io", pcfRequestConfig, domains);
      fail("Exception was expected");
    } catch (Exception e) {
      assertThat(e instanceof PivotalClientApiException).isTrue();
      assertThat(e.getMessage().contains("used domain not present in this space")).isTrue();
    }

    doNothing()
        .when(clientImpl)
        .createRouteMap(any(), anyString(), anyString(), anyString(), anyBoolean(), anyBoolean(), anyInt());
    clientImpl.createRouteFromPath("app1.apps.io", pcfRequestConfig, domains);
    ArgumentCaptor<String> hostCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> domainCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);

    verify(clientImpl)
        .createRouteMap(any(), hostCaptor.capture(), domainCaptor.capture(), pathCaptor.capture(), anyBoolean(),
            anyBoolean(), anyInt());
    assertThat(hostCaptor.getValue()).isEqualTo("app1");
    assertThat(domainCaptor.getValue()).isEqualTo("apps.io");
    assertThat(isBlank(pathCaptor.getValue())).isTrue();

    reset(clientImpl);
    doNothing()
        .when(clientImpl)
        .createRouteMap(any(), anyString(), anyString(), anyString(), anyBoolean(), anyBoolean(), anyInt());
    clientImpl.createRouteFromPath("app1.apps.io/inside/display.jsp", pcfRequestConfig, domains);
    hostCaptor = ArgumentCaptor.forClass(String.class);
    domainCaptor = ArgumentCaptor.forClass(String.class);
    pathCaptor = ArgumentCaptor.forClass(String.class);

    verify(clientImpl)
        .createRouteMap(any(), hostCaptor.capture(), domainCaptor.capture(), pathCaptor.capture(), anyBoolean(),
            anyBoolean(), anyInt());
    assertThat(hostCaptor.getValue()).isEqualTo("app1");
    assertThat(domainCaptor.getValue()).isEqualTo("apps.io");
    assertThat(pathCaptor.getValue()).isEqualTo("/inside/display.jsp");

    reset(clientImpl);
    doNothing()
        .when(clientImpl)
        .createRouteMap(any(), anyString(), anyString(), anyString(), anyBoolean(), anyBoolean(), anyInt());
    clientImpl.createRouteFromPath("cdp-10128.z.harness.io", pcfRequestConfig, domains);
    hostCaptor = ArgumentCaptor.forClass(String.class);
    domainCaptor = ArgumentCaptor.forClass(String.class);
    pathCaptor = ArgumentCaptor.forClass(String.class);

    verify(clientImpl)
        .createRouteMap(any(), hostCaptor.capture(), domainCaptor.capture(), pathCaptor.capture(), anyBoolean(),
            anyBoolean(), anyInt());
    assertThat(hostCaptor.getValue()).isEqualTo("cdp-10128");
    assertThat(domainCaptor.getValue()).isEqualTo("z.harness.io");
  }
}
