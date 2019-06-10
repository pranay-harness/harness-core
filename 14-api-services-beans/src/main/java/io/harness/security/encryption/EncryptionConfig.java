package io.harness.security.encryption;

public interface EncryptionConfig {
  /**
   * Return the UUID of this secret manager
   */
  String getUuid();

  /**
   * Return the name of this secret manager
   */
  String getName();

  /**
   * Get the account Id of this secret manager. For global default secret manager, the account Id could be of value
   * '__GLOBAL_ACCOUNT_ID__'.
   */
  String getAccountId();

  /**
   * Get the encryption type of this secret manager.
   */
  EncryptionType getEncryptionType();

  /**
   * Set the encryption type of this secret manager.
   */
  void setEncryptionType(EncryptionType encryptionType);

  /**
   * Retrieve if the current secret manager was set as default or not.
   */
  boolean isDefault();

  /**
   * Set or unset the current secret manager as default secret manager.
   */
  void setDefault(boolean isDefault);

  /**
   * Get the number of secrets encrypted using this secret manager.
   */
  int getNumOfEncryptedValue();

  /**
   * Get this secret manager's encryption service URL.
   */
  String getEncryptionServiceUrl();

  /**
   * Get the validation criteria string.
   */
  String getValidationCriteria();
}
