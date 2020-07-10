package io.harness.delegate.beans.connector.gitconnector;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CustomCommitAttributes {
  String authorName;
  String authorEmail;
  String commitMessage;
}
