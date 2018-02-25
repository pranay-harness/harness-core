package software.wings.service;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.AppContainer.Builder.anAppContainer;
import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.ConfigFile.DEFAULT_TEMPLATE_ID;
import static software.wings.beans.EntityVersion.Builder.anEntityVersion;
import static software.wings.beans.Graph.Builder.aGraph;
import static software.wings.beans.GraphNode.GraphNodeBuilder.aGraphNode;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.Service.Builder.aService;
import static software.wings.beans.ServiceTemplate.Builder.aServiceTemplate;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.beans.WorkflowPhase.WorkflowPhaseBuilder.aWorkflowPhase;
import static software.wings.beans.command.Command.Builder.aCommand;
import static software.wings.beans.command.ExecCommandUnit.Builder.anExecCommandUnit;
import static software.wings.beans.command.ServiceCommand.Builder.aServiceCommand;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.dl.PageResponse.Builder.aPageResponse;
import static software.wings.utils.ArtifactType.JAR;
import static software.wings.utils.ArtifactType.WAR;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_COMMAND_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID_CHANGED;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.SERVICE_VARIABLE_ID;
import static software.wings.utils.WingsTestConstants.TARGET_APP_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_NAME;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.stubbing.Answer;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.WingsBaseTest;
import software.wings.api.DeploymentType;
import software.wings.beans.Application;
import software.wings.beans.ConfigFile;
import software.wings.beans.EntityType;
import software.wings.beans.EntityVersion.ChangeType;
import software.wings.beans.ErrorCode;
import software.wings.beans.Graph;
import software.wings.beans.LambdaSpecification;
import software.wings.beans.LambdaSpecification.FunctionSpecification;
import software.wings.beans.Notification;
import software.wings.beans.PhaseStepType;
import software.wings.beans.SearchFilter;
import software.wings.beans.Service;
import software.wings.beans.Service.Builder;
import software.wings.beans.ServiceVariable;
import software.wings.beans.Setup;
import software.wings.beans.Setup.SetupStatus;
import software.wings.beans.Workflow.WorkflowBuilder;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandUnitType;
import software.wings.beans.command.ServiceCommand;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.scheduler.JobScheduler;
import software.wings.service.impl.ServiceResourceServiceImpl;
import software.wings.service.impl.yaml.YamlChangeSetHelper;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.CommandService;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.EntityVersionService;
import software.wings.service.intfc.NotificationService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.ServiceVariableService;
import software.wings.service.intfc.SetupService;
import software.wings.service.intfc.WorkflowService;
import software.wings.stencils.Stencil;
import software.wings.utils.BoundedInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Created by anubhaw on 5/4/16.
 */
public class ServiceResourceServiceTest extends WingsBaseTest {
  private static final Command.Builder commandBuilder = aCommand().withName("START").addCommandUnits(
      anExecCommandUnit().withCommandPath("/home/xxx/tomcat").withCommandString("bin/startup.sh").build());
  private static final ServiceCommand.Builder serviceCommandBuilder = aServiceCommand()
                                                                          .withServiceId(SERVICE_ID)
                                                                          .withUuid(SERVICE_COMMAND_ID)
                                                                          .withAppId(APP_ID)
                                                                          .withName("START")
                                                                          .withCommand(commandBuilder.but().build());
  private static final Builder serviceBuilder =
      aService()
          .withUuid(SERVICE_ID)
          .withAppId(APP_ID)
          .withName("SERVICE_NAME")
          .withDescription("SERVICE_DESC")
          .withArtifactType(JAR)
          .withAppContainer(anAppContainer().withUuid("APP_CONTAINER_ID").build());

  @Inject @Named("primaryDatastore") private AdvancedDatastore datastore;

  @Rule public TemporaryFolder folder = new TemporaryFolder();

  private static Answer executeRunnable(ArgumentCaptor<Runnable> runnableCaptor) {
    return invocation -> {
      runnableCaptor.getValue().run();
      return null;
    };
  }

  @Mock private ActivityService activityService;
  @Mock private NotificationService notificationService;
  @Mock private SetupService setupService;
  @Mock private EntityVersionService entityVersionService;
  @Mock private CommandService commandService;
  @Mock private WorkflowService workflowService;
  @Mock private WingsPersistence wingsPersistence;
  @Mock private ServiceTemplateService serviceTemplateService;
  @Mock private ConfigService configService;
  @Mock private ServiceVariableService serviceVariableService;
  @Mock private ArtifactStreamService artifactStreamService;
  @Mock private AppService appService;
  @Mock private YamlChangeSetHelper yamlChangeSetHelper;
  @Mock private ExecutorService executorService;

  @Mock private JobScheduler jobScheduler;

  @Inject @InjectMocks private ServiceResourceService srs;

  @Spy @InjectMocks private ServiceResourceService spyServiceResourceService = new ServiceResourceServiceImpl();

  @Captor
  private ArgumentCaptor<ServiceCommand> serviceCommandArgumentCaptor = ArgumentCaptor.forClass(ServiceCommand.class);

  @Mock private UpdateOperations<Service> updateOperations;

