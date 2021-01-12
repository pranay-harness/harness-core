package io.harness.notification.service;

import static io.harness.rule.OwnerRule.ANKUSH;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.NotificationRequest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.NotificationTaskResponse;
import io.harness.notification.beans.NotificationProcessingResponse;
import io.harness.notification.exception.NotificationException;
import io.harness.notification.remote.dto.MSTeamSettingDTO;
import io.harness.notification.remote.dto.NotificationSettingDTO;
import io.harness.notification.service.api.NotificationSettingsService;
import io.harness.notification.service.api.NotificationTemplateService;
import io.harness.notification.service.senders.MSTeamsSenderImpl;
import io.harness.rule.Owner;
import io.harness.service.DelegateGrpcClientWrapper;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class MSTeamsServiceImplTest extends CategoryTest {
  @Mock private NotificationSettingsService notificationSettingsService;
  @Mock private NotificationTemplateService notificationTemplateService;
  @Mock private MSTeamsSenderImpl msTeamsSender;
  @Mock private DelegateGrpcClientWrapper delegateGrpcClientWrapper;
  private MSTeamsServiceImpl msTeamService;
  private String accountId = "accountId";
  private String msTeamsTemplateName = "msteams_test";
  private String msTeamsWebhookurl = "msteams-webhookurl";
  private String id = "id";

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    msTeamService = new MSTeamsServiceImpl(
        notificationSettingsService, notificationTemplateService, msTeamsSender, delegateGrpcClientWrapper);
  }

  @Test
  @Owner(developers = ANKUSH)
  @Category(UnitTests.class)
  public void sendNotificaiton_EmptyRequest() {
    NotificationRequest notificationRequest = NotificationRequest.newBuilder().build();
    NotificationProcessingResponse notificationProcessingResponse = msTeamService.send(notificationRequest);
    assertTrue(notificationProcessingResponse.equals(NotificationProcessingResponse.trivialResponseWithNoRetries));
  }

  @Test
  @Owner(developers = ANKUSH)
  @Category(UnitTests.class)
  public void sendNotificaiton_OnlyIdInRequest() {
    NotificationRequest notificationRequest = NotificationRequest.newBuilder().setId(id).build();
    NotificationProcessingResponse notificationProcessingResponse = msTeamService.send(notificationRequest);
    assertTrue(notificationProcessingResponse.equals(NotificationProcessingResponse.trivialResponseWithNoRetries));
  }

  @Test
  @Owner(developers = ANKUSH)
  @Category(UnitTests.class)
  public void sendNotificaiton_NoTemplateIdInRequest() {
    NotificationRequest notificationRequest =
        NotificationRequest.newBuilder()
            .setId(id)
            .setAccountId(accountId)
            .setMsTeam(NotificationRequest.MSTeam.newBuilder()
                           .addAllMsTeamKeys(Collections.singletonList(msTeamsWebhookurl))
                           .build())
            .build();
    NotificationProcessingResponse notificationProcessingResponse = msTeamService.send(notificationRequest);
    assertTrue(notificationProcessingResponse.equals(NotificationProcessingResponse.trivialResponseWithNoRetries));
  }

  @Test
  @Owner(developers = ANKUSH)
  @Category(UnitTests.class)
  public void sendNotificaiton_NoRecipientInRequest() {
    NotificationRequest notificationRequest = NotificationRequest.newBuilder()
                                                  .setId(id)
                                                  .setAccountId(accountId)
                                                  .setMsTeam(NotificationRequest.MSTeam.newBuilder()
                                                                 .setTemplateId(msTeamsTemplateName)
                                                                 .addAllMsTeamKeys(Collections.EMPTY_LIST)
                                                                 .build())
                                                  .build();
    NotificationProcessingResponse notificationProcessingResponse = msTeamService.send(notificationRequest);
    assertTrue(notificationProcessingResponse.equals(NotificationProcessingResponse.trivialResponseWithNoRetries));
  }

  @SneakyThrows
  @Test
  @Owner(developers = ANKUSH)
  @Category(UnitTests.class)
  public void sendNotificaiton_ValidCase() {
    NotificationRequest notificationRequest =
        NotificationRequest.newBuilder()
            .setId(id)
            .setAccountId(accountId)
            .setMsTeam(NotificationRequest.MSTeam.newBuilder()
                           .setTemplateId(msTeamsTemplateName)
                           .addAllMsTeamKeys(Collections.singletonList(msTeamsWebhookurl))
                           .build())
            .build();
    NotificationProcessingResponse notificationExpectedResponse =
        NotificationProcessingResponse.builder().result(Arrays.asList(true, false)).shouldRetry(false).build();
    when(notificationTemplateService.getTemplateAsString(eq(msTeamsTemplateName), any()))
        .thenReturn(Optional.empty(), Optional.of("This is a test notification"));
    when(notificationSettingsService.getSendNotificationViaDelegate(eq(accountId))).thenReturn(false);
    when(msTeamsSender.send(any(), any(), any())).thenReturn(notificationExpectedResponse);

    NotificationProcessingResponse notificationProcessingResponse = msTeamService.send(notificationRequest);
    assertTrue(notificationProcessingResponse.equals(NotificationProcessingResponse.trivialResponseWithNoRetries));

    notificationProcessingResponse = msTeamService.send(notificationRequest);
    assertEquals(notificationExpectedResponse, notificationProcessingResponse);

    notificationRequest = NotificationRequest.newBuilder()
                              .setId(id)
                              .setAccountId(accountId)
                              .setMsTeam(NotificationRequest.MSTeam.newBuilder()
                                             .setTemplateId(msTeamsTemplateName)
                                             .addAllMsTeamKeys(Collections.singletonList(msTeamsWebhookurl))
                                             .build())
                              .build();
    notificationExpectedResponse =
        NotificationProcessingResponse.builder().result(Arrays.asList(true, false)).shouldRetry(false).build();
    when(notificationTemplateService.getTemplateAsString(eq(msTeamsTemplateName), any()))
        .thenReturn(Optional.of("this is test notification"));
    when(notificationSettingsService.getSendNotificationViaDelegate(eq(accountId))).thenReturn(true);
    when(delegateGrpcClientWrapper.executeSyncTask(any()))
        .thenReturn(NotificationTaskResponse.builder().processingResponse(notificationExpectedResponse).build());
    notificationProcessingResponse = msTeamService.send(notificationRequest);
    assertEquals(notificationExpectedResponse, notificationProcessingResponse);
  }

  @Test
  @Owner(developers = ANKUSH)
  @Category(UnitTests.class)
  public void sendTestNotification_CheckAllGaurds() {
    final NotificationSettingDTO notificationSettingDTO1 = MSTeamSettingDTO.builder().build();
    assertThatThrownBy(() -> msTeamService.sendTestNotification(notificationSettingDTO1))
        .isInstanceOf(NotificationException.class);

    final NotificationSettingDTO notificationSettingDTO2 = MSTeamSettingDTO.builder().accountId(accountId).build();
    assertThatThrownBy(() -> msTeamService.sendTestNotification(notificationSettingDTO2))
        .isInstanceOf(NotificationException.class);

    final NotificationSettingDTO notificationSettingDTO3 =
        MSTeamSettingDTO.builder().recipient("email@harness.io").build();
    assertThatThrownBy(() -> msTeamService.sendTestNotification(notificationSettingDTO3))
        .isInstanceOf(NotificationException.class);
  }

  @SneakyThrows
  @Test
  @Owner(developers = ANKUSH)
  @Category(UnitTests.class)
  public void sendTestNotificaiton_ValidRequest() {
    final NotificationSettingDTO notificationSettingDTO4 =
        MSTeamSettingDTO.builder().accountId(accountId).recipient(msTeamsWebhookurl).build();
    NotificationProcessingResponse notificationExpectedResponse =
        NotificationProcessingResponse.builder().result(Arrays.asList(true)).shouldRetry(false).build();
    when(msTeamsSender.send(any(), any(), any())).thenReturn(notificationExpectedResponse);
    when(notificationTemplateService.getTemplateAsString(any(), any()))
        .thenReturn(Optional.of("This is a test notification"));
    boolean response = msTeamService.sendTestNotification(notificationSettingDTO4);
    assertTrue(response);
  }
}