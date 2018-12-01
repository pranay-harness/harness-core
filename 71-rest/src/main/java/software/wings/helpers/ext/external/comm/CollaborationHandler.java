package software.wings.helpers.ext.external.comm;

import software.wings.helpers.ext.mail.SmtpConfig;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;

public interface CollaborationHandler {
  CollaborationProviderResponse handle(CollaborationProviderRequest request);

  boolean validateDelegateConnection(CollaborationProviderRequest request);
  boolean validateDelegateConnection(SmtpConfig smtpConfig, List<EncryptedDataDetail> encryptionDetails);
}
