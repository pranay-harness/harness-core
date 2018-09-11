/**
 *
 */

package software.wings.service;

import static io.harness.eraro.ErrorCode.INVALID_ARGUMENT;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.ApprovalNotification.Builder.anApprovalNotification;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.dl.HQuery.excludeAuthority;
import static software.wings.security.UserThreadLocal.userGuard;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.NOTIFICATION_ID;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.beans.Event.Type;
import software.wings.beans.Notification;
import software.wings.beans.Service;
import software.wings.beans.StringValue;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.scheduler.JobScheduler;
import software.wings.security.UserThreadLocal;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.AppContainerService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.NotificationService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.TriggerService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.instance.InstanceService;
import software.wings.service.intfc.yaml.YamlPushService;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.io.IOException;

/**
 * The type App service test.
 *
 * @author Rishi
 */
public class AppServiceTest extends WingsBaseTest {
  /**
   * The Query.
   */
  @Mock Query<Application> query;

  /**
   * The End.
   */
  @Mock FieldEnd end;
  /**
   * The Update operations.
   */
  @Mock UpdateOperations<Application> updateOperations;

  @Mock private WingsPersistence wingsPersistence;
  @Mock private YamlPushService yamlPushService;

  @Inject @InjectMocks AppService appService;

  @Mock private AlertService alertService;
  @Mock private AppContainerService appContainerService;
  @Mock private ArtifactService artifactService;
  @Mock private ArtifactStreamService artifactStreamService;
  @Mock private EnvironmentService environmentService;
  @Mock private InstanceService instanceService;
  @Mock private NotificationService notificationService;
  @Mock private PipelineService pipelineService;
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private SettingsService settingsService;
  @Mock private TriggerService triggerService;
  @Mock private WorkflowService workflowService;

  @Mock private JobScheduler jobScheduler;

  /**
   * Sets up.
   *
   * @throws Exception the exception
   */
  @Before
  public void setUp() throws Exception {
    when(wingsPersistence.createQuery(Application.class)).thenReturn(query);
    when(wingsPersistence.createQuery(Application.class, excludeAuthority)).thenReturn(query);
    when(wingsPersistence.createUpdateOperations(Application.class)).thenReturn(updateOperations);
    when(query.filter(any(), any())).thenReturn(query);
    when(updateOperations.set(any(), any())).thenReturn(updateOperations);
    when(updateOperations.addToSet(any(), any())).thenReturn(updateOperations);
    when(updateOperations.removeAll(any(), any(Service.class))).thenReturn(updateOperations);
  }

  /**
   * Should save application.
   */
  @Test
  public void shouldSaveApplication() {
    Application app =
        anApplication().withName("AppA").withAccountId(ACCOUNT_ID).withDescription("Description1").build();
    Application savedApp = anApplication()
                               .withUuid(APP_ID)
                               .withAccountId("ACCOUNT_ID")
                               .withName("AppA")
                               .withDescription("Description1")
                               .build();
    when(wingsPersistence.saveAndGet(eq(Application.class), any(Application.class))).thenReturn(savedApp);
    when(wingsPersistence.get(Application.class, APP_ID)).thenReturn(savedApp);
    when(notificationService.list(any(PageRequest.class))).thenReturn(new PageResponse<Notification>());
    when(settingsService.getGlobalSettingAttributesByType(ACCOUNT_ID, SettingVariableTypes.APP_DYNAMICS.name()))
        .thenReturn(Lists.newArrayList(aSettingAttribute().withUuid("id").build()));

    appService.save(app);
    ArgumentCaptor<Application> calledApp = ArgumentCaptor.forClass(Application.class);
    verify(wingsPersistence).saveAndGet(eq(Application.class), calledApp.capture());
    Application calledAppValue = calledApp.getValue();
    assertThat(calledAppValue)
        .isNotNull()
        .extracting("accountId", "name", "description")
        .containsExactly(app.getAccountId(), app.getName(), app.getDescription());
    assertThat(calledAppValue.getKeywords())
        .isNotNull()
        .contains(app.getName().toLowerCase(), app.getDescription().toLowerCase());

    verify(settingsService).createDefaultApplicationSettings(APP_ID, "ACCOUNT_ID", false);
    verify(notificationService).sendNotificationAsync(any(Notification.class));
    ArgumentCaptor<JobDetail> jobDetailArgumentCaptor = ArgumentCaptor.forClass(JobDetail.class);
    ArgumentCaptor<Trigger> triggerArgumentCaptor = ArgumentCaptor.forClass(Trigger.class);
    verify(jobScheduler, Mockito.times(1))
        .scheduleJob(jobDetailArgumentCaptor.capture(), triggerArgumentCaptor.capture());

    assertThat(jobDetailArgumentCaptor.getValue()).isNotNull();
    assertThat(jobDetailArgumentCaptor.getValue().getJobDataMap().getString("appId")).isEqualTo(savedApp.getUuid());
    assertThat(triggerArgumentCaptor.getValue()).isNotNull();
    assertThat(triggerArgumentCaptor.getValue().getKey().getName()).isEqualTo(savedApp.getUuid());
  }

