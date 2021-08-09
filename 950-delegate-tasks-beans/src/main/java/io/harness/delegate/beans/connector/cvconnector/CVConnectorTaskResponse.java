package io.harness.delegate.beans.connector.cvconnector;

import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CVConnectorTaskResponse implements DelegateTaskNotifyResponseData {
  private boolean valid;
  private String errorMessage;
  private DelegateMetaInfo delegateMetaInfo;
}
