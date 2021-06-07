package software.wings.delegatetasks.argo.beans.request;

import io.harness.delegate.task.TaskParameters;
import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.beans.settings.argo.ArgoConfig;

import java.util.List;

public interface ArgoRequest extends TaskParameters {
  String getAppName();
  RequestType requestType();
  List<EncryptedDataDetail> getEncryptedDataDetails();
  ArgoConfig getArgoConfig();
}