  /**
   * Sets the up.
   *
   * @throws Exception the exception
   */
  @Before
  public void setUp() throws Exception {
    when(wingsPersistence.saveAndGet(eq(Service.class), any(Service.class))).thenReturn(serviceBuilder.but().build());
    when(wingsPersistence.get(Service.class, APP_ID, SERVICE_ID)).thenReturn(serviceBuilder.but().build());
    when(wingsPersistence.get(ServiceCommand.class, APP_ID, SERVICE_COMMAND_ID))
        .thenReturn(serviceCommandBuilder.but().build());
    when(appService.get(TARGET_APP_ID))
        .thenReturn(Application.Builder.anApplication().withAccountId(ACCOUNT_ID).build());
    when(appService.get(APP_ID)).thenReturn(Application.Builder.anApplication().withAccountId(ACCOUNT_ID).build());

    when(wingsPersistence.createUpdateOperations(Service.class)).thenReturn(updateOperations);
    when(updateOperations.set(anyString(), any())).thenReturn(updateOperations);

    when(wingsPersistence.createQuery(Service.class)).thenReturn(datastore.createQuery(Service.class));
    when(wingsPersistence.createQuery(ServiceCommand.class)).thenReturn(datastore.createQuery(ServiceCommand.class));
    when(wingsPersistence.createQuery(Command.class)).thenReturn(datastore.createQuery(Command.class));

    PageRequest<ServiceCommand> serviceCommandPageRequest = aPageRequest()
                                                                .withLimit(PageRequest.UNLIMITED)
                                                                .addFilter("appId", EQ, APP_ID)
                                                                .addFilter("serviceId", EQ, SERVICE_ID)
                                                                .build();

    when(wingsPersistence.query(ServiceCommand.class, serviceCommandPageRequest))
        .thenReturn(PageResponse.Builder.aPageResponse()
                        .withResponse(asList(aServiceCommand()
                                                 .withTargetToAllEnv(true)
                                                 .withName("START")
                                                 .withCommand(commandBuilder.build())
                                                 .build()))
                        .build());
  }

  /**
   * Should list services.
   */
  @Test
  public void shouldListServices() {
    PageRequest<Service> request = new PageRequest<>();
    request.addFilter("appId", EQ, APP_ID);
    when(wingsPersistence.query(Service.class, request)).thenReturn(new PageResponse<>());
    PageRequest<ServiceCommand> serviceCommandPageRequest =
        aPageRequest().withLimit(PageRequest.UNLIMITED).addFilter("appId", EQ, APP_ID).build();
    when(wingsPersistence.query(ServiceCommand.class, serviceCommandPageRequest))
        .thenReturn(PageResponse.Builder.aPageResponse()
                        .withResponse(asList(aServiceCommand()
                                                 .withUuid(SERVICE_COMMAND_ID)
                                                 .withServiceId(SERVICE_ID)
                                                 .withTargetToAllEnv(true)
                                                 .withName("START")
                                                 .withCommand(commandBuilder.build())
                                                 .build()))
                        .build());
    srs.list(request, false, true);
    ArgumentCaptor<PageRequest> argument = ArgumentCaptor.forClass(PageRequest.class);
    verify(wingsPersistence).query(eq(Service.class), argument.capture());
    SearchFilter filter = (SearchFilter) argument.getValue().getFilters().get(0);
    assertThat(filter.getFieldName()).isEqualTo("appId");
    assertThat(filter.getFieldValues()).containsExactly(APP_ID);
    assertThat(filter.getOp()).isEqualTo(EQ);
  }

  /**
   * Should save service.
   */
  @Test
  public void shouldSaveService() {
    Service service = serviceBuilder.but().build();
    doReturn(service).when(spyServiceResourceService).addCommand(any(), any(), any(), eq(true));
    Service savedService = spyServiceResourceService.save(service);

    assertThat(savedService.getUuid()).isEqualTo(SERVICE_ID);
    verify(wingsPersistence).saveAndGet(Service.class, service);
    verify(serviceTemplateService).createDefaultTemplatesByService(savedService);
    verify(spyServiceResourceService, times(3))
        .addCommand(eq(APP_ID), eq(SERVICE_ID), serviceCommandArgumentCaptor.capture(), eq(true));
    verify(notificationService).sendNotificationAsync(any(Notification.class));
    List<ServiceCommand> allValues = serviceCommandArgumentCaptor.getAllValues();
    assertThat(
        allValues.stream()
            .filter(
                command -> asList("Start", "Stop", "Install").contains(command.getCommand().getGraph().getGraphName()))
            .count())
        .isEqualTo(3);
  }

  /**
   * Should fetch service.
   */
  @Test
  public void shouldGetService() {
    when(wingsPersistence.get(Service.class, APP_ID, SERVICE_ID)).thenReturn(serviceBuilder.but().build());
    when(configService.getConfigFilesForEntity(APP_ID, DEFAULT_TEMPLATE_ID, SERVICE_ID)).thenReturn(new ArrayList<>());
    srs.get(APP_ID, SERVICE_ID);
    verify(wingsPersistence).get(Service.class, APP_ID, SERVICE_ID);
    verify(configService).getConfigFilesForEntity(APP_ID, DEFAULT_TEMPLATE_ID, SERVICE_ID);
  }

  @Test
  public void shouldAddSetupSuggestionForIncompleteService() {
    when(wingsPersistence.get(Service.class, APP_ID, SERVICE_ID)).thenReturn(serviceBuilder.but().build());
    when(configService.getConfigFilesForEntity(APP_ID, DEFAULT_TEMPLATE_ID, SERVICE_ID)).thenReturn(new ArrayList<>());
    when(setupService.getServiceSetupStatus(serviceBuilder.but().build())).thenReturn(Setup.Builder.aSetup().build());

    Service service = srs.get(APP_ID, SERVICE_ID, SetupStatus.INCOMPLETE);

    verify(wingsPersistence).get(Service.class, APP_ID, SERVICE_ID);
    verify(configService).getConfigFilesForEntity(APP_ID, DEFAULT_TEMPLATE_ID, SERVICE_ID);
    verify(setupService).getServiceSetupStatus(serviceBuilder.but().build());
    assertThat(service.getSetup()).isNotNull();
  }

