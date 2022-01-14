package io.harness.delegate.beans.connector.servicenow.connection;

import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ServiceNowTestConnectionTaskNGResponse implements DelegateTaskNotifyResponseData {
  Boolean canConnect;
  String errorMessage;

  DelegateMetaInfo delegateMetaInfo;
}
