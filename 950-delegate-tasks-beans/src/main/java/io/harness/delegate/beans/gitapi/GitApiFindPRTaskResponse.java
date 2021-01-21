package io.harness.delegate.beans.gitapi;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GitApiFindPRTaskResponse implements GitApiResult {
  String prJson;
}