  /**
   * Should list.
   */
  @Test
  public void shouldListApplication() {
    Application application = anApplication().build();
    PageResponse<Application> pageResponse = new PageResponse<>();
    PageRequest<Application> pageRequest = new PageRequest<>();
    pageResponse.setResponse(asList(application));
    when(wingsPersistence.query(Application.class, pageRequest)).thenReturn(pageResponse);
    PageResponse<Application> applications = appService.list(pageRequest);
    assertThat(applications).containsAll(asList(application));
  }

  @Test
  public void shouldListApplicationWithDetails() {
    Application application = anApplication().build();
    PageResponse<Application> pageResponse = new PageResponse<>();
    PageRequest<Application> pageRequest = new PageRequest<>();
    pageResponse.setResponse(asList(application));
    when(wingsPersistence.query(Application.class, pageRequest)).thenReturn(pageResponse);
    PageResponse<Application> applications = appService.list(pageRequest);
    assertThat(applications).containsAll(asList(application));
  }

  /**
   * Should get application.
   */
  @Test
  public void shouldGetApplicationWithDetails() {
    PageResponse<Notification> notificationPageResponse = new PageResponse<>();
    notificationPageResponse.add(anApprovalNotification().withAppId(APP_ID).withUuid(NOTIFICATION_ID).build());
    when(wingsPersistence.get(Application.class, APP_ID)).thenReturn(anApplication().withUuid(APP_ID).build());
    Application application = appService.get(APP_ID, true);
    verify(wingsPersistence).get(Application.class, APP_ID);
    assertThat(application).isNotNull();
  }

  /**
   * Should get application.
   */
  @Test
  public void shouldGetApplicationWithDefaults() {
    when(wingsPersistence.get(Application.class, APP_ID))
        .thenReturn(anApplication()
                        .withUuid(APP_ID)
                        .withAccountId(ACCOUNT_ID)
                        .withDefaults(ImmutableMap.of("Param1", "Value1"))
                        .build());
    when(settingsService.listApplicationDefaults(ACCOUNT_ID, APP_ID))
        .thenReturn(asList(aSettingAttribute()
                               .withName("NAME")
                               .withAccountId("ACCOUNT_ID")
                               .withValue(StringValue.Builder.aStringValue().build())
                               .build(),
            aSettingAttribute()
                .withName("NAME2")
                .withAccountId("ACCOUNT_ID")
                .withValue(StringValue.Builder.aStringValue().withValue("VALUE").build())
                .build()));

    Application application = appService.getApplicationWithDefaults(APP_ID);
    assertThat(application).isNotNull();
    assertThat(application.getDefaults()).isNotEmpty().containsKeys("NAME", "NAME2");
    assertThat(application.getDefaults()).isNotEmpty().containsValues("", "VALUE");
    verify(wingsPersistence).get(Application.class, APP_ID);
    verify(settingsService).listApplicationDefaults(ACCOUNT_ID, APP_ID);
  }

  @Test
  public void shouldReturnTrueForExistingApplicationInExistApi() {
    when(query.getKey()).thenReturn(new Key<>(Application.class, "applications", APP_ID));
    assertThat(appService.exist(APP_ID)).isTrue();
    verify(query).filter(ID_KEY, APP_ID);
  }

  @Test
  public void shouldThrowExceptionForNonExistentApplicationGet() {
    assertThatThrownBy(() -> appService.get("NON_EXISTENT_APP_ID"))
        .isInstanceOf(WingsException.class)
        .hasMessage(INVALID_ARGUMENT.name());
  }

