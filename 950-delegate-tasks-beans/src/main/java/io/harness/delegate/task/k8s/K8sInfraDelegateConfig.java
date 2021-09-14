/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.List;

@OwnedBy(CDP)
public interface K8sInfraDelegateConfig {
  String getNamespace();
  List<EncryptedDataDetail> getEncryptionDataDetails();
}
