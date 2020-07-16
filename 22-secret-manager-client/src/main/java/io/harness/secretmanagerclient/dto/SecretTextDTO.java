package io.harness.secretmanagerclient.dto;

import io.harness.security.encryption.EncryptedDataParams;
import lombok.Data;

import java.util.Map;
import java.util.Set;

@Data
public class SecretTextDTO {
  private String name;
  private String value;
  private String path;
  private Set<EncryptedDataParams> parameters;
  private String kmsId;
  private Map<String, String> runtimeParameters;
  private boolean scopedToAccount;
}
