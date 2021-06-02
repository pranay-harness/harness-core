package io.harness.delegate.task.manifests.response;

import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.logging.CommandExecutionStatus;
import io.harness.manifest.CustomSourceFile;

import java.util.Collection;
import java.util.Map;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.NonFinal;

@Value
@Builder
public class CustomManifestValuesFetchResponse implements DelegateTaskNotifyResponseData {
  CommandExecutionStatus commandExecutionStatus;
  @Nullable Map<String, Collection<CustomSourceFile>> valuesFilesContentMap;

  @NonFinal @Setter DelegateMetaInfo delegateMetaInfo;
}
