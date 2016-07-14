package software.wings.service;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.Environment.EnvironmentType.PROD;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_DESCRIPTION;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;

import com.google.common.collect.ImmutableMap;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import software.wings.WingsBaseTest;
import software.wings.beans.Environment;
import software.wings.beans.Orchestration;
import software.wings.beans.SearchFilter;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.ServiceTemplate.Builder;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.EnvironmentServiceImpl;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfraService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.TagService;
import software.wings.service.intfc.WorkflowService;

import javax.inject.Inject;

/**
 * Created by anubhaw on 6/28/16.
 */
public class EnvironmentServiceTest extends WingsBaseTest {
  /**
   * The Query.
   */
  @Mock Query<Environment> query;
  /**
   * The End.
   */
  @Mock FieldEnd end;
  @Mock private WingsPersistence wingsPersistence;
  @Mock private AppService appService;
  @Mock private InfraService infraService;
  @Mock private ServiceTemplateService serviceTemplateService;
  @Mock private TagService tagService;
  @Mock private WorkflowService workflowService;

  @Inject @InjectMocks private EnvironmentService environmentService;

  @Spy @InjectMocks private EnvironmentService spyEnvService = new EnvironmentServiceImpl();

  @Captor private ArgumentCaptor<Environment> environmentArgumentCaptor;

  /**
   * Sets up.
   *
   * @throws Exception the exception
   */
  @Before
  public void setUp() throws Exception {
    when(wingsPersistence.createQuery(Environment.class)).thenReturn(query);
    when(query.field(any())).thenReturn(end);
    when(end.equal(any())).thenReturn(query);
  }

  /**
   * Should list environments.
   */
  @Test
  public void shouldListEnvironments() {
    Environment environment = anEnvironment().withAppId(APP_ID).withUuid(ENV_ID).build();
    PageRequest<Environment> envPageRequest = new PageRequest<>();
    PageResponse<Environment> envPageResponse = new PageResponse<>();
    envPageResponse.setResponse(asList(environment));
    when(wingsPersistence.query(Environment.class, envPageRequest)).thenReturn(envPageResponse);

    ServiceTemplate serviceTemplate = Builder.aServiceTemplate().build();
    PageRequest<ServiceTemplate> serviceTemplatePageRequest = new PageRequest<>();
    serviceTemplatePageRequest.addFilter("appId", environment.getAppId(), SearchFilter.Operator.EQ);
    serviceTemplatePageRequest.addFilter("envId", environment.getUuid(), EQ);
    PageResponse<ServiceTemplate> serviceTemplatePageResponse = new PageResponse<>();
    serviceTemplatePageResponse.setResponse(asList(serviceTemplate));
    when(serviceTemplateService.list(serviceTemplatePageRequest)).thenReturn(serviceTemplatePageResponse);

    Orchestration orchestration =
        Orchestration.Builder.anOrchestration().withAppId(APP_ID).withEnvironment(environment).build();
    PageRequest<Orchestration> orchestrationPageRequest = new PageRequest<>();
    orchestrationPageRequest.addFilter("appId", environment.getAppId(), SearchFilter.Operator.EQ);
    orchestrationPageRequest.addFilter("environment", environment, SearchFilter.Operator.EQ);
    PageResponse<Orchestration> orchestrationPageResponse = new PageResponse<>();
    orchestrationPageResponse.setResponse(asList(orchestration));
    when(workflowService.listOrchestration(orchestrationPageRequest)).thenReturn(orchestrationPageResponse);

    PageResponse<Environment> environments = environmentService.list(envPageRequest, true);

    assertThat(environments).containsAll(asList(environment));
    assertThat(environments.get(0).getServiceTemplates()).containsAll(asList(serviceTemplate));
    assertThat(environments.get(0).getOrchestrations()).containsAll(asList(orchestration));
    verify(serviceTemplateService).list(serviceTemplatePageRequest);
    verify(workflowService).listOrchestration(orchestrationPageRequest);
  }

