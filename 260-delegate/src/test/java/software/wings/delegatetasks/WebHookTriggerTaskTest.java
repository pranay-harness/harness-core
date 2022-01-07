/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.delegatetasks;

import static io.harness.delegate.beans.TaskData.DEFAULT_SYNC_CALL_TIMEOUT;
import static io.harness.rule.OwnerRule.INDER;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.TaskData;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.WingsBaseTest;
import software.wings.beans.trigger.WebHookTriggerResponseData;
import software.wings.beans.trigger.WebHookTriggerTask;
import software.wings.beans.trigger.WebhookSource;
import software.wings.beans.trigger.WebhookTriggerParameters;
import software.wings.service.intfc.security.EncryptionService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class WebHookTriggerTaskTest extends WingsBaseTest {
  private static final String DELEGATE_ID = "DELEGATE_ID";
  private static final String RESOURCE_FILE = "software/wings/delegatetasks/GitHubSamplePayload.json";
  private static final char[] SECRET = new char[] {'1', '2', '3', '4'};
  private static final String HASHED_PAYLOAD =
      "sha256=d9953c49e0e6fc141185ec53427170b144ecb474bf5aab4721db26faa90c4608";
  @Mock EncryptionService encryptionService;

  @InjectMocks
  WebHookTriggerTask webHookTriggerTask =
      new WebHookTriggerTask(DelegateTaskPackage.builder()
                                 .delegateId(DELEGATE_ID)
                                 .data(TaskData.builder().async(false).timeout(DEFAULT_SYNC_CALL_TIMEOUT).build())
                                 .build(),
          null, delegateTaskResponse -> {}, () -> true);

  @Before
  public void setUp() throws Exception {
    initMocks(this);
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void shouldFailIfSecretAbsentInSecretManager() throws IOException {
    WebhookTriggerParameters webhookTriggerParameters =
        createGitHubWebHookParameter(WebhookSource.GITHUB, HASHED_PAYLOAD);
    when(encryptionService.getDecryptedValue(webhookTriggerParameters.getEncryptedDataDetail(), false))
        .thenReturn(new char[] {});

    WebHookTriggerResponseData webHookTriggerResponseData = webHookTriggerTask.run(webhookTriggerParameters);
    assertThat(webHookTriggerResponseData).isNotNull();
    assertThat(webHookTriggerResponseData.getExecutionStatus()).isEqualTo(ExecutionStatus.FAILED);
    assertThat(webHookTriggerResponseData.getErrorMessage()).isEqualTo("Secret key not found in secret manager");
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testHashedPayloadMatches() throws IOException {
    WebhookTriggerParameters webhookTriggerParameters =
        createGitHubWebHookParameter(WebhookSource.GITHUB, HASHED_PAYLOAD);
    when(encryptionService.getDecryptedValue(webhookTriggerParameters.getEncryptedDataDetail(), false))
        .thenReturn(SECRET);

    WebHookTriggerResponseData webHookTriggerResponseData = webHookTriggerTask.run(webhookTriggerParameters);
    assertThat(webHookTriggerResponseData).isNotNull();
    assertThat(webHookTriggerResponseData.getExecutionStatus()).isEqualTo(ExecutionStatus.SUCCESS);
    assertThat(webHookTriggerResponseData.isWebhookAuthenticated()).isTrue();
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testHashedPayloadDoesNotMatch() throws IOException {
    WebhookTriggerParameters webhookTriggerParameters =
        createGitHubWebHookParameter(WebhookSource.GITHUB, "sha256=12345");
    when(encryptionService.getDecryptedValue(webhookTriggerParameters.getEncryptedDataDetail(), false))
        .thenReturn(SECRET);

    WebHookTriggerResponseData webHookTriggerResponseData = webHookTriggerTask.run(webhookTriggerParameters);
    assertThat(webHookTriggerResponseData).isNotNull();
    assertThat(webHookTriggerResponseData.getExecutionStatus()).isEqualTo(ExecutionStatus.SUCCESS);
    assertThat(webHookTriggerResponseData.isWebhookAuthenticated()).isFalse();
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testWebHookAuthenticationWithBitbucket() throws IOException {
    WebhookTriggerParameters webhookTriggerParameters =
        createGitHubWebHookParameter(WebhookSource.BITBUCKET, HASHED_PAYLOAD);
    when(encryptionService.getDecryptedValue(webhookTriggerParameters.getEncryptedDataDetail(), false))
        .thenReturn(SECRET);

    WebHookTriggerResponseData webHookTriggerResponseData = webHookTriggerTask.run(webhookTriggerParameters);
    assertThat(webHookTriggerResponseData).isNotNull();
    assertThat(webHookTriggerResponseData.getExecutionStatus()).isEqualTo(ExecutionStatus.FAILED);
    assertThat(webHookTriggerResponseData.getErrorMessage())
        .isEqualTo("Webhook authentication is not implemented for BITBUCKET");
  }

  private WebhookTriggerParameters createGitHubWebHookParameter(WebhookSource webhookSource, String hashedPayload)
      throws IOException {
    EncryptedDataDetail encryptedDataDetail = EncryptedDataDetail.builder().build();
    String githubPayload = IOUtils.resourceToString(RESOURCE_FILE, StandardCharsets.UTF_8, getClass().getClassLoader());
    return WebhookTriggerParameters.builder()
        .encryptedDataDetail(encryptedDataDetail)
        .eventPayload(githubPayload)
        .hashedPayload(hashedPayload)
        .webhookSource(webhookSource)
        .build();
  }
}
