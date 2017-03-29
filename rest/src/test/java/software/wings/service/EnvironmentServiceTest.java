package software.wings.service;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
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
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import software.wings.WingsBaseTest;
import software.wings.beans.Environment;
import software.wings.beans.Notification;
import software.wings.beans.SearchFilter;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.ServiceTemplate.Builder;
import software.wings.common.Constants;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.EnvironmentServiceImpl;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.NotificationService;
import software.wings.service.intfc.ServiceTemplateService;

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
  @Mock private ServiceTemplateService serviceTemplateService;
  @Mock private NotificationService notificationService;

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
    when(serviceTemplateService.list(serviceTemplatePageRequest, true)).thenReturn(serviceTemplatePageResponse);

    PageResponse<Environment> environments = environmentService.list(envPageRequest, true);

    assertThat(environments).containsAll(asList(environment));
    assertThat(environments.get(0).getServiceTemplates()).containsAll(asList(serviceTemplate));
    verify(serviceTemplateService).list(serviceTemplatePageRequest, true);
  }

  /**
   * Should get environment.
   */
  @Test
  public void shouldGetEnvironment() {
    when(wingsPersistence.get(Environment.class, APP_ID, ENV_ID))
        .thenReturn(anEnvironment().withUuid(ENV_ID).withAppId(APP_ID).build());
    when(serviceTemplateService.list(any(PageRequest.class), eq(true))).thenReturn(new PageResponse<>());
    environmentService.get(APP_ID, ENV_ID, true);
    verify(wingsPersistence).get(Environment.class, APP_ID, ENV_ID);
  }

  @Test
  public void shouldReturnTrueForExistingEnvironmentInExistApi() {
    when(query.getKey()).thenReturn(new Key<>(Environment.class, "environments", ENV_ID));
    assertThat(environmentService.exist(APP_ID, ENV_ID)).isTrue();
    verify(query).field(ID_KEY);
    verify(end).equal(ENV_ID);
    verify(query).field("appId");
    verify(end).equal(APP_ID);
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
    verify(serviceTemplateService).createDefaultTemplatesByEnv(savedEnvironment);
    verify(notificationService).sendNotificationAsync(any(Notification.class));
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
    when(wingsPersistence.get(Environment.class, APP_ID, ENV_ID))
        .thenReturn(anEnvironment().withUuid(ENV_ID).withName("PROD").build());
    when(wingsPersistence.delete(any(Query.class))).thenReturn(true);
    environmentService.delete(APP_ID, ENV_ID);
    InOrder inOrder = inOrder(wingsPersistence, serviceTemplateService, notificationService);
    inOrder.verify(wingsPersistence).get(Environment.class, APP_ID, ENV_ID);
    inOrder.verify(wingsPersistence).delete(any(Query.class));
    inOrder.verify(serviceTemplateService).deleteByEnv(APP_ID, ENV_ID);
    inOrder.verify(notificationService).sendNotificationAsync(any());
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
    verify(spyEnvService, times(3)).save(environmentArgumentCaptor.capture());
    assertThat(environmentArgumentCaptor.getAllValues())
        .extracting(Environment::getName)
        .containsExactly(Constants.DEV_ENV, Constants.QA_ENV, Constants.PROD_ENV);
  }
}
