package io.harness.delegate.beans.connector.jira.connection;

import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class JiraTestConnectionTaskNGResponse implements DelegateTaskNotifyResponseData {
  Boolean canConnect;
  String errorMessage;

  DelegateMetaInfo delegateMetaInfo;
}
