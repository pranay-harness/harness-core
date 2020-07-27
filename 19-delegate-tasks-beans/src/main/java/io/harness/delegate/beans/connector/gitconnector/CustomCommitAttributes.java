package io.harness.delegate.beans.connector.gitconnector;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CustomCommitAttributes {
  public static final String COMMIT_MSG = "Harness IO Git Sync.";
  public static final String HARNESS_IO_KEY_ = "Harness.io";
  public static final String HARNESS_SUPPORT_EMAIL_KEY = "support@harness.io";
  String authorName;
  String authorEmail;
  String commitMessage;
}