  /**
   * Should update service.
   */
  @Test
  public void shouldUpdateService() {
    Service service = serviceBuilder.withName("UPDATED_SERVICE_NAME")
                          .withDescription("UPDATED_SERVICE_DESC")
                          .withArtifactType(WAR)
                          .withAppContainer(anAppContainer().withUuid("UPDATED_APP_CONTAINER_ID").build())
                          .build();
    ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
    when(executorService.submit(runnableCaptor.capture())).then(executeRunnable(runnableCaptor));

    srs.update(service);
    verify(wingsPersistence).update(any(Service.class), any(UpdateOperations.class));
    verify(wingsPersistence).createUpdateOperations(Service.class);
    verify(updateOperations).set("name", "UPDATED_SERVICE_NAME");
    verify(updateOperations).set("description", "UPDATED_SERVICE_DESC");

    verify(serviceTemplateService)
        .updateDefaultServiceTemplateName(APP_ID, SERVICE_ID, SERVICE_NAME, "UPDATED_SERVICE_NAME");
    verify(wingsPersistence, times(2)).get(Service.class, APP_ID, SERVICE_ID);
  }

  /**
   * Should delete service.
   */
  @Test
  public void shouldDeleteService() {
    when(wingsPersistence.delete(any(), any())).thenReturn(true);
    when(workflowService.listWorkflows(any(PageRequest.class)))
        .thenReturn(aPageResponse().withResponse(asList()).build());
    ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
    when(executorService.submit(runnableCaptor.capture())).then(executeRunnable(runnableCaptor));
    srs.delete(APP_ID, SERVICE_ID);
    InOrder inOrder = inOrder(wingsPersistence, workflowService, notificationService, serviceTemplateService,
        configService, serviceVariableService, artifactStreamService);
    inOrder.verify(wingsPersistence).get(Service.class, APP_ID, SERVICE_ID);
    inOrder.verify(workflowService).listWorkflows(any(PageResponse.class));
    inOrder.verify(wingsPersistence).delete(Service.class, SERVICE_ID);
    inOrder.verify(notificationService).sendNotificationAsync(any(Notification.class));
  }

  @Test
  public void shouldPruneDescendingObjects() {
    srs.pruneDescendingEntities(APP_ID, SERVICE_ID);
    InOrder inOrder = inOrder(wingsPersistence, workflowService, notificationService, serviceTemplateService,
        configService, serviceVariableService, artifactStreamService);
    inOrder.verify(artifactStreamService).pruneByService(APP_ID, SERVICE_ID);
    inOrder.verify(configService).pruneByService(APP_ID, SERVICE_ID);
    inOrder.verify(serviceTemplateService).pruneByService(APP_ID, SERVICE_ID);
    inOrder.verify(serviceVariableService).pruneByService(APP_ID, SERVICE_ID);
  }

  @Test
  public void shouldThrowExceptionOnReferencedServiceDelete() {
    when(workflowService.listWorkflows(any(PageRequest.class)))
        .thenReturn(
            aPageResponse()
                .withResponse(asList(WorkflowBuilder.aWorkflow()
                                         .withName(WORKFLOW_NAME)
                                         .withServices(asList(Service.Builder.aService().withUuid(SERVICE_ID).build()))
                                         .build()))
                .build());
    assertThatThrownBy(() -> srs.delete(APP_ID, SERVICE_ID))
        .isInstanceOf(WingsException.class)
        .hasMessage(ErrorCode.INVALID_REQUEST.name());
    verify(wingsPersistence).get(Service.class, APP_ID, SERVICE_ID);
    verify(workflowService).listWorkflows(any(PageResponse.class));
  }

