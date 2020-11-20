package io.harness.beans;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.expression.SecretString.SECRET_MASK;
import static io.harness.security.encryption.SecretManagerType.CUSTOM;
import static io.harness.security.encryption.SecretManagerType.KMS;
import static io.harness.security.encryption.SecretManagerType.VAULT;

import io.harness.security.encryption.EncryptedDataParams;
import io.harness.security.encryption.SecretManagerType;
import lombok.Value;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Value
public class SecretUpdateData {
  EncryptedData existingRecord;
  HarnessSecret updatedSecret;
  boolean nameChanged;
  boolean valueChanged;
  boolean parametersChanged;
  boolean referenceChanged;
  boolean usageScopeChanged;

  public SecretUpdateData(HarnessSecret updatedSecret, EncryptedData existingRecord) {
    this.updatedSecret = updatedSecret;
    this.existingRecord = existingRecord;
    this.nameChanged = !Objects.equals(updatedSecret.getName(), existingRecord.getName());

    if (updatedSecret instanceof SecretText) {
      SecretText newSecretText = (SecretText) updatedSecret;
      this.valueChanged = isNotEmpty(newSecretText.getValue()) && !newSecretText.getValue().equals(SECRET_MASK);
      Set<EncryptedDataParams> parameters =
          newSecretText.getParameters() == null ? new HashSet<>() : newSecretText.getParameters();
      this.parametersChanged = !Objects.equals(parameters, existingRecord.getParameters());
      this.referenceChanged = !Objects.equals(newSecretText.getPath(), existingRecord.getPath());
    } else if (updatedSecret instanceof SecretFile) {
      SecretFile newSecretFile = (SecretFile) updatedSecret;
      this.valueChanged = isNotEmpty(newSecretFile.getFileContent());
      this.parametersChanged = false;
      this.referenceChanged = false;
    } else {
      this.valueChanged = false;
      this.parametersChanged = false;
      this.referenceChanged = false;
    }

    this.usageScopeChanged =
        !Objects.equals(updatedSecret.getUsageRestrictions(), existingRecord.getUsageRestrictions())
        || updatedSecret.isScopedToAccount() != existingRecord.isScopedToAccount()
        || updatedSecret.isInheritScopesFromSM() != existingRecord.isInheritScopesFromSM();
  }

  public String getChangeSummary() {
    StringBuilder builder = new StringBuilder();
    if (nameChanged) {
      builder.append("Changed name");
    }
    if (valueChanged) {
      builder.append(builder.length() > 0 ? " & secret" : " Changed secret");
    }
    if (referenceChanged) {
      builder.append(builder.length() > 0 ? " & reference" : " Changed reference");
    }
    if (parametersChanged) {
      builder.append(builder.length() > 0 ? " & secret variables" : " Changed secret variables");
    }
    if (usageScopeChanged) {
      builder.append(builder.length() > 0 ? " & usage restrictions" : "Changed usage restrictions");
    }
    return builder.toString();
  }

  public boolean shouldRencryptUsingKms(SecretManagerType secretManagerType) {
    return existingRecord.isInlineSecret() && valueChanged && secretManagerType.equals(KMS);
  }

  public boolean shouldRencryptUsingVault(SecretManagerType secretManagerType) {
    return existingRecord.isInlineSecret() && (valueChanged || nameChanged) && secretManagerType.equals(VAULT);
  }

  public boolean validateReferenceUsingVault(SecretManagerType secretManagerType) {
    return existingRecord.isReferencedSecret() && referenceChanged && secretManagerType.equals(VAULT);
  }

  public boolean validateCustomReference(SecretManagerType secretManagerType) {
    return existingRecord.isParameterizedSecret() && parametersChanged && secretManagerType.equals(CUSTOM);
  }
}
