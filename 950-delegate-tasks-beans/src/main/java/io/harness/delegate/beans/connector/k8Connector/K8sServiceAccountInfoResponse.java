package io.harness.delegate.beans.connector.k8Connector;

import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;

import com.hazelcast.util.Preconditions;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class K8sServiceAccountInfoResponse implements DelegateTaskNotifyResponseData {
  private String username;
  private List<String> groups;
  private DelegateMetaInfo delegateMetaInfo;

  public String getName() {
    return getUsernameArray()[3];
  }

  public String getNamespace() {
    return getUsernameArray()[2];
  }

  private String[] getUsernameArray() {
    Preconditions.checkState(username != null, "serviceAccount username is null");
    // "username": "system:serviceaccount:harness-delegate:default"
    final String[] usernameArray = username.split(":");

    Preconditions.checkState(
        usernameArray.length == 4, String.format("serviceAccount username [%s] is not of size 4", username));

    return usernameArray;
  }
}