  @Test
  public void shouldCloneService() throws IOException {
    PageRequest<ServiceCommand> serviceCommandPageRequest = aPageRequest()
                                                                .withLimit(PageRequest.UNLIMITED)
                                                                .addFilter("appId", EQ, APP_ID)
                                                                .addFilter("serviceId", EQ, SERVICE_ID)
                                                                .build();

    when(wingsPersistence.saveAndGet(eq(ServiceCommand.class), any(ServiceCommand.class))).thenAnswer(invocation -> {
      ServiceCommand command = invocation.getArgumentAt(1, ServiceCommand.class);
      command.setUuid(ID_KEY);
      return command;
    });

    Graph commandGraph = aGraph()
                             .withGraphName("START")
                             .addNodes(aGraphNode()
                                           .withId("1")
                                           .withOrigin(true)
                                           .withType("EXEC")
                                           .addProperty("commandPath", "/home/xxx/tomcat")
                                           .addProperty("commandString", "bin/startup.sh")
                                           .build())
                             .build();

    Command command = aCommand().withGraph(commandGraph).build();
    command.transformGraph();
    command.setVersion(1L);

    when(wingsPersistence.query(ServiceCommand.class, serviceCommandPageRequest))
        .thenReturn(PageResponse.Builder.aPageResponse()
                        .withResponse(asList(aServiceCommand()
                                                 .withUuid("SERVICE_COMMAND_ID")
                                                 .withTargetToAllEnv(true)
                                                 .withName("START")
                                                 .withCommand(command)
                                                 .build()))
                        .build());

    when(commandService.getCommand(APP_ID, "SERVICE_COMMAND_ID", 1)).thenReturn(command);

    Service originalService =
        serviceBuilder.but().withCommands(asList(aServiceCommand().withUuid("SERVICE_COMMAND_ID").build())).build();
    when(wingsPersistence.get(Service.class, APP_ID, SERVICE_ID)).thenReturn(originalService);

    Service savedClonedService = originalService.clone();
    savedClonedService.setName("Clone Service");
    savedClonedService.setDescription("clone description");
    savedClonedService.setUuid("CLONED_SERVICE_ID");
    when(wingsPersistence.saveAndGet(eq(Service.class), any(Service.class))).thenReturn(savedClonedService);

    doReturn(savedClonedService)
        .when(spyServiceResourceService)
        .addCommand(eq(APP_ID), eq("CLONED_SERVICE_ID"), any(ServiceCommand.class), eq(true));

    ConfigFile configFile = ConfigFile.builder().build();
    configFile.setAppId(APP_ID);
    configFile.setUuid("CONFIG_FILE_ID");
    when(configService.getConfigFilesForEntity(APP_ID, DEFAULT_TEMPLATE_ID, SERVICE_ID)).thenReturn(asList(configFile));
    when(configService.download(APP_ID, "CONFIG_FILE_ID")).thenReturn(folder.newFile("abc.txt"));

    ServiceVariable serviceVariable = ServiceVariable.builder().build();
    serviceVariable.setAppId(APP_ID);
    serviceVariable.setUuid(SERVICE_VARIABLE_ID);
    when(serviceVariableService.getServiceVariablesForEntity(APP_ID, SERVICE_ID, false))
        .thenReturn(asList(serviceVariable));

    when(serviceTemplateService.list(any(PageRequest.class), any(Boolean.class), any(Boolean.class)))
        .thenReturn(aPageResponse().withResponse(asList(aServiceTemplate().build())).build());

    Service clonedService = spyServiceResourceService.clone(
        APP_ID, SERVICE_ID, aService().withName("Clone Service").withDescription("clone description").build());

    assertThat(clonedService)
        .isNotNull()
        .isInstanceOf(Service.class)
        .hasFieldOrPropertyWithValue("name", "Clone Service")
        .hasFieldOrPropertyWithValue("description", "clone description")
        .hasFieldOrPropertyWithValue("artifactType", originalService.getArtifactType())
        .hasFieldOrPropertyWithValue("appContainer", originalService.getAppContainer());

    verify(wingsPersistence).get(Service.class, APP_ID, SERVICE_ID);
    verify(configService).getConfigFilesForEntity(APP_ID, DEFAULT_TEMPLATE_ID, SERVICE_ID);

    ArgumentCaptor<Service> serviceArgumentCaptor = ArgumentCaptor.forClass(Service.class);
    verify(wingsPersistence).saveAndGet(eq(Service.class), serviceArgumentCaptor.capture());
    Service savedService = serviceArgumentCaptor.getAllValues().get(0);
    assertThat(savedService)
        .isNotNull()
        .isInstanceOf(Service.class)
        .hasFieldOrPropertyWithValue("name", "Clone Service")
        .hasFieldOrPropertyWithValue("description", "clone description")
        .hasFieldOrPropertyWithValue("artifactType", originalService.getArtifactType())
        .hasFieldOrPropertyWithValue("appContainer", originalService.getAppContainer());

    verify(configService).getConfigFilesForEntity(APP_ID, DEFAULT_TEMPLATE_ID, SERVICE_ID);
    verify(configService).download(APP_ID, "CONFIG_FILE_ID");
    verify(configService).save(any(ConfigFile.class), new BoundedInputStream(any(InputStream.class)));
    verify(serviceVariableService).getServiceVariablesForEntity(APP_ID, SERVICE_ID, false);
    verify(serviceVariableService).save(any(ServiceVariable.class));
  }

  @Test
  public void shouldThrowExceptionOnDeleteServiceStillReferencedInWorkflow() {
    when(wingsPersistence.delete(any(), any())).thenReturn(true);
    when(workflowService.listWorkflows(any(PageRequest.class)))
        .thenReturn(
            aPageResponse()
                .withResponse(asList(aWorkflow().withServices(asList(aService().withUuid(SERVICE_ID).build())).build()))
                .build());

    assertThatThrownBy(() -> srs.delete(APP_ID, SERVICE_ID))
        .isInstanceOf(WingsException.class)
        .hasMessage(ErrorCode.INVALID_REQUEST.name());

    verify(wingsPersistence).get(Service.class, APP_ID, SERVICE_ID);
    verify(workflowService).listWorkflows(any(PageResponse.class));
    verify(wingsPersistence, never()).delete(Service.class, SERVICE_ID);
  }

  /**
   * Should add command state.
   */
  @Test
  public void shouldAddCommand() {
    when(wingsPersistence.saveAndGet(eq(ServiceCommand.class), any(ServiceCommand.class))).thenAnswer(invocation -> {
      ServiceCommand command = invocation.getArgumentAt(1, ServiceCommand.class);
      command.setUuid(ID_KEY);
      return command;
    });

    Graph commandGraph = aGraph()
                             .withGraphName("START")
                             .addNodes(aGraphNode()
                                           .withId("1")
                                           .withOrigin(true)
                                           .withType("EXEC")
                                           .addProperty("commandPath", "/home/xxx/tomcat")
                                           .addProperty("command", "bin/startup.sh")
                                           .build())
                             .build();

    Command expectedCommand = aCommand().withGraph(commandGraph).build();
    expectedCommand.transformGraph();

    srs.addCommand(APP_ID, SERVICE_ID,
        aServiceCommand().withTargetToAllEnv(true).withCommand(aCommand().withGraph(commandGraph).build()).build(),
        true);

    verify(wingsPersistence, times(2)).get(Service.class, APP_ID, SERVICE_ID);
    verify(wingsPersistence)
        .save(serviceBuilder.but()
                  .addCommands(aServiceCommand()
                                   .withTargetToAllEnv(true)
                                   .withAppId(APP_ID)
                                   .withUuid(ID_KEY)
                                   .withServiceId(SERVICE_ID)
                                   .withDefaultVersion(1)
                                   .withName("START")
                                   .build())
                  .build());
    verify(configService).getConfigFilesForEntity(APP_ID, DEFAULT_TEMPLATE_ID, SERVICE_ID);
    verify(wingsPersistence).saveAndGet(eq(ServiceCommand.class), any(ServiceCommand.class));
  }