  @Test
  public void shouldNotThrowExceptionForNonExistentApplicationDelete() {
    appService.delete("NON_EXISTENT_APP_ID");
  }

  /**
   * Should update.
   */
  @Test
  public void shouldUpdateApplication() throws IOException {
    try (UserThreadLocal.Guard guard = userGuard(null)) {
      Application application = anApplication().withUuid(APP_ID).withName(APP_NAME).withAccountId(ACCOUNT_ID).build();
      when(wingsPersistence.get(Application.class, APP_ID)).thenReturn(application);
      appService.update(anApplication()
                            .withUuid(APP_ID)
                            .withName("App_Name")
                            .withDescription("Description")
                            .withAccountId(ACCOUNT_ID)
                            .build());
      verify(query).filter(ID_KEY, APP_ID);
      verify(updateOperations).set("name", "App_Name");
      verify(updateOperations).set("description", "Description");
      verify(updateOperations).set("keywords", asList("App_Name".toLowerCase(), "Description".toLowerCase()));
      verify(wingsPersistence).update(query, updateOperations);
      verify(wingsPersistence, times(2)).get(Application.class, APP_ID);
      verify(yamlPushService).pushYamlChangeSet(ACCOUNT_ID, application, application, Type.UPDATE, false, true);
    }
  }

  /**
   * Should delete.
   */
  @Test
  public void shouldDeleteApplication() {
    when(wingsPersistence.delete(any(), any())).thenReturn(true);
    Application application = anApplication().withUuid(APP_ID).withName("APP_NAME").build();
    when(wingsPersistence.get(Application.class, APP_ID)).thenReturn(application);
    appService.delete(APP_ID);
    InOrder inOrder = inOrder(wingsPersistence, notificationService, serviceResourceService, environmentService,
        appContainerService, artifactService, artifactStreamService, instanceService, workflowService, pipelineService,
        alertService, triggerService);
    inOrder.verify(wingsPersistence).delete(Application.class, APP_ID);
    inOrder.verify(notificationService).sendNotificationAsync(any(Notification.class));
  }

  @Test
  public void shouldPruneDescendingObjects() {
    when(wingsPersistence.get(Application.class, APP_ID)).thenReturn(null);
    appService.pruneDescendingEntities(APP_ID);
    InOrder inOrder = inOrder(wingsPersistence, notificationService, serviceResourceService, environmentService,
        appContainerService, artifactService, artifactStreamService, instanceService, workflowService, pipelineService,
        alertService, triggerService);

    inOrder.verify(alertService).pruneByApplication(APP_ID);
    inOrder.verify(environmentService).pruneByApplication(APP_ID);
    inOrder.verify(instanceService).pruneByApplication(APP_ID);
    inOrder.verify(notificationService).pruneByApplication(APP_ID);
    inOrder.verify(pipelineService).pruneByApplication(APP_ID);
    inOrder.verify(serviceResourceService).pruneByApplication(APP_ID);
    inOrder.verify(triggerService).pruneByApplication(APP_ID);
    inOrder.verify(workflowService).pruneByApplication(APP_ID);
  }

  @Test
  public void shouldPruneDescendingObjectSomeFailed() {
    when(wingsPersistence.get(Application.class, APP_ID)).thenReturn(null);
    doThrow(new WingsException("Forced exception")).when(pipelineService).pruneByApplication(APP_ID);

    assertThatThrownBy(() -> appService.pruneDescendingEntities(APP_ID)).isInstanceOf(WingsException.class);

    InOrder inOrder = inOrder(wingsPersistence, notificationService, serviceResourceService, environmentService,
        appContainerService, artifactService, artifactStreamService, instanceService, workflowService, pipelineService,
        alertService, triggerService);

    inOrder.verify(alertService).pruneByApplication(APP_ID);
    inOrder.verify(environmentService).pruneByApplication(APP_ID);
    inOrder.verify(instanceService).pruneByApplication(APP_ID);
    inOrder.verify(notificationService).pruneByApplication(APP_ID);
    inOrder.verify(pipelineService).pruneByApplication(APP_ID);
    inOrder.verify(serviceResourceService).pruneByApplication(APP_ID);
    inOrder.verify(triggerService).pruneByApplication(APP_ID);
    inOrder.verify(workflowService).pruneByApplication(APP_ID);
  }
}
