package software.wings.service;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.ApprovalNotification.Builder.anApprovalNotification;
import static software.wings.beans.EntityType.ARTIFACT;
import static software.wings.beans.InformationNotification.Builder.anInformationNotification;
import static software.wings.beans.NotificationAction.NotificationActionType.APPROVE;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.ARTIFACT_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_NAME;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.NOTIFICATION_ID;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Injector;

import io.harness.beans.PageRequest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import software.wings.WingsBaseTest;
import software.wings.beans.ApprovalNotification;
import software.wings.beans.InformationNotification;
import software.wings.beans.Notification;
import software.wings.common.NotificationMessageResolver.NotificationMessageType;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.NotificationServiceImpl;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.NotificationDispatcherService;
import software.wings.service.intfc.NotificationService;

/**
 * Created by anubhaw on 7/28/16.
 */
public class NotificationServiceTest extends WingsBaseTest {
  @Mock private WingsPersistence wingsPersistence;
  @Mock private Injector injector;
  @Mock private AppService appService;
  @Mock private NotificationDispatcherService notificationDispatcherService;

  @Inject @InjectMocks private NotificationService notificationService;

  @Spy @InjectMocks private NotificationService spyNotificationService = new NotificationServiceImpl();

  /**
   * Sets up.
   *
   * @throws Exception the exception
   */
  @Before
  public void setUp() throws Exception {
    when(appService.get(APP_ID))
        .thenReturn(anApplication().withUuid(APP_ID).withAppId(APP_ID).withAccountId("ACCOUNT_ID").build());
  }

  /**
   * Should list.
   */
  @Test
  public void shouldList() {
    PageRequest pageRequest = aPageRequest().addFilter("appId", EQ, APP_ID).build();
    notificationService.list(pageRequest);
    verify(wingsPersistence).query(Notification.class, pageRequest);
  }

  /**
   * Should get.
   */
  @Test
  public void shouldGet() {
    when(wingsPersistence.get(Notification.class, APP_ID, NOTIFICATION_ID))
        .thenReturn(anInformationNotification().withAppId(APP_ID).withUuid(NOTIFICATION_ID).build());
    Notification notification = notificationService.get(APP_ID, NOTIFICATION_ID);
    assertThat(notification)
        .isInstanceOf(Notification.class)
        .hasFieldOrPropertyWithValue("appId", APP_ID)
        .hasFieldOrPropertyWithValue("uuid", NOTIFICATION_ID);
  }

  /**
   * Should send notification async.
   */
  @Test
  public void shouldSendNotificationAsync() {
    InformationNotification notification =
        anInformationNotification()
            .withAppId(APP_ID)
            .withEnvironmentId(ENV_ID)
            .withNotificationTemplateId(NotificationMessageType.ENTITY_CREATE_NOTIFICATION.name())
            .withNotificationTemplateVariables(ImmutableMap.of("ENTITY_TYPE", "Application", "ENTITY_NAME", APP_NAME))
            .build();
    notificationService.sendNotificationAsync(notification);
    verify(wingsPersistence).saveAndGet(Notification.class, notification);
    verify(notificationDispatcherService).dispatchNotification(any(), any());
    verifyNoMoreInteractions(wingsPersistence, injector);
  }

  /**
   * Should mark notification completed.
   */
  @Test
  public void shouldMarkNotificationCompleted() {
    notificationService.markNotificationCompleted(APP_ID, NOTIFICATION_ID);
    verify(wingsPersistence).updateFields(Notification.class, NOTIFICATION_ID, ImmutableMap.of("complete", true));
  }

  /**
   * Should act.
   */
  @Test
  public void shouldAct() {
    ApprovalNotification approvalNotification = Mockito.spy(anApprovalNotification()
                                                                .withAppId(APP_ID)
                                                                .withUuid(NOTIFICATION_ID)
                                                                .withEntityId(ARTIFACT_ID)
                                                                .withEntityType(ARTIFACT)
                                                                .withEntityName(ARTIFACT_NAME)
                                                                .withArtifactStreamId(ARTIFACT_STREAM_ID)
                                                                .build());

    doReturn(approvalNotification).when(spyNotificationService).get(APP_ID, NOTIFICATION_ID);
    doNothing().when(spyNotificationService).markNotificationCompleted(APP_ID, NOTIFICATION_ID);
    doReturn(true).when(approvalNotification).performAction(APPROVE);
    spyNotificationService.act(APP_ID, NOTIFICATION_ID, APPROVE);

    verify(spyNotificationService, times(2)).get(APP_ID, NOTIFICATION_ID);
    verify(injector).injectMembers(approvalNotification);
    verify(approvalNotification).performAction(APPROVE);
    verify(spyNotificationService).markNotificationCompleted(APP_ID, NOTIFICATION_ID);
  }
}
