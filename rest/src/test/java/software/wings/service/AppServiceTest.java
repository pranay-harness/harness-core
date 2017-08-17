/**
 *
 */

package software.wings.service;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.ApprovalNotification.Builder.anApprovalNotification;
import static software.wings.beans.ErrorCode.INVALID_ARGUMENT;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.Setup.Builder.aSetup;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.NOTIFICATION_ID;

import com.google.common.collect.Lists;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.beans.Notification;
import software.wings.beans.Service;
import software.wings.beans.Setup.SetupStatus;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.scheduler.JobScheduler;
import software.wings.service.intfc.AppContainerService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.NotificationService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.SetupService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.settings.SettingValue.SettingVariableTypes;

import javax.inject.Inject;

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
  @Mock private SettingsService settingsService;
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private EnvironmentService environmentService;
  @Mock private AppContainerService appContainerService;
  @Mock private WorkflowExecutionService workflowExecutionService;
  @Mock private NotificationService notificationService;
  @Mock private SetupService setupService;
  @Mock private JobScheduler jobScheduler;
  @Mock private ArtifactService artifactService;
  @Mock private ArtifactStreamService artifactStreamService;
  @Mock private WorkflowService workflowService;
  @Mock private PipelineService pipelineService;

  @Inject @InjectMocks AppService appService;
  /**
   * Sets up.
   *
   * @throws Exception the exception
   */
  @Before
  public void setUp() throws Exception {
    when(wingsPersistence.createQuery(Application.class)).thenReturn(query);
    when(wingsPersistence.createUpdateOperations(Application.class)).thenReturn(updateOperations);
    when(query.field(any())).thenReturn(end);
    when(end.equal(any())).thenReturn(query);
    when(updateOperations.set(any(), any())).thenReturn(updateOperations);
    when(updateOperations.add(any(), any())).thenReturn(updateOperations);
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
    verify(wingsPersistence).saveAndGet(Application.class, app);
    verify(settingsService).createDefaultApplicationSettings(APP_ID, "ACCOUNT_ID");
    verify(notificationService).sendNotificationAsync(any(Notification.class));
    ArgumentCaptor<JobDetail> jobDetailArgumentCaptor = ArgumentCaptor.forClass(JobDetail.class);
    ArgumentCaptor<Trigger> triggerArgumentCaptor = ArgumentCaptor.forClass(Trigger.class);
    verify(jobScheduler).scheduleJob(jobDetailArgumentCaptor.capture(), triggerArgumentCaptor.capture());

    assertThat(jobDetailArgumentCaptor.getValue()).isNotNull();
    assertThat(jobDetailArgumentCaptor.getValue().getJobDataMap().getString("appId")).isEqualTo(savedApp.getUuid());
    assertThat(triggerArgumentCaptor.getValue()).isNotNull();
    assertThat(triggerArgumentCaptor.getValue().getKey().getName()).isEqualTo(savedApp.getUuid());
  }

  /**
   * Should list.
   */
  @Test
  public void shouldListApplicationWithSummary() {
    Application application = anApplication().build();
    PageResponse<Application> pageResponse = new PageResponse<>();
    PageRequest<Application> pageRequest = new PageRequest<>();
    pageResponse.setResponse(asList(application));
    when(wingsPersistence.query(Application.class, pageRequest)).thenReturn(pageResponse);
    when(workflowExecutionService.listExecutions(any(PageRequest.class), eq(false))).thenReturn(new PageResponse<>());
    PageResponse<Notification> notificationPageResponse = new PageResponse<>();
    notificationPageResponse.add(anApprovalNotification().withAppId(APP_ID).withUuid(NOTIFICATION_ID).build());
    when(notificationService.list(any(PageRequest.class))).thenReturn(notificationPageResponse);
    PageResponse<Application> applications = appService.list(pageRequest, true, 5, 0);
    assertThat(applications).containsAll(asList(application));
    assertThat(application.getRecentExecutions()).isNotNull();
    assertThat(application.getNotifications())
        .hasSize(1)
        .containsExactly(anApprovalNotification().withAppId(APP_ID).withUuid(NOTIFICATION_ID).build());
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
    PageResponse<Application> applications = appService.list(pageRequest, false, 5, 0);
    assertThat(applications).containsAll(asList(application));
  }

  /**
   * Should get application.
   */
  @Test
  public void shouldGetApplication() {
    PageResponse<Notification> notificationPageResponse = new PageResponse<>();
    notificationPageResponse.add(anApprovalNotification().withAppId(APP_ID).withUuid(NOTIFICATION_ID).build());
    when(notificationService.list(any(PageRequest.class))).thenReturn(notificationPageResponse);
    when(wingsPersistence.get(Application.class, APP_ID)).thenReturn(anApplication().withUuid(APP_ID).build());
    Application application = appService.get(APP_ID, SetupStatus.COMPLETE, true, 0);
    verify(wingsPersistence).get(Application.class, APP_ID);
    assertThat(application.getNotifications())
        .hasSize(1)
        .containsExactly(anApprovalNotification().withAppId(APP_ID).withUuid(NOTIFICATION_ID).build());
  }

  @Test
  public void shouldReturnTrueForExistingApplicationInExistApi() {
    when(query.getKey()).thenReturn(new Key<>(Application.class, "applications", APP_ID));
    assertThat(appService.exist(APP_ID)).isTrue();
    verify(query).field(ID_KEY);
    verify(end).equal(APP_ID);
  }

  @Test
  public void shouldAddSetupSuggestionForIncompleteApplicationGet() {
    PageResponse<Notification> notificationPageResponse = new PageResponse<>();
    notificationPageResponse.add(anApprovalNotification().withAppId(APP_ID).withUuid(NOTIFICATION_ID).build());
    when(notificationService.list(any(PageRequest.class))).thenReturn(notificationPageResponse);
    when(wingsPersistence.get(Application.class, APP_ID)).thenReturn(anApplication().withUuid(APP_ID).build());
    when(setupService.getApplicationSetupStatus(anApplication().withUuid(APP_ID).build())).thenReturn(aSetup().build());
    Application application = appService.get(APP_ID, SetupStatus.INCOMPLETE, false, 0);

    verify(wingsPersistence).get(Application.class, APP_ID);
    verify(setupService).getApplicationSetupStatus(anApplication().withUuid(APP_ID).build());
    assertThat(application.getSetup()).isNotNull();
  }

  @Test
  public void shouldThrowExceptionForNonExistentApplicationGet() {
    assertThatThrownBy(() -> appService.get("NON_EXISTENT_APP_ID"))
        .isInstanceOf(WingsException.class)
        .hasMessage(INVALID_ARGUMENT.name());
  }

  @Test
  public void shouldThrowExceptionForNonExistentApplicationDelete() {
    assertThatThrownBy(() -> appService.delete("NON_EXISTENT_APP_ID"))
        .isInstanceOf(WingsException.class)
        .hasMessage(INVALID_ARGUMENT.name());
  }

  /**
   * Should update.
   */
  @Test
  public void shouldUpdateApplication() {
    appService.update(anApplication().withUuid(APP_ID).withName("App_Name").withDescription("Description").build());
    verify(query).field(ID_KEY);
    verify(end).equal(APP_ID);
    verify(updateOperations).set("name", "App_Name");
    verify(updateOperations).set("description", "Description");
    verify(wingsPersistence).update(query, updateOperations);
    verify(wingsPersistence).get(Application.class, APP_ID);
  }

  /**
   * Should delete.
   */
  @Test
  public void shouldDeleteApplication() {
    when(wingsPersistence.delete(any(), any())).thenReturn(true);
    when(wingsPersistence.get(Application.class, APP_ID))
        .thenReturn(anApplication().withUuid(APP_ID).withName("APP_NAME").build());
    appService.delete(APP_ID);
    InOrder inOrder = inOrder(wingsPersistence, notificationService, serviceResourceService, environmentService,
        appContainerService, jobScheduler, artifactService, artifactStreamService, workflowService, pipelineService);
    inOrder.verify(wingsPersistence).delete(Application.class, APP_ID);
    inOrder.verify(notificationService).sendNotificationAsync(any(Notification.class));
    inOrder.verify(environmentService).deleteByApp(APP_ID);
    inOrder.verify(workflowService).deleteWorkflowByApplication(APP_ID);
    inOrder.verify(workflowService).deleteStateMachinesByApplication(APP_ID);
    inOrder.verify(pipelineService).deletePipelineByApplication(APP_ID);
    inOrder.verify(serviceResourceService).deleteByApp(APP_ID);
    inOrder.verify(jobScheduler).deleteJob(eq(APP_ID), anyString());
  }
}