  /**
   * Should update command.
   */
  @Test
  public void shouldUpdateCommandWhenCommandChanged() {
    Graph oldCommandGraph = aGraph()
                                .withGraphName("START")
                                .addNodes(aGraphNode()
                                              .withId("1")
                                              .withOrigin(true)
                                              .withType("EXEC")
                                              .addProperty("commandPath", "/home/xxx/tomcat")
                                              .addProperty("commandString", "bin/startup.sh")
                                              .build())
                                .build();

    Command oldCommand = aCommand().withGraph(oldCommandGraph).build();
    oldCommand.transformGraph();
    oldCommand.setVersion(1L);
    oldCommand.setDeploymentType(DeploymentType.SSH.name());

    PageRequest<ServiceCommand> serviceCommandPageRequest = aPageRequest()
                                                                .withLimit(PageRequest.UNLIMITED)
                                                                .addFilter("appId", EQ, APP_ID)
                                                                .addFilter("serviceId", EQ, SERVICE_ID)
                                                                .build();

    when(wingsPersistence.query(ServiceCommand.class, serviceCommandPageRequest))
        .thenReturn(PageResponse.Builder.aPageResponse()
                        .withResponse(asList(aServiceCommand()
                                                 .withUuid(ID_KEY)
                                                 .withTargetToAllEnv(true)
                                                 .withName("START")
                                                 .withCommand(oldCommand)
                                                 .build()))
                        .build());

    when(wingsPersistence.createUpdateOperations(ServiceCommand.class))
        .thenReturn(datastore.createUpdateOperations(ServiceCommand.class));
    when(wingsPersistence.createQuery(ServiceCommand.class)).thenReturn(datastore.createQuery(ServiceCommand.class));

    when(wingsPersistence.get(Service.class, APP_ID, SERVICE_ID))
        .thenReturn(serviceBuilder.but()
                        .addCommands(aServiceCommand()
                                         .withTargetToAllEnv(true)
                                         .withUuid(ID_KEY)
                                         .withAppId(APP_ID)
                                         .withServiceId(SERVICE_ID)
                                         .withDefaultVersion(1)
                                         .withCommand(oldCommand)
                                         .build())
                        .build());

    when(entityVersionService.newEntityVersion(
             APP_ID, EntityType.COMMAND, ID_KEY, SERVICE_ID, "START", ChangeType.UPDATED, null))
        .thenReturn(anEntityVersion().withVersion(2).build());

    when(commandService.getCommand(APP_ID, ID_KEY, 1)).thenReturn(oldCommand);

    when(entityVersionService.lastEntityVersion(APP_ID, EntityType.COMMAND, ID_KEY, SERVICE_ID))
        .thenReturn(anEntityVersion().withVersion(1).build());

    Graph commandGraph = aGraph()
                             .withGraphName("START")
                             .addNodes(aGraphNode()
                                           .withId("1")
                                           .withOrigin(true)
                                           .withType("EXEC")
                                           .addProperty("commandPath", "/home/xxx/tomcat1")
                                           .addProperty("commandString", "bin/startup.sh")
                                           .build())
                             .build();

    Command expectedCommand = aCommand().withGraph(commandGraph).build();
    expectedCommand.transformGraph();
    expectedCommand.setVersion(2L);

    Service updatedService = srs.updateCommand(APP_ID, SERVICE_ID,
        aServiceCommand()
            .withTargetToAllEnv(true)
            .withUuid(ID_KEY)
            .withName("START")
            .withCommand(aCommand().withGraph(commandGraph).build())
            .build());

    verify(wingsPersistence).createUpdateOperations(ServiceCommand.class);

    verify(wingsPersistence).update(any(Query.class), any(UpdateOperations.class));

    verify(wingsPersistence).createQuery(ServiceCommand.class);

    verify(wingsPersistence, times(2)).get(Service.class, APP_ID, SERVICE_ID);

    verify(configService).getConfigFilesForEntity(APP_ID, DEFAULT_TEMPLATE_ID, SERVICE_ID);

    assertThat(updatedService).isNotNull();
    assertThat(
        updatedService.getServiceCommands().stream().anyMatch(
            serviceCommand -> serviceCommand.getCommand().getDeploymentType().equals(oldCommand.getDeploymentType())))
        .isTrue();
  }

