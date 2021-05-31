package io.harness.signup.notification;

import static io.harness.remote.client.NGRestUtils.getResponse;

import io.harness.Team;
import io.harness.cloud.google.DownloadResult;
import io.harness.cloud.google.GoogleCloudFileService;
import io.harness.notification.remote.NotificationHTTPClient;
import io.harness.signup.SignupNotificationConfiguration;

import com.google.common.cache.CacheLoader;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

@Slf4j
public class SignupNotificationTemplateLoader extends CacheLoader<EmailType, Boolean> {
  private final GoogleCloudFileService googleCloudFileService;
  private final NotificationHTTPClient notificationHTTPClient;
  private final SignupNotificationConfiguration notificationConfiguration;

  @Inject
  SignupNotificationTemplateLoader(GoogleCloudFileService googleCloudFileService,
      NotificationHTTPClient notificationHTTPClient, SignupNotificationConfiguration notificationConfiguration) {
    this.googleCloudFileService = googleCloudFileService;
    this.notificationHTTPClient = notificationHTTPClient;
    this.notificationConfiguration = notificationConfiguration;

    try {
      googleCloudFileService.initialize(notificationConfiguration.getProjectId());
    } catch (IllegalArgumentException e) {
      log.error("Failed to initialize GCS for signup notification template", e);
    }
  }

  @Override
  public Boolean load(EmailType emailType) {
    try {
      EmailInfo emailInfo = notificationConfiguration.getTemplates().get(emailType);
      DownloadResult downloadResult =
          googleCloudFileService.downloadFile(emailInfo.getGcsFileName(), notificationConfiguration.getBucketName());
      if (downloadResult.getContent() != null) {
        // save template to notification service
        final MultipartBody.Part formData = MultipartBody.Part.createFormData(
            "file", null, RequestBody.create(MultipartBody.FORM, downloadResult.getContent()));
        getResponse(
            notificationHTTPClient.saveNotificationTemplate(formData, Team.GTM, emailInfo.getTemplateId(), true));
        return true;
      } else {
        log.error("File {} doesn't exists in bucket {}", emailInfo.getGcsFileName(),
            notificationConfiguration.getBucketName());
      }
    } catch (Exception e) {
      log.error("Failed to download/save notification template for {}", emailType.name(), e);
    }
    return false;
  }
}
