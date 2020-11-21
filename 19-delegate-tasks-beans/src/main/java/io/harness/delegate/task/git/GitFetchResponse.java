package io.harness.delegate.task.git;

import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.git.model.FetchFilesResult;

import java.util.Map;
import lombok.Builder;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.NonFinal;

@Value
@Builder
public class GitFetchResponse implements DelegateTaskNotifyResponseData {
  Map<String, FetchFilesResult> filesFromMultipleRepo;
  TaskStatus taskStatus;
  String errorMessage;
  @NonFinal @Setter DelegateMetaInfo delegateMetaInfo;
}