  /**
   * Should update command when command graph changed.
   */
  @Test
  public void shouldUpdateCommandWhenCommandGraphChanged() {
    Graph oldCommandGraph = aGraph()
                                .withGraphName("START")
                                .addNodes(aGraphNode()
                                              .withId("1")
                                              .withOrigin(true)
                                              .withType("EXEC")
                                              .withRollback(true)
                                              .addProperty("commandPath", "/home/xxx/tomcat")
                                              .addProperty("commandString", "bin/startup.sh")
                                              .build())
                                .build();

    Command oldCommand = aCommand().withGraph(oldCommandGraph).build();
    oldCommand.transformGraph();
    oldCommand.setVersion(1L);

    when(wingsPersistence.createUpdateOperations(ServiceCommand.class))
        .thenReturn(datastore.createUpdateOperations(ServiceCommand.class));
    when(wingsPersistence.createQuery(ServiceCommand.class)).thenReturn(datastore.createQuery(ServiceCommand.class));

    when(wingsPersistence.get(Service.class, APP_ID, SERVICE_ID))
        .thenReturn(serviceBuilder.but()
                        .addCommands(aServiceCommand()
                                         .withTargetToAllEnv(true)
                                         .withUuid(ID_KEY)
                                         .withAppId(APP_ID)
                                         .withServiceId(SERVICE_ID)
                                         .withDefaultVersion(1)
                                         .withCommand(oldCommand)
                                         .build())
                        .build());

    when(entityVersionService.newEntityVersion(
             APP_ID, EntityType.COMMAND, ID_KEY, SERVICE_ID, "START", ChangeType.UPDATED, null))
        .thenReturn(anEntityVersion().withVersion(2).build());

    when(commandService.getCommand(APP_ID, ID_KEY, 1)).thenReturn(oldCommand);

    when(entityVersionService.lastEntityVersion(APP_ID, EntityType.COMMAND, ID_KEY, SERVICE_ID))
        .thenReturn(anEntityVersion().withVersion(1).build());

    Graph commandGraph = aGraph()
                             .withGraphName("START")
                             .addNodes(aGraphNode()
                                           .withId("1")
                                           .withOrigin(true)
                                           .withType("EXEC")
                                           .withRollback(false)
                                           .addProperty("commandPath", "/home/xxx/tomcat")
                                           .addProperty("commandString", "bin/startup.sh")
                                           .build())
                             .build();

    Command expectedCommand = aCommand().withGraph(commandGraph).build();
    expectedCommand.transformGraph();
    expectedCommand.setVersion(2L);

    srs.updateCommand(APP_ID, SERVICE_ID,
        aServiceCommand()
            .withTargetToAllEnv(true)
            .withUuid(ID_KEY)
            .withName("START")
            .withCommand(aCommand().withGraph(commandGraph).build())
            .build());

    verify(wingsPersistence).createUpdateOperations(ServiceCommand.class);

    verify(wingsPersistence).update(any(Query.class), any(UpdateOperations.class));

    verify(wingsPersistence).createQuery(ServiceCommand.class);

    verify(wingsPersistence, times(2)).get(Service.class, APP_ID, SERVICE_ID);

    verify(configService).getConfigFilesForEntity(APP_ID, DEFAULT_TEMPLATE_ID, SERVICE_ID);

    verify(commandService).update(expectedCommand);
  }

  /**
   * Should not update command nothing changed.
   */
  @Test
  public void shouldNotUpdateCommandNothingChanged() {
    Graph oldCommandGraph = aGraph()
                                .withGraphName("START")
                                .addNodes(aGraphNode()
                                              .withId("1")
                                              .withOrigin(true)
                                              .withType("EXEC")
                                              .addProperty("commandPath", "/home/xxx/tomcat")
                                              .addProperty("commandString", "bin/startup.sh")
                                              .build())
                                .build();

    Command oldCommand = aCommand().withGraph(oldCommandGraph).build();
    oldCommand.transformGraph();
    oldCommand.setVersion(1L);

    when(wingsPersistence.createUpdateOperations(ServiceCommand.class))
        .thenReturn(datastore.createUpdateOperations(ServiceCommand.class));
    when(wingsPersistence.createQuery(ServiceCommand.class)).thenReturn(datastore.createQuery(ServiceCommand.class));

    when(wingsPersistence.get(Service.class, APP_ID, SERVICE_ID))
        .thenReturn(serviceBuilder.but()
                        .addCommands(aServiceCommand()
                                         .withTargetToAllEnv(true)
                                         .withUuid(ID_KEY)
                                         .withAppId(APP_ID)
                                         .withServiceId(SERVICE_ID)
                                         .withDefaultVersion(1)
                                         .withCommand(oldCommand)
                                         .build())
                        .build());

    when(entityVersionService.newEntityVersion(
             APP_ID, EntityType.COMMAND, ID_KEY, SERVICE_ID, "START", ChangeType.UPDATED, null))
        .thenReturn(anEntityVersion().withVersion(2).build());

    when(commandService.getCommand(APP_ID, ID_KEY, 1)).thenReturn(oldCommand);

    when(entityVersionService.lastEntityVersion(APP_ID, EntityType.COMMAND, ID_KEY, SERVICE_ID))
        .thenReturn(anEntityVersion().withVersion(1).build());

    Graph commandGraph = aGraph()
                             .withGraphName("START")
                             .addNodes(aGraphNode()
                                           .withId("1")
                                           .withOrigin(true)
                                           .withType("EXEC")
                                           .addProperty("commandPath", "/home/xxx/tomcat")
                                           .addProperty("commandString", "bin/startup.sh")
                                           .build())
                             .build();

    Command expectedCommand = aCommand().withGraph(commandGraph).build();
    expectedCommand.transformGraph();
    expectedCommand.setVersion(2L);

    srs.updateCommand(APP_ID, SERVICE_ID,
        aServiceCommand()
            .withTargetToAllEnv(true)
            .withUuid(ID_KEY)
            .withName("START")
            .withCommand(aCommand().withGraph(commandGraph).build())
            .build());

    verify(wingsPersistence, times(2)).get(Service.class, APP_ID, SERVICE_ID);

    verify(wingsPersistence).createUpdateOperations(ServiceCommand.class);

    verify(wingsPersistence).update(any(Query.class), any(UpdateOperations.class));

    verify(wingsPersistence).createQuery(ServiceCommand.class);

    verify(commandService, never()).save(any(Command.class), eq(true));

    verify(configService).getConfigFilesForEntity(APP_ID, DEFAULT_TEMPLATE_ID, SERVICE_ID);
  }

