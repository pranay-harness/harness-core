package software.wings.delegatetasks.cvng;

import io.harness.cvng.beans.DataCollectionConnectorBundle;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.TaskType;
import software.wings.delegatetasks.DelegateTaskType;

import io.kubernetes.client.openapi.ApiException;
import java.util.List;

public interface K8InfoDataService {
  @DelegateTaskType(TaskType.K8_FETCH_NAMESPACES)
  List<String> getNameSpaces(DataCollectionConnectorBundle bundle, List<EncryptedDataDetail> encryptedDataDetails,
      String filter) throws ApiException;

  @DelegateTaskType(TaskType.K8_FETCH_WORKLOADS)
  List<String> getWorkloads(String namespace, DataCollectionConnectorBundle bundle,
      List<EncryptedDataDetail> encryptedDataDetails, String filter) throws ApiException;
}
