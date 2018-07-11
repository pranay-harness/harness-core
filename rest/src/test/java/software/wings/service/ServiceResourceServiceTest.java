package software.wings.service;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
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
import static software.wings.beans.Base.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.CommandCategory.Type.COMMANDS;
import static software.wings.beans.CommandCategory.Type.COPY;
import static software.wings.beans.CommandCategory.Type.SCRIPTS;
import static software.wings.beans.CommandCategory.Type.VERIFICATIONS;
import static software.wings.beans.ConfigFile.DEFAULT_TEMPLATE_ID;
import static software.wings.beans.EntityVersion.Builder.anEntityVersion;
import static software.wings.beans.ErrorCode.INVALID_REQUEST;
import static software.wings.beans.Graph.Builder.aGraph;
import static software.wings.beans.GraphNode.GraphNodeBuilder.aGraphNode;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.Service.ServiceBuilder;
import static software.wings.beans.ServiceTemplate.Builder.aServiceTemplate;
import static software.wings.beans.Variable.VariableBuilder.aVariable;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.beans.WorkflowPhase.WorkflowPhaseBuilder.aWorkflowPhase;
import static software.wings.beans.command.Command.Builder.aCommand;
import static software.wings.beans.command.CommandType.START;
import static software.wings.beans.command.CommandUnitType.COMMAND;
import static software.wings.beans.command.CommandUnitType.COPY_CONFIGS;
import static software.wings.beans.command.CommandUnitType.DOCKER_START;
import static software.wings.beans.command.CommandUnitType.DOCKER_STOP;
import static software.wings.beans.command.CommandUnitType.EXEC;
import static software.wings.beans.command.CommandUnitType.PORT_CHECK_CLEARED;
import static software.wings.beans.command.CommandUnitType.PORT_CHECK_LISTENING;
import static software.wings.beans.command.CommandUnitType.PROCESS_CHECK_RUNNING;
import static software.wings.beans.command.CommandUnitType.SCP;
import static software.wings.beans.command.CommandUnitType.values;
import static software.wings.beans.command.ExecCommandUnit.Builder.anExecCommandUnit;
import static software.wings.beans.command.ScpCommandUnit.Builder.aScpCommandUnit;
import static software.wings.beans.command.ServiceCommand.Builder.aServiceCommand;
import static software.wings.common.TemplateConstants.HARNESS_GALLERY;
import static software.wings.common.TemplateConstants.LATEST_TAG;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.dl.PageResponse.PageResponseBuilder.aPageResponse;
import static software.wings.security.UserThreadLocal.userGuard;
import static software.wings.stencils.StencilCategory.CONTAINERS;
import static software.wings.utils.ArtifactType.JAR;
import static software.wings.utils.ArtifactType.WAR;
import static software.wings.utils.TemplateTestConstants.TEMPLATE_CUSTOM_KEYWORD;
import static software.wings.utils.TemplateTestConstants.TEMPLATE_DESC;
import static software.wings.utils.TemplateTestConstants.TEMPLATE_ID;
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import de.danielbechler.diff.ObjectDifferBuilder;
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
import software.wings.beans.Application;
import software.wings.beans.CommandCategory;
import software.wings.beans.ConfigFile;
import software.wings.beans.EntityType;
import software.wings.beans.EntityVersion.ChangeType;
import software.wings.beans.Graph;
import software.wings.beans.LambdaSpecification;
import software.wings.beans.LambdaSpecification.FunctionSpecification;
import software.wings.beans.Notification;
import software.wings.beans.PhaseStepType;
import software.wings.beans.SearchFilter;
import software.wings.beans.Service;
import software.wings.beans.ServiceVariable;
import software.wings.beans.Workflow.WorkflowBuilder;
import software.wings.beans.command.AmiCommandUnit;
import software.wings.beans.command.AwsLambdaCommandUnit;
import software.wings.beans.command.CleanupSshCommandUnit;
import software.wings.beans.command.CodeDeployCommandUnit;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandType;
import software.wings.beans.command.CopyConfigCommandUnit;
import software.wings.beans.command.InitSshCommandUnit;
import software.wings.beans.command.ServiceCommand;
import software.wings.beans.container.ContainerTask;
import software.wings.beans.container.KubernetesContainerTask;
import software.wings.beans.container.KubernetesPayload;
import software.wings.beans.template.Template;
import software.wings.beans.template.command.SshCommandTemplate;
import software.wings.dl.HQuery;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.scheduler.JobScheduler;
import software.wings.security.UserThreadLocal;
import software.wings.service.impl.ServiceResourceServiceImpl;
import software.wings.service.impl.command.CommandHelper;
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
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.template.TemplateService;
import software.wings.stencils.Stencil;
import software.wings.utils.BoundedInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
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
                                                                          .withDefaultVersion(1)
                                                                          .withAppId(APP_ID)
                                                                          .withName("START")
                                                                          .withCommand(commandBuilder.but().build());
  private ServiceBuilder serviceBuilder = getServiceBuilder();

  PageRequest<ServiceCommand> serviceCommandPageRequest = getServiceCommandPageRequest();

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
  @Mock private TemplateService templateService;

  @Inject @InjectMocks private ServiceResourceService srs;

  @Inject @InjectMocks private CommandHelper commandHelper;

  @Spy @InjectMocks private ServiceResourceService spyServiceResourceService = new ServiceResourceServiceImpl();

  @Captor
  private ArgumentCaptor<ServiceCommand> serviceCommandArgumentCaptor = ArgumentCaptor.forClass(ServiceCommand.class);

  @Mock private UpdateOperations<Service> updateOperations;

  @Mock private HQuery<ServiceCommand> serviceCommandQuery;

  private static ServiceBuilder getServiceBuilder() {
    return Service.builder()
        .uuid(SERVICE_ID)
        .appId(APP_ID)
        .name("SERVICE_NAME")
        .description("SERVICE_DESC")
        .artifactType(JAR)
        .appContainer(anAppContainer().withUuid("APP_CONTAINER_ID").build());
  }

  @Before
  public void setUp() throws Exception {
    when(wingsPersistence.saveAndGet(eq(Service.class), any(Service.class))).thenReturn(serviceBuilder.build());
    when(wingsPersistence.get(Service.class, APP_ID, SERVICE_ID)).thenReturn(serviceBuilder.build());
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
    when(wingsPersistence.createQuery(ContainerTask.class)).thenReturn(datastore.createQuery(ContainerTask.class));

    PageRequest<ServiceCommand> serviceCommandPageRequest = getServiceCommandPageRequest();

    when(wingsPersistence.query(ServiceCommand.class, serviceCommandPageRequest))
        .thenReturn(aPageResponse()
                        .withResponse(asList(aServiceCommand()
                                                 .withTargetToAllEnv(true)
                                                 .withName("START")
                                                 .withDefaultVersion(1)
                                                 .withCommand(commandBuilder.build())
                                                 .build()))
                        .build());
  }

  @Test
  public void shouldListServices() {
    PageRequest<Service> request = new PageRequest<>();
    request.addFilter("appId", EQ, APP_ID);
    when(wingsPersistence.query(Service.class, request)).thenReturn(new PageResponse<>());
    PageRequest<ServiceCommand> serviceCommandPageRequest =
        aPageRequest().withLimit(PageRequest.UNLIMITED).addFilter("appId", EQ, APP_ID).build();
    when(wingsPersistence.query(ServiceCommand.class, serviceCommandPageRequest))
        .thenReturn(aPageResponse()
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

  @Test
  public void shouldSaveService() {
    Service service = serviceBuilder.build();
    doReturn(service).when(spyServiceResourceService).addCommand(any(), any(), any(ServiceCommand.class), eq(true));
    Service savedService = spyServiceResourceService.save(service);

    assertThat(savedService.getUuid()).isEqualTo(SERVICE_ID);
    ArgumentCaptor<Service> calledService = ArgumentCaptor.forClass(Service.class);
    verify(wingsPersistence).saveAndGet(eq(Service.class), calledService.capture());
    Service calledServiceValue = calledService.getValue();
    assertThat(calledServiceValue)
        .isNotNull()
        .extracting("appId", "name", "description", "artifactType")
        .containsExactly(service.getAppId(), service.getName(), service.getDescription(), service.getArtifactType());
    assertThat(calledServiceValue.getKeywords())
        .isNotNull()
        .contains(service.getName().toLowerCase(), service.getDescription().toLowerCase(),
            service.getArtifactType().name().toLowerCase());

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

  @Test
  public void shouldGetService() {
    when(wingsPersistence.get(Service.class, APP_ID, SERVICE_ID)).thenReturn(serviceBuilder.build());
    when(configService.getConfigFilesForEntity(APP_ID, DEFAULT_TEMPLATE_ID, SERVICE_ID)).thenReturn(new ArrayList<>());
    srs.get(APP_ID, SERVICE_ID);
    verify(wingsPersistence).get(Service.class, APP_ID, SERVICE_ID);
    verify(configService).getConfigFilesForEntity(APP_ID, DEFAULT_TEMPLATE_ID, SERVICE_ID);
  }

  @Test
  public void shouldUpdateService() throws IOException {
    try (UserThreadLocal.Guard guard = userGuard(null)) {
      Service service = serviceBuilder.name("UPDATED_SERVICE_NAME")
                            .description("UPDATED_SERVICE_DESC")
                            .artifactType(WAR)
                            .appContainer(anAppContainer().withUuid("UPDATED_APP_CONTAINER_ID").build())
                            .build();
      ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
      when(executorService.submit(runnableCaptor.capture())).then(executeRunnable(runnableCaptor));

      srs.update(service);
      verify(wingsPersistence).update(any(Service.class), any(UpdateOperations.class));
      verify(wingsPersistence).createUpdateOperations(Service.class);
      verify(updateOperations).set("name", "UPDATED_SERVICE_NAME");
      verify(updateOperations).set("description", "UPDATED_SERVICE_DESC");
      verify(updateOperations)
          .set("keywords",
              asList(service.getName().toLowerCase(), service.getDescription().toLowerCase(),
                  service.getArtifactType().name().toLowerCase()));

      verify(serviceTemplateService)
          .updateDefaultServiceTemplateName(APP_ID, SERVICE_ID, SERVICE_NAME, "UPDATED_SERVICE_NAME");
      verify(wingsPersistence, times(2)).get(Service.class, APP_ID, SERVICE_ID);
    }
  }

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
        .thenReturn(aPageResponse()
                        .withResponse(asList(WorkflowBuilder.aWorkflow()
                                                 .withName(WORKFLOW_NAME)
                                                 .withServices(asList(Service.builder().uuid(SERVICE_ID).build()))
                                                 .build()))
                        .build());
    assertThatThrownBy(() -> srs.delete(APP_ID, SERVICE_ID))
        .isInstanceOf(WingsException.class)
        .hasMessage(INVALID_REQUEST.name());
    verify(wingsPersistence).get(Service.class, APP_ID, SERVICE_ID);
    verify(workflowService).listWorkflows(any(PageResponse.class));
  }

  @Test
  public void shouldCloneService() throws IOException {
    PageRequest<ServiceCommand> serviceCommandPageRequest = getServiceCommandPageRequest();

    when(wingsPersistence.saveAndGet(eq(ServiceCommand.class), any(ServiceCommand.class))).thenAnswer(invocation -> {
      ServiceCommand command = invocation.getArgumentAt(1, ServiceCommand.class);
      command.setUuid(ID_KEY);
      return command;
    });

    Graph commandGraph = getGraph();

    Command command = aCommand().withGraph(commandGraph).build();
    command.transformGraph();
    command.setVersion(1L);

    when(wingsPersistence.query(ServiceCommand.class, serviceCommandPageRequest))
        .thenReturn(aPageResponse()
                        .withResponse(asList(aServiceCommand()
                                                 .withUuid("SERVICE_COMMAND_ID")
                                                 .withDefaultVersion(1)
                                                 .withTargetToAllEnv(true)
                                                 .withName("START")
                                                 .withCommand(command)
                                                 .build()))
                        .build());

    when(commandService.getCommand(APP_ID, "SERVICE_COMMAND_ID", 1)).thenReturn(command);

    Service originalService =
        serviceBuilder.serviceCommands(asList(aServiceCommand().withUuid("SERVICE_COMMAND_ID").build())).build();
    when(wingsPersistence.get(Service.class, APP_ID, SERVICE_ID)).thenReturn(originalService);

    Service savedClonedService = originalService.cloneInternal();
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
        APP_ID, SERVICE_ID, Service.builder().name("Clone Service").description("clone description").build());

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

  private Graph getGraph() {
    return aGraph()
        .withGraphName("START")
        .addNodes(aGraphNode()
                      .withId("1")
                      .withOrigin(true)
                      .withType("EXEC")
                      .addProperty("commandPath", "/home/xxx/tomcat")
                      .addProperty("commandString", "bin/startup.sh")
                      .build())
        .build();
  }

  @Test
  public void shouldThrowExceptionOnDeleteServiceStillReferencedInWorkflow() {
    when(wingsPersistence.delete(any(), any())).thenReturn(true);
    when(workflowService.listWorkflows(any(PageRequest.class)))
        .thenReturn(aPageResponse()
                        .withResponse(asList(
                            aWorkflow().withServices(asList(Service.builder().uuid(SERVICE_ID).build())).build()))
                        .build());

    assertThatThrownBy(() -> srs.delete(APP_ID, SERVICE_ID))
        .isInstanceOf(WingsException.class)
        .hasMessage(INVALID_REQUEST.name());

    verify(wingsPersistence).get(Service.class, APP_ID, SERVICE_ID);
    verify(workflowService).listWorkflows(any(PageResponse.class));
    verify(wingsPersistence, never()).delete(Service.class, SERVICE_ID);
  }

  @Test
  public void shouldAddCommand() {
    when(wingsPersistence.saveAndGet(eq(ServiceCommand.class), any(ServiceCommand.class))).thenAnswer(invocation -> {
      ServiceCommand command = invocation.getArgumentAt(1, ServiceCommand.class);
      command.setServiceId(SERVICE_ID);
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

    Service service = serviceBuilder.build();
    service.getServiceCommands().add(aServiceCommand()
                                         .withTargetToAllEnv(true)
                                         .withAppId(APP_ID)
                                         .withServiceId(SERVICE_ID)
                                         .withUuid(ID_KEY)
                                         .withDefaultVersion(1)
                                         .withName("START")
                                         .withCommand(expectedCommand)
                                         .build());

    srs.addCommand(APP_ID, SERVICE_ID,
        aServiceCommand().withServiceId(SERVICE_ID).withTargetToAllEnv(true).withCommand(expectedCommand).build(),
        true);

    verify(wingsPersistence, times(2)).get(Service.class, APP_ID, SERVICE_ID);

    verify(wingsPersistence).save(Mockito.any(Service.class));
    verify(configService).getConfigFilesForEntity(APP_ID, DEFAULT_TEMPLATE_ID, SERVICE_ID);
    verify(wingsPersistence).saveAndGet(eq(ServiceCommand.class), any(ServiceCommand.class));
  }

  /**
   * Should add command state.
   */
  @Test
  public void shouldAddCommandWithCommandUnits() {
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
        aServiceCommand()
            .withTargetToAllEnv(true)
            .withCommand(aCommand().withCommandUnits(expectedCommand.getCommandUnits()).build())
            .build(),
        true);

    verify(wingsPersistence, times(2)).get(Service.class, APP_ID, SERVICE_ID);
    verify(wingsPersistence).save(Mockito.any(Service.class));
    verify(configService).getConfigFilesForEntity(APP_ID, DEFAULT_TEMPLATE_ID, SERVICE_ID);
    verify(wingsPersistence).saveAndGet(eq(ServiceCommand.class), any(ServiceCommand.class));
  }

  @Test
  public void shouldUpdateCommandWhenCommandChanged() {
    Command oldCommand =
        aCommand()
            .withName("START")
            .addCommandUnits(
                anExecCommandUnit().withCommandPath("/home/xxx/tomcat").withCommandString("bin/startup.sh").build())
            .build();

    oldCommand.setVersion(1L);

    PageRequest<ServiceCommand> serviceCommandPageRequest = getServiceCommandPageRequest();

    when(wingsPersistence.query(ServiceCommand.class, serviceCommandPageRequest))
        .thenReturn(aPageResponse()
                        .withResponse(asList(aServiceCommand()
                                                 .withUuid(ID_KEY)
                                                 .withTargetToAllEnv(true)
                                                 .withName("START")
                                                 .withDefaultVersion(1)
                                                 .withCommand(oldCommand)
                                                 .build()))
                        .build());

    when(wingsPersistence.createUpdateOperations(ServiceCommand.class))
        .thenReturn(datastore.createUpdateOperations(ServiceCommand.class));
    when(wingsPersistence.createQuery(ServiceCommand.class)).thenReturn(datastore.createQuery(ServiceCommand.class));

    when(wingsPersistence.get(Service.class, APP_ID, SERVICE_ID))
        .thenReturn(serviceBuilder.serviceCommands(asList(getStartCommand(oldCommand, aServiceCommand()))).build());

    when(entityVersionService.newEntityVersion(
             APP_ID, EntityType.COMMAND, ID_KEY, SERVICE_ID, "START", ChangeType.UPDATED, null))
        .thenReturn(anEntityVersion().withVersion(2).build());

    when(commandService.getCommand(APP_ID, ID_KEY, 1)).thenReturn(oldCommand);

    when(entityVersionService.lastEntityVersion(APP_ID, EntityType.COMMAND, ID_KEY, SERVICE_ID))
        .thenReturn(anEntityVersion().withVersion(1).build());

    Command expectedCommand =
        aCommand()
            .withName("START")
            .addCommandUnits(
                anExecCommandUnit().withCommandPath("/home/xxx/tomcat1").withCommandString("bin/startup.sh").build())
            .build();
    expectedCommand.setVersion(2L);

    Service updatedService = srs.updateCommand(APP_ID, SERVICE_ID,
        aServiceCommand()
            .withTargetToAllEnv(true)
            .withUuid(ID_KEY)
            .withName("START")
            .withCommand(expectedCommand)
            .build());

    verify(wingsPersistence).createUpdateOperations(ServiceCommand.class);

    verify(wingsPersistence).update(any(Query.class), any(UpdateOperations.class));

    verify(wingsPersistence).createQuery(ServiceCommand.class);

    verify(wingsPersistence, times(2)).get(Service.class, APP_ID, SERVICE_ID);

    verify(configService).getConfigFilesForEntity(APP_ID, DEFAULT_TEMPLATE_ID, SERVICE_ID);

    assertThat(updatedService).isNotNull();
  }

  /**
   * Should update command when command graph changed.
   */
  @Test
  public void shouldUpdateCommandWhenNameChanged() {
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

    when(wingsPersistence.createUpdateOperations(Command.class))
        .thenReturn(datastore.createUpdateOperations(Command.class));

    when(wingsPersistence.get(Service.class, APP_ID, SERVICE_ID))
        .thenReturn(
            serviceBuilder.serviceCommands(ImmutableList.of(getStartCommand(oldCommand, aServiceCommand()))).build());

    when(entityVersionService.newEntityVersion(
             APP_ID, EntityType.COMMAND, ID_KEY, SERVICE_ID, "START", ChangeType.UPDATED, null))
        .thenReturn(anEntityVersion().withVersion(2).build());

    when(commandService.getCommand(APP_ID, ID_KEY, 1)).thenReturn(oldCommand);

    when(entityVersionService.lastEntityVersion(APP_ID, EntityType.COMMAND, ID_KEY, SERVICE_ID))
        .thenReturn(anEntityVersion().withVersion(1).build());

    Graph commandGraph = aGraph()
                             .withGraphName("START2")
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
            .withName("START2")
            .withCommand(expectedCommand)
            .build());

    verify(wingsPersistence).createUpdateOperations(ServiceCommand.class);

    verify(wingsPersistence, times(2)).update(any(Query.class), any(UpdateOperations.class));

    verify(wingsPersistence).createQuery(ServiceCommand.class);

    verify(wingsPersistence, times(2)).get(Service.class, APP_ID, SERVICE_ID);

    verify(configService).getConfigFilesForEntity(APP_ID, DEFAULT_TEMPLATE_ID, SERVICE_ID);
  }

  @Test
  public void shouldUpdateCommandWhenCommandUnitsChanged() {
    Command oldCommand = commandBuilder.build();
    oldCommand.setVersion(1L);

    when(wingsPersistence.createUpdateOperations(ServiceCommand.class))
        .thenReturn(datastore.createUpdateOperations(ServiceCommand.class));
    when(wingsPersistence.createQuery(ServiceCommand.class)).thenReturn(datastore.createQuery(ServiceCommand.class));

    when(wingsPersistence.get(Service.class, APP_ID, SERVICE_ID))
        .thenReturn(
            serviceBuilder.serviceCommands(ImmutableList.of(getStartCommand(oldCommand, aServiceCommand()))).build());

    when(entityVersionService.newEntityVersion(
             APP_ID, EntityType.COMMAND, ID_KEY, SERVICE_ID, "START", ChangeType.UPDATED, null))
        .thenReturn(anEntityVersion().withVersion(2).build());

    when(commandService.getCommand(APP_ID, ID_KEY, 1)).thenReturn(oldCommand);

    when(entityVersionService.lastEntityVersion(APP_ID, EntityType.COMMAND, ID_KEY, SERVICE_ID))
        .thenReturn(anEntityVersion().withVersion(1).build());

    Command expectedCommand =
        aCommand()
            .withName("START")
            .addCommandUnits(
                anExecCommandUnit().withCommandPath("/home/xxx/tomcat1").withCommandString("bin/startup.sh").build())
            .build();
    expectedCommand.setVersion(2L);

    srs.updateCommand(APP_ID, SERVICE_ID,
        aServiceCommand()
            .withTargetToAllEnv(true)
            .withUuid(ID_KEY)
            .withName("START")
            .withDefaultVersion(1)
            .withCommand(expectedCommand)
            .build());

    verify(wingsPersistence).createUpdateOperations(ServiceCommand.class);

    verify(wingsPersistence).update(any(Query.class), any(UpdateOperations.class));

    verify(wingsPersistence).createQuery(ServiceCommand.class);

    verify(wingsPersistence, times(2)).get(Service.class, APP_ID, SERVICE_ID);

    verify(configService).getConfigFilesForEntity(APP_ID, DEFAULT_TEMPLATE_ID, SERVICE_ID);

    verify(commandService).save(expectedCommand, false);
  }

  /**
   * Should not update command nothing changed.
   */
  @Test
  public void shouldUpdateCommandWhenCommandUnitsOrderChanged() {
    Graph oldCommandGraph = aGraph()
                                .withGraphName("START")
                                .addNodes(aGraphNode()
                                              .withId("1")
                                              .withOrigin(true)
                                              .withName("Exec")
                                              .withType("EXEC")
                                              .addProperty("commandPath", "/home/xxx/tomcat")
                                              .addProperty("commandString", "bin/startup.sh")
                                              .build(),
                                    aGraphNode()
                                        .withId("2")
                                        .withOrigin(true)
                                        .withName("Exec2")
                                        .withType("EXEC")
                                        .addProperty("commandPath", "/home/xxx/tomcat")
                                        .addProperty("commandString", "bin/startup.sh")
                                        .build())
                                .build();

    Command oldCommand = aCommand().withGraph(oldCommandGraph).build();
    oldCommand.transformGraph();
    oldCommand.setGraph(null);
    oldCommand.setVersion(1L);

    when(wingsPersistence.createUpdateOperations(ServiceCommand.class))
        .thenReturn(datastore.createUpdateOperations(ServiceCommand.class));
    when(wingsPersistence.createQuery(ServiceCommand.class)).thenReturn(datastore.createQuery(ServiceCommand.class));

    when(wingsPersistence.get(Service.class, APP_ID, SERVICE_ID))
        .thenReturn(
            serviceBuilder.serviceCommands(ImmutableList.of(getStartCommand(oldCommand, aServiceCommand()))).build());

    when(entityVersionService.newEntityVersion(
             APP_ID, EntityType.COMMAND, ID_KEY, SERVICE_ID, "START", ChangeType.UPDATED, null))
        .thenReturn(anEntityVersion().withVersion(2).build());

    when(commandService.getCommand(APP_ID, ID_KEY, 1)).thenReturn(oldCommand);

    when(entityVersionService.lastEntityVersion(APP_ID, EntityType.COMMAND, ID_KEY, SERVICE_ID))
        .thenReturn(anEntityVersion().withVersion(1).build());

    Graph commandGraph = aGraph()
                             .withGraphName("START")

                             .addNodes(aGraphNode()
                                           .withId("2")
                                           .withOrigin(true)
                                           .withName("Exec2")
                                           .withType("EXEC")
                                           .addProperty("commandPath", "/home/xxx/tomcat")
                                           .addProperty("commandString", "bin/startup.sh")
                                           .build(),
                                 aGraphNode()
                                     .withId("1")
                                     .withOrigin(true)
                                     .withType("EXEC")
                                     .withName("Exec")
                                     .addProperty("commandPath", "/home/xxx/tomcat")
                                     .addProperty("commandString", "bin/startup.sh")
                                     .build())
                             .build();

    Command expectedCommand = aCommand().withGraph(commandGraph).build();
    expectedCommand.transformGraph();
    expectedCommand.setGraph(null);
    expectedCommand.setVersion(2L);

    srs.updateCommand(APP_ID, SERVICE_ID,
        aServiceCommand()
            .withTargetToAllEnv(true)
            .withUuid(ID_KEY)
            .withName("START")
            .withDefaultVersion(1)
            .withCommand(expectedCommand)
            .build());

    verify(wingsPersistence).createUpdateOperations(ServiceCommand.class);

    verify(wingsPersistence).update(any(Query.class), any(UpdateOperations.class));

    verify(wingsPersistence).createQuery(ServiceCommand.class);

    verify(wingsPersistence, times(2)).get(Service.class, APP_ID, SERVICE_ID);

    verify(configService).getConfigFilesForEntity(APP_ID, DEFAULT_TEMPLATE_ID, SERVICE_ID);

    verify(commandService).save(expectedCommand, false);
  }

  /**
   * Should not update command nothing changed.
   */
  @Test
  public void shouldNotUpdateCommandNothingChanged() {
    Graph oldCommandGraph = getGraph();

    Command oldCommand = aCommand().withGraph(oldCommandGraph).build();
    oldCommand.transformGraph();
    oldCommand.setVersion(1L);

    when(wingsPersistence.createUpdateOperations(ServiceCommand.class))
        .thenReturn(datastore.createUpdateOperations(ServiceCommand.class));
    when(wingsPersistence.createQuery(ServiceCommand.class)).thenReturn(datastore.createQuery(ServiceCommand.class));

    when(wingsPersistence.get(Service.class, APP_ID, SERVICE_ID))
        .thenReturn(
            serviceBuilder.serviceCommands(ImmutableList.of(getStartCommand(oldCommand, aServiceCommand()))).build());

    when(entityVersionService.newEntityVersion(
             APP_ID, EntityType.COMMAND, ID_KEY, SERVICE_ID, "START", ChangeType.UPDATED, null))
        .thenReturn(anEntityVersion().withVersion(2).build());

    when(commandService.getCommand(APP_ID, ID_KEY, 1)).thenReturn(oldCommand);

    when(entityVersionService.lastEntityVersion(APP_ID, EntityType.COMMAND, ID_KEY, SERVICE_ID))
        .thenReturn(anEntityVersion().withVersion(1).build());

    Graph commandGraph = getGraph();

    Command expectedCommand = aCommand().withGraph(commandGraph).build();
    expectedCommand.transformGraph();
    expectedCommand.setVersion(2L);

    srs.updateCommand(APP_ID, SERVICE_ID,
        aServiceCommand()
            .withTargetToAllEnv(true)
            .withUuid(ID_KEY)
            .withName("START")
            .withDefaultVersion(1)
            .withCommand(expectedCommand)
            .build());

    verify(wingsPersistence, times(2)).get(Service.class, APP_ID, SERVICE_ID);

    verify(wingsPersistence).createUpdateOperations(ServiceCommand.class);

    verify(wingsPersistence).update(any(Query.class), any(UpdateOperations.class));

    verify(wingsPersistence).createQuery(ServiceCommand.class);

    verify(commandService, never()).save(any(Command.class), eq(true));

    verify(configService).getConfigFilesForEntity(APP_ID, DEFAULT_TEMPLATE_ID, SERVICE_ID);
  }

  /**
   * Should not update command nothing changed.
   */
  @Test
  public void shouldNotUpdateCommandWhenCommandUnitsOrderNotChanged() {
    Graph oldCommandGraph = aGraph()
                                .withGraphName("START")
                                .addNodes(aGraphNode()
                                              .withId("1")
                                              .withOrigin(true)
                                              .withName("EXEC")
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
        .thenReturn(
            serviceBuilder.serviceCommands(ImmutableList.of(getStartCommand(oldCommand, aServiceCommand()))).build());

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
                                           .withName("EXEC")
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
            .withDefaultVersion(1)
            .withCommand(expectedCommand)
            .build());

    verify(wingsPersistence, times(2)).get(Service.class, APP_ID, SERVICE_ID);

    verify(wingsPersistence).createUpdateOperations(ServiceCommand.class);

    verify(wingsPersistence).update(any(Query.class), any(UpdateOperations.class));

    verify(wingsPersistence).createQuery(ServiceCommand.class);

    verify(commandService, never()).save(any(Command.class), eq(true));

    verify(configService).getConfigFilesForEntity(APP_ID, DEFAULT_TEMPLATE_ID, SERVICE_ID);
  }
  /**
   * Should not update command nothing changed.
   */
  @Test
  public void shouldNotUpdateVersionWhenNothingChanged() {
    Graph oldCommandGraph = getGraph();

    Command oldCommand = aCommand().withGraph(oldCommandGraph).build();
    oldCommand.transformGraph();
    oldCommand.setVersion(1L);

    when(wingsPersistence.createUpdateOperations(ServiceCommand.class))
        .thenReturn(datastore.createUpdateOperations(ServiceCommand.class));
    when(wingsPersistence.createQuery(ServiceCommand.class)).thenReturn(datastore.createQuery(ServiceCommand.class));

    when(wingsPersistence.get(Service.class, APP_ID, SERVICE_ID))
        .thenReturn(serviceBuilder
                        .serviceCommands(ImmutableList.of(aServiceCommand()
                                                              .withName("START")
                                                              .withUuid(ID_KEY)
                                                              .withAppId(APP_ID)
                                                              .withServiceId(SERVICE_ID)
                                                              .withDefaultVersion(1)
                                                              .withCommand(oldCommand)
                                                              .build()))
                        .build());

    when(entityVersionService.newEntityVersion(
             APP_ID, EntityType.COMMAND, ID_KEY, SERVICE_ID, "START", ChangeType.UPDATED, null))
        .thenReturn(anEntityVersion().withVersion(2).build());

    when(commandService.getCommand(APP_ID, ID_KEY, 1)).thenReturn(oldCommand);

    when(entityVersionService.lastEntityVersion(APP_ID, EntityType.COMMAND, ID_KEY, SERVICE_ID))
        .thenReturn(anEntityVersion().withVersion(1).build());

    Graph commandGraph = getGraph();

    Command expectedCommand = aCommand().withGraph(commandGraph).build();
    expectedCommand.transformGraph();
    expectedCommand.setVersion(2L);

    Service updatedService = srs.updateCommand(APP_ID, SERVICE_ID,
        aServiceCommand()
            .withTargetToAllEnv(true)
            .withDefaultVersion(1)
            .withUuid(ID_KEY)
            .withName("START")
            .withCommand(expectedCommand)
            .build());

    assertThat(updatedService).isNotNull();
    assertThat(updatedService.getServiceCommands()).isNotEmpty();
    assertThat(updatedService.getServiceCommands()).extracting("defaultVersion").contains(1);
    verify(wingsPersistence, times(2)).get(Service.class, APP_ID, SERVICE_ID);

    verify(wingsPersistence).createUpdateOperations(ServiceCommand.class);

    verify(wingsPersistence).update(any(Query.class), any(UpdateOperations.class));

    verify(wingsPersistence).createQuery(ServiceCommand.class);

    verify(commandService, never()).save(any(Command.class), eq(true));

    verify(configService).getConfigFilesForEntity(APP_ID, DEFAULT_TEMPLATE_ID, SERVICE_ID);
  }

  /**
   * Should not update command nothing changed.
   */
  @Test
  public void shouldUpdateCommandNameAndTypeChanged() {
    Graph oldCommandGraph = getGraph();

    Command oldCommand = aCommand().withGraph(oldCommandGraph).build();
    oldCommand.transformGraph();
    oldCommand.setVersion(1L);
    oldCommand.setGraph(null);
    oldCommand.setName("START");
    oldCommand.setCommandType(CommandType.START);

    when(wingsPersistence.createUpdateOperations(ServiceCommand.class))
        .thenReturn(datastore.createUpdateOperations(ServiceCommand.class));
    when(wingsPersistence.createQuery(ServiceCommand.class)).thenReturn(datastore.createQuery(ServiceCommand.class));

    when(wingsPersistence.createUpdateOperations(Command.class))
        .thenReturn(datastore.createUpdateOperations(Command.class));
    when(wingsPersistence.createQuery(Command.class)).thenReturn(datastore.createQuery(Command.class));

    when(wingsPersistence.get(Service.class, APP_ID, SERVICE_ID))
        .thenReturn(
            serviceBuilder
                .serviceCommands(ImmutableList.of(getStartCommand(oldCommand, aServiceCommand().withName("START"))))
                .build());

    when(entityVersionService.newEntityVersion(
             APP_ID, EntityType.COMMAND, ID_KEY, SERVICE_ID, "START", ChangeType.UPDATED, null))
        .thenReturn(anEntityVersion().withVersion(2).build());

    when(commandService.getCommand(APP_ID, ID_KEY, 1)).thenReturn(oldCommand);

    when(entityVersionService.lastEntityVersion(APP_ID, EntityType.COMMAND, ID_KEY, SERVICE_ID))
        .thenReturn(anEntityVersion().withVersion(1).build());

    Graph commandGraph = getGraph();

    Command expectedCommand = aCommand().withGraph(commandGraph).build();
    expectedCommand.transformGraph();
    expectedCommand.setVersion(2L);
    expectedCommand.setGraph(null);
    expectedCommand.setName("STOP");
    expectedCommand.setCommandType(CommandType.STOP);

    srs.updateCommand(APP_ID, SERVICE_ID,
        aServiceCommand()
            .withTargetToAllEnv(true)
            .withUuid(ID_KEY)
            .withName("STOP")
            .withDefaultVersion(1)
            .withCommand(expectedCommand)
            .build());

    verify(wingsPersistence, times(2)).get(Service.class, APP_ID, SERVICE_ID);

    verify(wingsPersistence).createUpdateOperations(ServiceCommand.class);

    verify(wingsPersistence).createUpdateOperations(Command.class);

    verify(wingsPersistence, times(2)).update(any(Query.class), any(UpdateOperations.class));

    verify(wingsPersistence).createQuery(ServiceCommand.class);

    verify(commandService, never()).save(any(Command.class), eq(true));

    verify(configService).getConfigFilesForEntity(APP_ID, DEFAULT_TEMPLATE_ID, SERVICE_ID);
  }

  private ServiceCommand getStartCommand(Command oldCommand, ServiceCommand.Builder start) {
    return start.withTargetToAllEnv(true)
        .withUuid(ID_KEY)
        .withAppId(APP_ID)
        .withServiceId(SERVICE_ID)
        .withDefaultVersion(1)
        .withCommand(oldCommand)
        .build();
  }

  @Test
  public void shouldUpdateCommandsOrder() {
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
    oldCommand.setGraph(null);

    Graph commandGraph = aGraph()
                             .withGraphName("START")
                             .addNodes(aGraphNode()
                                           .withId("1")
                                           .withOrigin(true)
                                           .withType("EXEC")
                                           .withRollback(false)
                                           .addProperty("commandPath", "/home/xxx/tomcat")
                                           .addProperty("commandString", "bin/startup2.sh")
                                           .build())
                             .build();

    Command expectedCommand = aCommand().withGraph(commandGraph).build();
    expectedCommand.transformGraph();
    expectedCommand.setVersion(2L);
    expectedCommand.setGraph(null);

    when(wingsPersistence.createUpdateOperations(ServiceCommand.class))
        .thenReturn(datastore.createUpdateOperations(ServiceCommand.class));
    when(wingsPersistence.createQuery(ServiceCommand.class)).thenReturn(datastore.createQuery(ServiceCommand.class));

    List<ServiceCommand> serviceCommands = asList(aServiceCommand()
                                                      .withTargetToAllEnv(true)
                                                      .withUuid(ID_KEY)
                                                      .withName("EXEC")
                                                      .withAppId(APP_ID)
                                                      .withServiceId(SERVICE_ID)
                                                      .withDefaultVersion(1)
                                                      .withCommand(oldCommand)
                                                      .build(),
        aServiceCommand()
            .withTargetToAllEnv(true)
            .withUuid(ID_KEY)
            .withName("START")
            .withAppId(APP_ID)
            .withServiceId(SERVICE_ID)
            .withDefaultVersion(1)
            .withCommand(aCommand().withCommandUnits(expectedCommand.getCommandUnits()).build())
            .build());

    PageRequest<ServiceCommand> serviceCommandPageRequest = getServiceCommandPageRequest();

    when(wingsPersistence.query(ServiceCommand.class, serviceCommandPageRequest))
        .thenReturn(aPageResponse().withResponse(serviceCommands).build());

    Service service = Service.builder().uuid(SERVICE_ID).appId(APP_ID).serviceCommands(serviceCommands).build();
    when(wingsPersistence.get(Service.class, APP_ID, SERVICE_ID)).thenReturn(service);

    when(entityVersionService.newEntityVersion(
             APP_ID, EntityType.COMMAND, ID_KEY, SERVICE_ID, "START", ChangeType.UPDATED, null))
        .thenReturn(anEntityVersion().withVersion(2).build());

    when(commandService.getCommand(APP_ID, ID_KEY, 1)).thenReturn(oldCommand);

    when(entityVersionService.lastEntityVersion(APP_ID, EntityType.COMMAND, ID_KEY, SERVICE_ID))
        .thenReturn(anEntityVersion().withVersion(1).build());

    service = srs.updateCommandsOrder(APP_ID, SERVICE_ID, serviceCommands);

    verify(wingsPersistence).createUpdateOperations(ServiceCommand.class);

    verify(wingsPersistence, times(2)).createQuery(ServiceCommand.class);

    verify(wingsPersistence, times(2)).get(Service.class, APP_ID, SERVICE_ID);

    verify(configService).getConfigFilesForEntity(APP_ID, DEFAULT_TEMPLATE_ID, SERVICE_ID);

    assertThat(service.getServiceCommands()).extracting(ServiceCommand::getName).containsSequence("EXEC", "START");
    assertThat(service.getServiceCommands()).extracting(ServiceCommand::getOrder).containsSequence(0.0, 0.0);

    serviceCommands = asList(aServiceCommand()
                                 .withTargetToAllEnv(true)
                                 .withUuid(ID_KEY)
                                 .withName("START")
                                 .withAppId(APP_ID)
                                 .withServiceId(SERVICE_ID)
                                 .withDefaultVersion(1)
                                 .withCommand(aCommand().withCommandUnits(expectedCommand.getCommandUnits()).build())
                                 .build(),
        aServiceCommand()
            .withTargetToAllEnv(true)
            .withUuid(ID_KEY)
            .withName("EXEC")
            .withAppId(APP_ID)
            .withServiceId(SERVICE_ID)
            .withDefaultVersion(1)
            .withCommand(oldCommand)
            .build());

    service = srs.updateCommandsOrder(APP_ID, SERVICE_ID, serviceCommands);

    assertThat(service.getServiceCommands()).extracting(ServiceCommand::getName).containsSequence("EXEC", "START");

    verify(wingsPersistence, times(4)).update(any(Query.class), any(UpdateOperations.class));
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
                                .withServices(asList(Service.builder()
                                                         .uuid(SERVICE_ID)
                                                         .appId(APP_ID)
                                                         .serviceCommands(asList(serviceCommand))
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
        .hasMessage(INVALID_REQUEST.name());
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
                                .withServices(asList(Service.builder()
                                                         .uuid(SERVICE_ID)
                                                         .appId(APP_ID)
                                                         .serviceCommands(asList(serviceCommand))
                                                         .build()))
                                .withOrchestrationWorkflow(
                                    aCanaryOrchestrationWorkflow()
                                        .withWorkflowPhases(asList(
                                            aWorkflowPhase()
                                                .withServiceId(SERVICE_ID_CHANGED)
                                                .addPhaseStep(aPhaseStep(PhaseStepType.STOP_SERVICE, "Phase 1")
                                                                  .addStep(aGraphNode()
                                                                               .withType("SSH")
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
        .thenReturn(serviceBuilder
                        .serviceCommands(ImmutableList.of(aServiceCommand()
                                                              .withTargetToAllEnv(true)
                                                              .withName("START")
                                                              .withDefaultVersion(1)
                                                              .withCommand(commandBuilder.build())
                                                              .build(),
                            aServiceCommand()
                                .withDefaultVersion(1)
                                .withTargetToAllEnv(true)
                                .withName("START2")
                                .withCommand(commandBuilder.but().withName("START2").build())
                                .build()))
                        .build());

    PageRequest<ServiceCommand> serviceCommandPageRequest = getServiceCommandPageRequest();

    when(wingsPersistence.query(ServiceCommand.class, serviceCommandPageRequest))
        .thenReturn(getServiceCommandPageResponse());

    List<Stencil> commandStencils = srs.getCommandStencils(APP_ID, SERVICE_ID, null);

    assertThat(commandStencils)
        .isNotNull()
        .hasSize(values().length + 1)
        .extracting(Stencil::getName)
        .contains("START", "START2");

    verify(wingsPersistence, times(2)).get(Service.class, APP_ID, SERVICE_ID);
  }

  private PageResponse getServiceCommandPageResponse() {
    return aPageResponse()
        .withResponse(asList(aServiceCommand()
                                 .withTargetToAllEnv(false)
                                 .withName("START")
                                 .withDefaultVersion(1)
                                 .withCommand(commandBuilder.build())
                                 .build(),
            aServiceCommand()
                .withTargetToAllEnv(true)
                .withName("START2")
                .withDefaultVersion(1)
                .withCommand(commandBuilder.but().withName("START2").build())
                .build()))
        .build();
  }

  /**
   * Should get command stencils.
   */
  @Test
  public void shouldGetScriptCommandStencilsOnly() {
    when(wingsPersistence.get(eq(Service.class), anyString(), anyString()))
        .thenReturn(serviceBuilder
                        .serviceCommands(ImmutableList.of(aServiceCommand()
                                                              .withTargetToAllEnv(true)
                                                              .withName("START")
                                                              .withDefaultVersion(1)
                                                              .withCommand(commandBuilder.build())
                                                              .build(),
                            aServiceCommand()
                                .withDefaultVersion(1)
                                .withTargetToAllEnv(true)
                                .withName("START2")
                                .withCommand(commandBuilder.but().withName("START2").build())
                                .build()))
                        .build());

    PageRequest<ServiceCommand> serviceCommandPageRequest = getServiceCommandPageRequest();

    when(wingsPersistence.query(ServiceCommand.class, serviceCommandPageRequest))
        .thenReturn(getServiceCommandPageResponse());

    List<Stencil> commandStencils = srs.getCommandStencils(APP_ID, SERVICE_ID, null, true);

    assertThat(commandStencils).isNotNull().extracting(Stencil::getName).contains("START", "START2");

    assertThat(commandStencils)
        .isNotNull()
        .extracting(Stencil::getTypeClass)
        .isNotOfAnyClassIn(CodeDeployCommandUnit.class, AwsLambdaCommandUnit.class, AmiCommandUnit.class);

    assertThat(commandStencils).isNotNull().extracting(Stencil::getStencilCategory).doesNotContain(CONTAINERS);

    verify(wingsPersistence, times(2)).get(Service.class, APP_ID, SERVICE_ID);
  }

  /**
   * Should get command categories
   */
  @Test
  public void shouldGetCommandCategories() {
    when(wingsPersistence.createQuery(ServiceCommand.class)).thenReturn(serviceCommandQuery);
    when(serviceCommandQuery.project("name", true)).thenReturn(serviceCommandQuery);
    when(serviceCommandQuery.filter("appId", APP_ID)).thenReturn(serviceCommandQuery);
    when(serviceCommandQuery.filter("serviceId", SERVICE_ID)).thenReturn(serviceCommandQuery);

    when(serviceCommandQuery.asList())
        .thenReturn(asList(aServiceCommand()
                               .withTargetToAllEnv(false)
                               .withName("START")
                               .withDefaultVersion(1)
                               .withCommand(commandBuilder.build())
                               .build(),
            aServiceCommand()
                .withTargetToAllEnv(true)
                .withName("START2")
                .withDefaultVersion(1)
                .withCommand(commandBuilder.but().withName("START2").build())
                .build()));

    List<CommandCategory> commandCategories = srs.getCommandCategories(APP_ID, SERVICE_ID, "MyCommand");
    assertCommandCategories(commandCategories);
  }

  /**
   * Should get command by name.
   */
  @Test
  public void shouldGetCommandByName() {
    when(wingsPersistence.get(eq(Service.class), anyString(), anyString()))
        .thenReturn(serviceBuilder
                        .serviceCommands(ImmutableList.of(aServiceCommand()
                                                              .withTargetToAllEnv(true)
                                                              .withName("START")
                                                              .withDefaultVersion(1)
                                                              .withCommand(commandBuilder.build())
                                                              .build()))
                        .build());

    assertThat(srs.getCommandByName(APP_ID, SERVICE_ID, "START")).isNotNull();

    verify(wingsPersistence, times(1)).get(Service.class, APP_ID, SERVICE_ID);
  }

  @Test
  public void shouldGetCommandByNameAndEnv() {
    when(wingsPersistence.get(eq(Service.class), anyString(), anyString()))
        .thenReturn(serviceBuilder
                        .serviceCommands(ImmutableList.of(aServiceCommand()
                                                              .withTargetToAllEnv(true)
                                                              .withName("START")
                                                              .withDefaultVersion(1)
                                                              .withCommand(commandBuilder.build())
                                                              .build()))
                        .build());

    assertThat(srs.getCommandByName(APP_ID, SERVICE_ID, ENV_ID, "START")).isNotNull();

    verify(wingsPersistence, times(1)).get(Service.class, APP_ID, SERVICE_ID);
  }

  @Test
  public void shouldGetCommandByNameAndEnvForSpecificEnv() {
    when(wingsPersistence.get(eq(Service.class), anyString(), anyString()))
        .thenReturn(serviceBuilder
                        .serviceCommands(ImmutableList.of(
                            aServiceCommand()
                                .withEnvIdVersionMap(ImmutableMap.of(ENV_ID, anEntityVersion().withVersion(2).build()))
                                .withName("START")
                                .withDefaultVersion(1)
                                .withCommand(commandBuilder.build())
                                .build()))
                        .build());

    assertThat(srs.getCommandByName(APP_ID, SERVICE_ID, ENV_ID, "START")).isNotNull();

    verify(wingsPersistence, times(1)).get(Service.class, APP_ID, SERVICE_ID);
  }

  @Test
  public void shouldGetCommandByNameAndEnvForSpecificEnvNotTargetted() {
    when(wingsPersistence.query(ServiceCommand.class, serviceCommandPageRequest))
        .thenReturn(aPageResponse()
                        .withResponse(asList(aServiceCommand()
                                                 .withTargetToAllEnv(false)
                                                 .withName("START")
                                                 .withDefaultVersion(1)
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
    LambdaSpecification lambdaSpecification = LambdaSpecification.builder()
                                                  .serviceId("TestServiceID")
                                                  .functions(asList(functionSpecification, functionSpecification))
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
                              .functions(asList(functionSpecification, functionSpecification2))
                              .build();
    lambdaSpecification.setAppId("TestAppID");
    when(wingsPersistence.saveAndGet(Mockito.any(Class.class), Mockito.any(LambdaSpecification.class)))
        .thenReturn(lambdaSpecification);

    try {
      srs.updateLambdaSpecification(lambdaSpecification);
    } catch (WingsException e) {
      fail("Should not have thrown a wingsException", e);
    }
  }

  @Test
  public void shouldUpdateContainerTaskAdvanced() {
    datastore.save(Service.builder().uuid(SERVICE_ID).appId(APP_ID).build());
    KubernetesContainerTask containerTask = new KubernetesContainerTask();
    containerTask.setAppId(APP_ID);
    containerTask.setServiceId(SERVICE_ID);
    containerTask.setUuid("TASK_ID");
    datastore.save(containerTask);
    KubernetesPayload payload = new KubernetesPayload();

    when(wingsPersistence.saveAndGet(ContainerTask.class, containerTask)).thenAnswer(t -> t.getArguments()[1]);

    payload.setAdvancedConfig("${DOCKER_IMAGE_NAME}");
    KubernetesContainerTask result =
        (KubernetesContainerTask) srs.updateContainerTaskAdvanced(APP_ID, SERVICE_ID, "TASK_ID", payload, false);
    assertThat(result.getAdvancedConfig()).isEqualTo("${DOCKER_IMAGE_NAME}");

    payload.setAdvancedConfig("a\n${DOCKER_IMAGE_NAME}");
    result = (KubernetesContainerTask) srs.updateContainerTaskAdvanced(APP_ID, SERVICE_ID, "TASK_ID", payload, false);
    assertThat(result.getAdvancedConfig()).isEqualTo("a\n${DOCKER_IMAGE_NAME}");

    payload.setAdvancedConfig("a \n${DOCKER_IMAGE_NAME}");
    result = (KubernetesContainerTask) srs.updateContainerTaskAdvanced(APP_ID, SERVICE_ID, "TASK_ID", payload, false);
    assertThat(result.getAdvancedConfig()).isEqualTo("a\n${DOCKER_IMAGE_NAME}");

    payload.setAdvancedConfig("a    \n b   \n  ${DOCKER_IMAGE_NAME}");
    result = (KubernetesContainerTask) srs.updateContainerTaskAdvanced(APP_ID, SERVICE_ID, "TASK_ID", payload, false);
    assertThat(result.getAdvancedConfig()).isEqualTo("a\n b\n  ${DOCKER_IMAGE_NAME}");

    result = (KubernetesContainerTask) srs.updateContainerTaskAdvanced(APP_ID, SERVICE_ID, "TASK_ID", null, true);
    assertThat(result.getAdvancedConfig()).isNull();
  }

  private PageRequest getServiceCommandPageRequest() {
    return aPageRequest()
        .withLimit(PageRequest.UNLIMITED)
        .addFilter("appId", EQ, APP_ID)
        .addFilter("serviceId", EQ, SERVICE_ID)
        .build();
  }

  @Test
  public void shouldCompareCommandUnits() {
    ObjectDifferBuilder.buildDefault().compare(anExecCommandUnit().build(), anExecCommandUnit().build());
    ObjectDifferBuilder.buildDefault().compare(aScpCommandUnit().build(), aScpCommandUnit().build());
    ObjectDifferBuilder.buildDefault().compare(new CleanupSshCommandUnit(), new CleanupSshCommandUnit());
    ObjectDifferBuilder.buildDefault().compare(new CopyConfigCommandUnit(), new CopyConfigCommandUnit());
    ObjectDifferBuilder.buildDefault().compare(new InitSshCommandUnit(), new InitSshCommandUnit());
  }

  @Test
  public void shouldAddCommandFromTemplate() {
    when(wingsPersistence.saveAndGet(eq(ServiceCommand.class), any(ServiceCommand.class))).thenAnswer(invocation -> {
      ServiceCommand command = invocation.getArgumentAt(1, ServiceCommand.class);
      command.setUuid(ID_KEY);
      return command;
    });

    Command expectedCommand = commandBuilder.build();

    when(templateService.constructEntityFromTemplate(TEMPLATE_ID, LATEST_TAG)).thenReturn(expectedCommand);

    srs.addCommand(APP_ID, SERVICE_ID,
        aServiceCommand()
            .withTemplateUuid(TEMPLATE_ID)
            .withTemplateVersion(LATEST_TAG)
            .withTargetToAllEnv(true)
            .withCommand(expectedCommand)
            .build(),
        true);

    verify(wingsPersistence, times(2)).get(Service.class, APP_ID, SERVICE_ID);
    verify(wingsPersistence).save(any(Service.class));
    verify(configService).getConfigFilesForEntity(APP_ID, DEFAULT_TEMPLATE_ID, SERVICE_ID);
    verify(wingsPersistence).saveAndGet(eq(ServiceCommand.class), any(ServiceCommand.class));
    verify(templateService).constructEntityFromTemplate(TEMPLATE_ID, LATEST_TAG);
  }

  @Test
  public void shouldUpdateCommandLinkedToTemplate() {
    Command oldCommand = commandBuilder.build();
    oldCommand.setVersion(1L);

    when(wingsPersistence.createUpdateOperations(ServiceCommand.class))
        .thenReturn(datastore.createUpdateOperations(ServiceCommand.class));
    when(wingsPersistence.createQuery(ServiceCommand.class)).thenReturn(datastore.createQuery(ServiceCommand.class));

    ServiceCommand serviceCommand = getTemplateServiceCommand(oldCommand);
    when(wingsPersistence.get(Service.class, APP_ID, SERVICE_ID))
        .thenReturn(serviceBuilder.serviceCommands(ImmutableList.of(serviceCommand)).build());

    when(entityVersionService.newEntityVersion(
             APP_ID, EntityType.COMMAND, ID_KEY, SERVICE_ID, "START", ChangeType.UPDATED, null))
        .thenReturn(anEntityVersion().withVersion(2).build());

    when(commandService.getCommand(APP_ID, ID_KEY, 1)).thenReturn(oldCommand);

    when(entityVersionService.lastEntityVersion(APP_ID, EntityType.COMMAND, ID_KEY, SERVICE_ID))
        .thenReturn(anEntityVersion().withVersion(1).build());

    Command expectedCommand =
        aCommand()
            .withName("START")
            .addCommandUnits(
                anExecCommandUnit().withCommandPath("/home/xxx/tomcat1").withCommandString("bin/startup.sh").build())
            .build();
    expectedCommand.setVersion(2L);
    expectedCommand.setAppId(APP_ID);

    when(commandService.getServiceCommand(APP_ID, ID_KEY)).thenReturn(serviceCommand);

    Template template = getTemplate();

    when(templateService.get(TEMPLATE_ID, "1")).thenReturn(template);

    srs.updateCommand(APP_ID, SERVICE_ID,
        aServiceCommand()
            .withTargetToAllEnv(true)
            .withUuid(ID_KEY)
            .withName("START")
            .withDefaultVersion(1)
            .withTemplateUuid(TEMPLATE_ID)
            .withTemplateVersion("1")
            .withCommand(expectedCommand)
            .build());

    verify(wingsPersistence).createUpdateOperations(ServiceCommand.class);

    verify(wingsPersistence).update(any(Query.class), any(UpdateOperations.class));

    verify(wingsPersistence).createQuery(ServiceCommand.class);

    verify(wingsPersistence, times(2)).get(Service.class, APP_ID, SERVICE_ID);

    verify(configService).getConfigFilesForEntity(APP_ID, DEFAULT_TEMPLATE_ID, SERVICE_ID);

    verify(commandService).save(Mockito.any(Command.class), Mockito.anyBoolean());
  }

  @Test
  public void shouldUpdateLinkedCommandVariables() {
    Command oldCommand = commandBuilder.build();
    oldCommand.setVersion(1L);

    when(wingsPersistence.createUpdateOperations(ServiceCommand.class))
        .thenReturn(datastore.createUpdateOperations(ServiceCommand.class));
    when(wingsPersistence.createQuery(ServiceCommand.class)).thenReturn(datastore.createQuery(ServiceCommand.class));

    ServiceCommand serviceCommand = getTemplateServiceCommand(oldCommand);
    when(wingsPersistence.get(Service.class, APP_ID, SERVICE_ID))
        .thenReturn(serviceBuilder.serviceCommands(ImmutableList.of(serviceCommand)).build());

    when(entityVersionService.newEntityVersion(
             APP_ID, EntityType.COMMAND, ID_KEY, SERVICE_ID, "START", ChangeType.UPDATED, null))
        .thenReturn(anEntityVersion().withVersion(2).build());

    when(commandService.getCommand(APP_ID, ID_KEY, 1)).thenReturn(oldCommand);

    when(entityVersionService.lastEntityVersion(APP_ID, EntityType.COMMAND, ID_KEY, SERVICE_ID))
        .thenReturn(anEntityVersion().withVersion(1).build());

    Command expectedCommand =
        aCommand()
            .withName("START")
            .withTemplateVariables(asList(aVariable().withName("MyVar").withValue("MyValue").build()))
            .addCommandUnits(
                anExecCommandUnit().withCommandPath("/home/xxx/tomcat1").withCommandString("bin/startup.sh").build())
            .build();
    expectedCommand.setVersion(2L);
    expectedCommand.setAppId(APP_ID);

    when(commandService.getServiceCommand(APP_ID, ID_KEY)).thenReturn(serviceCommand);

    Template template = getTemplate();

    when(templateService.get(TEMPLATE_ID, "1")).thenReturn(template);

    srs.updateCommand(APP_ID, SERVICE_ID,
        aServiceCommand()
            .withTargetToAllEnv(true)
            .withUuid(ID_KEY)
            .withName("START")
            .withDefaultVersion(1)
            .withTemplateUuid(TEMPLATE_ID)
            .withTemplateVersion(LATEST_TAG)
            .withCommand(expectedCommand)
            .build());

    verify(wingsPersistence).createUpdateOperations(ServiceCommand.class);

    verify(wingsPersistence).update(any(Query.class), any(UpdateOperations.class));

    verify(wingsPersistence).createQuery(ServiceCommand.class);

    verify(wingsPersistence, times(2)).get(Service.class, APP_ID, SERVICE_ID);

    verify(configService).getConfigFilesForEntity(APP_ID, DEFAULT_TEMPLATE_ID, SERVICE_ID);

    verify(commandService).save(Mockito.any(Command.class), Mockito.anyBoolean());
  }

  private ServiceCommand getTemplateServiceCommand(Command oldCommand) {
    return aServiceCommand()
        .withTargetToAllEnv(true)
        .withUuid(ID_KEY)
        .withAppId(APP_ID)
        .withServiceId(SERVICE_ID)
        .withTemplateUuid(TEMPLATE_ID)
        .withTemplateVersion(LATEST_TAG)
        .withDefaultVersion(1)
        .withCommand(oldCommand)
        .build();
  }

  private Template getTemplate() {
    SshCommandTemplate sshCommandTemplate = SshCommandTemplate.builder()
                                                .commandType(START)
                                                .commandUnits(asList(anExecCommandUnit()
                                                                         .withName("Start")
                                                                         .withCommandPath("/home/xxx/tomcat")
                                                                         .withCommandString("bin/startup.sh")
                                                                         .build()))
                                                .build();

    return Template.builder()
        .templateObject(sshCommandTemplate)
        .name("My Start Command")
        .description(TEMPLATE_DESC)
        .folderPath("Harness/Tomcat Commands/Standard")
        .keywords(asList(TEMPLATE_CUSTOM_KEYWORD))
        .gallery(HARNESS_GALLERY)
        .appId(GLOBAL_APP_ID)
        .accountId(GLOBAL_ACCOUNT_ID)
        .build();
  }

  public void assertCommandCategories(List<CommandCategory> commandCategories) {
    assertThat(commandCategories).isNotEmpty();
    assertThat(commandCategories)
        .isNotEmpty()
        .extracting(CommandCategory::getType)
        .contains(CommandCategory.Type.values());
    assertThat(commandCategories)
        .isNotEmpty()
        .extracting(CommandCategory::getDisplayName)
        .contains(
            COMMANDS.getDisplayName(), COPY.getDisplayName(), SCRIPTS.getDisplayName(), VERIFICATIONS.getDisplayName());

    List<CommandCategory> copyCategories =
        commandCategories.stream().filter(commandCategory -> commandCategory.getType().equals(COPY)).collect(toList());
    assertThat(copyCategories).extracting(CommandCategory::getCommandUnits).isNotEmpty();
    copyCategories.forEach(commandCategory -> {
      assertThat(commandCategory.getType()).isEqualTo(COPY);
      assertThat(commandCategory.getDisplayName()).isEqualTo(COPY.getDisplayName());
      assertThat(commandCategory.getCommandUnits())
          .isNotEmpty()
          .extracting(CommandCategory.CommandUnit::getType)
          .contains(COPY_CONFIGS, SCP);
    });
    List<CommandCategory> scriptCategories = commandCategories.stream()
                                                 .filter(commandCategory -> commandCategory.getType().equals(SCRIPTS))
                                                 .collect(toList());
    assertThat(scriptCategories).extracting(CommandCategory::getCommandUnits).isNotEmpty();
    scriptCategories.forEach(commandCategory -> {
      assertThat(commandCategory.getType()).isEqualTo(SCRIPTS);
      assertThat(commandCategory.getDisplayName()).isEqualTo(SCRIPTS.getDisplayName());
      assertThat(commandCategory.getCommandUnits())
          .isNotEmpty()
          .extracting(CommandCategory.CommandUnit::getType)
          .contains(EXEC, DOCKER_START, DOCKER_STOP);
    });

    List<CommandCategory> commandCommandCategories =
        commandCategories.stream()
            .filter(commandCategory -> commandCategory.getType().equals(COMMANDS))
            .collect(toList());
    assertThat(commandCommandCategories).extracting(CommandCategory::getCommandUnits).isNotEmpty();
    commandCommandCategories.forEach(commandCategory -> {
      assertThat(commandCategory.getType()).isEqualTo(COMMANDS);
      assertThat(commandCategory.getDisplayName()).isEqualTo(COMMANDS.getDisplayName());
      assertThat(commandCategory.getCommandUnits())
          .isNotEmpty()
          .extracting(CommandCategory.CommandUnit::getType)
          .contains(COMMAND, COMMAND);
      assertThat(commandCategory.getCommandUnits())
          .isNotEmpty()
          .extracting(CommandCategory.CommandUnit::getName)
          .contains("START", "START2");
    });

    List<CommandCategory> verifyCategories =
        commandCategories.stream()
            .filter(commandCategory -> commandCategory.getType().equals(VERIFICATIONS))
            .collect(toList());
    assertThat(verifyCategories).extracting(CommandCategory::getCommandUnits).isNotEmpty();
    verifyCategories.forEach(commandCategory -> {
      assertThat(commandCategory.getType()).isEqualTo(VERIFICATIONS);
      assertThat(commandCategory.getDisplayName()).isEqualTo(VERIFICATIONS.getDisplayName());
      assertThat(commandCategory.getCommandUnits())
          .isNotEmpty()
          .extracting(CommandCategory.CommandUnit::getType)
          .contains(PROCESS_CHECK_RUNNING, PORT_CHECK_CLEARED, PORT_CHECK_LISTENING);
    });
  }
}