  /**
   * Should get environment.
   */
  @Test
  public void shouldGetEnvironment() {
    when(wingsPersistence.get(Environment.class, APP_ID, ENV_ID))
        .thenReturn(anEnvironment().withUuid(ENV_ID).withAppId(APP_ID).build());
    when(serviceTemplateService.list(any(PageRequest.class))).thenReturn(new PageResponse<ServiceTemplate>());
    when(workflowService.listOrchestration(any(PageRequest.class))).thenReturn(new PageResponse<Orchestration>());
    environmentService.get(APP_ID, ENV_ID, true);
    verify(wingsPersistence).get(Environment.class, APP_ID, ENV_ID);
  }

  /**
   * Should save environment.
   */
  @Test
  public void shouldSaveEnvironment() {
    Environment environment =
        anEnvironment().withAppId(APP_ID).withName(ENV_NAME).withDescription(ENV_DESCRIPTION).build();
    Environment savedEnvironment =
        anEnvironment().withAppId(APP_ID).withUuid(ENV_ID).withName(ENV_NAME).withDescription(ENV_DESCRIPTION).build();
    when(wingsPersistence.saveAndGet(Environment.class, environment)).thenReturn(savedEnvironment);

    environmentService.save(environment);
    verify(wingsPersistence).saveAndGet(Environment.class, environment);
    verify(appService).addEnvironment(savedEnvironment);
    verify(infraService).createDefaultInfraForEnvironment(APP_ID, ENV_ID);
    verify(tagService).createDefaultRootTagForEnvironment(savedEnvironment);
    verify(serviceTemplateService).createDefaultTemplatesByEnv(savedEnvironment);
    verify(workflowService).createWorkflow(eq(Orchestration.class), any(Orchestration.class));
  }

  /**
   * Should update environment.
   */
  @Test
  public void shouldUpdateEnvironment() {
    Environment environment = anEnvironment()
                                  .withAppId(APP_ID)
                                  .withUuid(ENV_ID)
                                  .withName(ENV_NAME)
                                  .withEnvironmentType(PROD)
                                  .withDescription(ENV_DESCRIPTION)
                                  .build();
    environmentService.update(environment);
    verify(wingsPersistence)
        .updateFields(Environment.class, ENV_ID,
            ImmutableMap.of("name", ENV_NAME, "description", ENV_DESCRIPTION, "environmentType", PROD));
    verify(wingsPersistence).get(Environment.class, APP_ID, ENV_ID);
  }

  /**
   * Should delete environment.
   */
  @Test
  public void shouldDeleteEnvironment() {
    environmentService.delete(APP_ID, ENV_ID);
    InOrder inOrder = inOrder(wingsPersistence, serviceTemplateService, tagService, infraService);
    inOrder.verify(wingsPersistence).delete(any(Query.class));
    inOrder.verify(serviceTemplateService).deleteByEnv(APP_ID, ENV_ID);
    inOrder.verify(tagService).deleteByEnv(APP_ID, ENV_ID);
    inOrder.verify(infraService).deleteByEnv(APP_ID, ENV_ID);
  }

  /**
   * Should delete by app.
   */
  @Test
  public void shouldDeleteByApp() {
    when(query.asList()).thenReturn(asList(anEnvironment().withAppId(APP_ID).withUuid(ENV_ID).build()));
    doNothing().when(spyEnvService).delete(APP_ID, ENV_ID);
    spyEnvService.deleteByApp(APP_ID);
    verify(query).field("appId");
    verify(end).equal(APP_ID);
    verify(spyEnvService).delete(APP_ID, ENV_ID);
  }

  /**
   * Should create default environments.
   */
  @Test
  public void shouldCreateDefaultEnvironments() {
    doReturn(anEnvironment().build()).when(spyEnvService).save(any(Environment.class));
    spyEnvService.createDefaultEnvironments(APP_ID);
    verify(spyEnvService, times(4)).save(environmentArgumentCaptor.capture());
    assertThat(asList("Production", "User Acceptance", "Quality Assurance", "Development"))
        .isEqualTo(environmentArgumentCaptor.getAllValues().stream().map(Environment::getName).collect(toList()));
  }
}