  /**
   * Should delete command state.
   */
  @Test
  public void shouldDeleteCommand() {
    when(workflowService.listWorkflows(any(PageRequest.class)))
        .thenReturn(aPageResponse().withResponse(asList()).build());
    when(wingsPersistence.delete(any(Query.class))).thenReturn(true);
    when(wingsPersistence.delete(any(ServiceCommand.class))).thenReturn(true);
    srs.deleteCommand(APP_ID, SERVICE_ID, SERVICE_COMMAND_ID);

    verify(wingsPersistence, times(2)).get(Service.class, APP_ID, SERVICE_ID);
    verify(wingsPersistence, times(1)).get(ServiceCommand.class, APP_ID, SERVICE_COMMAND_ID);
    verify(workflowService, times(1)).listWorkflows(any(PageResponse.class));
    verify(wingsPersistence, times(1)).createQuery(Command.class);
    verify(wingsPersistence, times(1)).delete(any(ServiceCommand.class));
    verify(wingsPersistence, times(1)).delete(any(Query.class));
    verify(configService).getConfigFilesForEntity(APP_ID, DEFAULT_TEMPLATE_ID, SERVICE_ID);
  }

  @Test
  public void shouldThrowExceptionOnReferencedServiceCommandDelete() {
    ServiceCommand serviceCommand = serviceCommandBuilder.but().build();
    when(workflowService.listWorkflows(any(PageRequest.class)))
        .thenReturn(aPageResponse()
                        .withResponse(asList(
                            WorkflowBuilder.aWorkflow()
                                .withName(WORKFLOW_NAME)
                                .withServices(asList(Service.Builder.aService()
                                                         .withUuid(SERVICE_ID)
                                                         .withAppId(APP_ID)
                                                         .withCommands(asList(serviceCommand))
                                                         .build()))
                                .withOrchestrationWorkflow(
                                    aCanaryOrchestrationWorkflow()
                                        .withWorkflowPhases(asList(
                                            aWorkflowPhase()
                                                .withServiceId(SERVICE_ID)
                                                .addPhaseStep(aPhaseStep(PhaseStepType.STOP_SERVICE, "Phase 1")
                                                                  .addStep(aGraphNode()
                                                                               .withType("COMMAND")
                                                                               .addProperty("commandName", "START")
                                                                               .build())
                                                                  .build())
                                                .build()))
                                        .build())
                                .build()))
                        .build());
    assertThatThrownBy(() -> srs.deleteCommand(APP_ID, SERVICE_ID, SERVICE_COMMAND_ID))
        .isInstanceOf(WingsException.class)
        .hasMessage(ErrorCode.INVALID_REQUEST.name());
    verify(wingsPersistence).get(Service.class, APP_ID, SERVICE_ID);
    verify(wingsPersistence).get(ServiceCommand.class, APP_ID, SERVICE_COMMAND_ID);
    verify(workflowService).listWorkflows(any(PageResponse.class));
  }

  @Test
  public void shouldNotThrowExceptionOnReferencedServiceCommandDelete() {
    ServiceCommand serviceCommand = serviceCommandBuilder.but().build();
    when(workflowService.listWorkflows(any(PageRequest.class)))
        .thenReturn(aPageResponse()
                        .withResponse(asList(
                            WorkflowBuilder.aWorkflow()
                                .withName(WORKFLOW_NAME)
                                .withServices(asList(Service.Builder.aService()
                                                         .withUuid(SERVICE_ID_CHANGED)
                                                         .withAppId(APP_ID)
                                                         .withCommands(asList(serviceCommand))
                                                         .build()))
                                .withOrchestrationWorkflow(
                                    aCanaryOrchestrationWorkflow()
                                        .withWorkflowPhases(asList(
                                            aWorkflowPhase()
                                                .withServiceId(SERVICE_ID_CHANGED)
                                                .addPhaseStep(aPhaseStep(PhaseStepType.STOP_SERVICE, "Phase 1")
                                                                  .addStep(aGraphNode()
                                                                               .withType("COMMAND")
                                                                               .addProperty("commandName", "START")
                                                                               .build())
                                                                  .build())
                                                .build()))
                                        .build())
                                .build()))
                        .build());
    when(wingsPersistence.delete(any(Query.class))).thenReturn(true);
    when(wingsPersistence.delete(any(ServiceCommand.class))).thenReturn(true);

    srs.deleteCommand(APP_ID, SERVICE_ID, SERVICE_COMMAND_ID);

    verify(wingsPersistence, times(2)).get(Service.class, APP_ID, SERVICE_ID);
    verify(wingsPersistence, times(1)).get(ServiceCommand.class, APP_ID, SERVICE_COMMAND_ID);
    verify(workflowService, times(1)).listWorkflows(any(PageResponse.class));
    verify(wingsPersistence, times(1)).createQuery(Command.class);
    verify(wingsPersistence, times(1)).delete(any(ServiceCommand.class));
    verify(wingsPersistence, times(1)).delete(any(Query.class));
    verify(configService).getConfigFilesForEntity(APP_ID, DEFAULT_TEMPLATE_ID, SERVICE_ID);
  }

