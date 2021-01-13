package io.harness.delegate.beans;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.security.encryption.EncryptionConfig;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
public class DelegateTaskPackage {
  private String accountId;
  private String delegateTaskId;
  private String delegateId;
  private String logStreamingToken;
  private String delegateCallbackToken;

  private TaskData data;

  @Default private Map<String, EncryptionConfig> encryptionConfigs = new HashMap<>();
  @Default private Map<String, SecretDetail> secretDetails = new HashMap<>();
  @Default private Set<String> secrets = new HashSet<>();

  private List<ExecutionCapability> executionCapabilities;
  private LinkedHashMap<String, String> logStreamingAbstractions;
}
