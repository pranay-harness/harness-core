package io.harness.delegate.beans.connector.k8Connector;

import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class K8sServiceAccountInfoResponse implements DelegateTaskNotifyResponseData {
  String username;
  List<String> groups;
  private DelegateMetaInfo delegateMetaInfo;
}
