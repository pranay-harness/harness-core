package io.harness.connector.entities.embedded.customhealthconnector;

import lombok.Builder;
import lombok.Value;
@Value
@Builder
public class CustomHealthConnectorKeyAndValue {
  String key;
  boolean isValueEncrypted;
  String encryptedValueRef;
  String value;
}
