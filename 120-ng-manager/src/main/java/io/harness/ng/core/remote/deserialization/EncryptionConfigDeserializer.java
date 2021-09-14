/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.remote.deserialization;

import io.harness.security.encryption.EncryptionConfig;
import io.harness.security.encryption.EncryptionType;

import software.wings.beans.AwsSecretsManagerConfig;
import software.wings.beans.AzureVaultConfig;
import software.wings.beans.CyberArkConfig;
import software.wings.beans.GcpKmsConfig;
import software.wings.beans.KmsConfig;
import software.wings.beans.LocalEncryptionConfig;
import software.wings.beans.SSHVaultConfig;
import software.wings.beans.VaultConfig;
import software.wings.security.encryption.secretsmanagerconfigs.CustomSecretsManagerConfig;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;

public class EncryptionConfigDeserializer extends StdDeserializer<EncryptionConfig> {
  public EncryptionConfigDeserializer() {
    super(EncryptionConfig.class);
  }

  @Override
  public EncryptionConfig deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
    ObjectMapper mapper = (ObjectMapper) jp.getCodec();
    ObjectNode obj = mapper.readTree(jp);
    EncryptionType encryptionType = EncryptionType.valueOf(obj.get("encryptionType").asText());
    switch (encryptionType) {
      case VAULT:
        return mapper.treeToValue(obj, VaultConfig.class);
      case VAULT_SSH:
        return mapper.treeToValue(obj, SSHVaultConfig.class);
      case LOCAL:
        return mapper.treeToValue(obj, LocalEncryptionConfig.class);
      case AWS_SECRETS_MANAGER:
        return mapper.treeToValue(obj, AwsSecretsManagerConfig.class);
      case KMS:
        return mapper.treeToValue(obj, KmsConfig.class);
      case GCP_KMS:
        return mapper.treeToValue(obj, GcpKmsConfig.class);
      case CUSTOM:
        return mapper.treeToValue(obj, CustomSecretsManagerConfig.class);
      case AZURE_VAULT:
        return mapper.treeToValue(obj, AzureVaultConfig.class);
      case CYBERARK:
        return mapper.treeToValue(obj, CyberArkConfig.class);
      default:
        throw new IllegalArgumentException("Unsupported type: " + encryptionType);
    }
  }
}