  /**
   * Should get command stencils.
   */
  @Test
  public void shouldGetCommandStencils() {
    when(wingsPersistence.get(eq(Service.class), anyString(), anyString()))
        .thenReturn(serviceBuilder.but()
                        .addCommands(aServiceCommand()
                                         .withTargetToAllEnv(true)
                                         .withName("START")
                                         .withCommand(commandBuilder.build())
                                         .build(),
                            aServiceCommand()
                                .withTargetToAllEnv(true)
                                .withName("START2")
                                .withCommand(commandBuilder.but().withName("START2").build())
                                .build())
                        .build());

    PageRequest<ServiceCommand> serviceCommandPageRequest = aPageRequest()
                                                                .withLimit(PageRequest.UNLIMITED)
                                                                .addFilter("appId", EQ, APP_ID)
                                                                .addFilter("serviceId", EQ, SERVICE_ID)
                                                                .build();

    when(wingsPersistence.query(ServiceCommand.class, serviceCommandPageRequest))
        .thenReturn(PageResponse.Builder.aPageResponse()
                        .withResponse(asList(aServiceCommand()
                                                 .withTargetToAllEnv(false)
                                                 .withName("START")
                                                 .withCommand(commandBuilder.build())
                                                 .build(),
                            aServiceCommand()
                                .withTargetToAllEnv(true)
                                .withName("START2")
                                .withCommand(commandBuilder.but().withName("START2").build())
                                .build()))
                        .build());
    List<Stencil> commandStencils = srs.getCommandStencils(APP_ID, SERVICE_ID, null);

    assertThat(commandStencils)
        .isNotNull()
        .hasSize(CommandUnitType.values().length + 1)
        .extracting(Stencil::getName)
        .contains("START", "START2");

    verify(wingsPersistence, times(2)).get(Service.class, APP_ID, SERVICE_ID);
  }

  /**
   * Should get command by name.
   */
  @Test
  public void shouldGetCommandByName() {
    when(wingsPersistence.get(eq(Service.class), anyString(), anyString()))
        .thenReturn(serviceBuilder.but()
                        .addCommands(aServiceCommand()
                                         .withTargetToAllEnv(true)
                                         .withName("START")
                                         .withCommand(commandBuilder.build())
                                         .build())
                        .build());

    assertThat(srs.getCommandByName(APP_ID, SERVICE_ID, "START")).isNotNull();

    verify(wingsPersistence, times(1)).get(Service.class, APP_ID, SERVICE_ID);
  }

  @Test
  public void shouldGetCommandByNameAndEnv() {
    when(wingsPersistence.get(eq(Service.class), anyString(), anyString()))
        .thenReturn(serviceBuilder.but()
                        .addCommands(aServiceCommand()
                                         .withTargetToAllEnv(true)
                                         .withName("START")
                                         .withCommand(commandBuilder.build())
                                         .build())
                        .build());

    assertThat(srs.getCommandByName(APP_ID, SERVICE_ID, ENV_ID, "START")).isNotNull();

    verify(wingsPersistence, times(1)).get(Service.class, APP_ID, SERVICE_ID);
  }

  @Test
  public void shouldGetCommandByNameAndEnvForSpecificEnv() {
    when(wingsPersistence.get(eq(Service.class), anyString(), anyString()))
        .thenReturn(
            serviceBuilder.but()
                .addCommands(aServiceCommand()
                                 .withEnvIdVersionMap(ImmutableMap.of(ENV_ID, anEntityVersion().withVersion(2).build()))
                                 .withName("START")
                                 .withCommand(commandBuilder.build())
                                 .build())
                .build());

    assertThat(srs.getCommandByName(APP_ID, SERVICE_ID, ENV_ID, "START")).isNotNull();

    verify(wingsPersistence, times(1)).get(Service.class, APP_ID, SERVICE_ID);
  }

  @Test
  public void shouldGetCommandByNameAndEnvForSpecificEnvNotTargetted() {
    PageRequest<ServiceCommand> serviceCommandPageRequest = aPageRequest()
                                                                .withLimit(PageRequest.UNLIMITED)
                                                                .addFilter("appId", EQ, APP_ID)
                                                                .addFilter("serviceId", EQ, SERVICE_ID)
                                                                .build();

    when(wingsPersistence.query(ServiceCommand.class, serviceCommandPageRequest))
        .thenReturn(PageResponse.Builder.aPageResponse()
                        .withResponse(asList(aServiceCommand()
                                                 .withTargetToAllEnv(false)
                                                 .withName("START")
                                                 .withCommand(commandBuilder.build())
                                                 .build()))
                        .build());

    assertThat(srs.getCommandByName(APP_ID, SERVICE_ID, ENV_ID, "START")).isNull();

    verify(wingsPersistence, times(1)).get(Service.class, APP_ID, SERVICE_ID);
  }

  @Test
  public void testLambdaValidation() {
    FunctionSpecification functionSpecification = FunctionSpecification.builder()
                                                      .runtime("TestRunTime")
                                                      .functionName("TestFunctionName")
                                                      .handler("TestHandler")
                                                      .build();
    LambdaSpecification lambdaSpecification =
        LambdaSpecification.builder()
            .serviceId("TestServiceID")
            .functions(Arrays.asList(functionSpecification, functionSpecification))
            .build();
    lambdaSpecification.setAppId("TestAppID");
    try {
      srs.updateLambdaSpecification(lambdaSpecification);
      fail("Should have thrown a wingsException");
    } catch (WingsException e) {
      log().info("Expected exception");
    }

    FunctionSpecification functionSpecification2 = FunctionSpecification.builder()
                                                       .runtime("TestRunTime")
                                                       .functionName("TestFunctionName2")
                                                       .handler("TestHandler")
                                                       .build();
    lambdaSpecification = LambdaSpecification.builder()
                              .serviceId("TestServiceID")
                              .functions(Arrays.asList(functionSpecification, functionSpecification2))
                              .build();
    lambdaSpecification.setAppId("TestAppID");
    Mockito.when(wingsPersistence.saveAndGet(Mockito.any(Class.class), Mockito.any(LambdaSpecification.class)))
        .thenReturn(lambdaSpecification);

    try {
      srs.updateLambdaSpecification(lambdaSpecification);
    } catch (WingsException e) {
      fail("Should not have thrown a wingsException", e);
    }
  }
}
