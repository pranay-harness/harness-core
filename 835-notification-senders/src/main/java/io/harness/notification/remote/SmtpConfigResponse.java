package io.harness.notification.remote;

import io.harness.notification.SmtpConfig;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SmtpConfigResponse {
  SmtpConfig smtpConfig;
  List<EncryptedDataDetail> encryptionDetails;
}