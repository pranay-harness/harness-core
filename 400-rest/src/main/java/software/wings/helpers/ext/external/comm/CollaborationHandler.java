package software.wings.helpers.ext.external.comm;

import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.helpers.ext.mail.SmtpConfig;

import java.util.List;

public interface CollaborationHandler {
  CollaborationProviderResponse handle(CollaborationProviderRequest request);

  boolean validateDelegateConnection(CollaborationProviderRequest request);
  boolean validateDelegateConnection(SmtpConfig smtpConfig, List<EncryptedDataDetail> encryptionDetails);
}
