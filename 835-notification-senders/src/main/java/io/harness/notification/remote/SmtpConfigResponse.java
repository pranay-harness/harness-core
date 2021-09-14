/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
