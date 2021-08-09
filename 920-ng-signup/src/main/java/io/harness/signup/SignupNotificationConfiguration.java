package io.harness.signup;

import io.harness.signup.notification.EmailInfo;
import io.harness.signup.notification.EmailType;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignupNotificationConfiguration {
  private String projectId;
  private String bucketName;
  private Map<EmailType, EmailInfo> templates;
  private int expireDurationInMinutes;
}
