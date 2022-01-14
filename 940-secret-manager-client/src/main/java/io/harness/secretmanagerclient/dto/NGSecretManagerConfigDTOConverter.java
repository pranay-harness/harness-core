package io.harness.secretmanagerclient.dto;

public interface NGSecretManagerConfigDTOConverter {
  SecretManagerConfigDTO toDTO(boolean maskSecrets);
}
