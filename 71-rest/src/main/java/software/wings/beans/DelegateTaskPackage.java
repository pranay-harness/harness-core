package software.wings.beans;

import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.SecretDetail;
import io.harness.security.encryption.EncryptionConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Value;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Value
@Builder
@AllArgsConstructor
public class DelegateTaskPackage {
  private String accountId;
  private String delegateTaskId;
  private String delegateId;
  private DelegateTask delegateTask;
  @Default private Map<String, EncryptionConfig> encryptionConfigs = new HashMap<>();
  @Default private Map<String, SecretDetail> secretDetails = new HashMap<>();
  @Default private Set<String> secrets = new HashSet<>();
}
