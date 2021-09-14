/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.security;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.security.encryption.DelegateDecryptionService;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.EncryptedRecord;
import io.harness.security.encryption.EncryptedRecordData;
import io.harness.security.encryption.EncryptionConfig;

import software.wings.service.intfc.security.EncryptionService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

@OwnedBy(PL)
@Singleton
public class DelegateDecryptionServiceImpl implements DelegateDecryptionService {
  @Inject private EncryptionService encryptionService;

  @Override
  public Map<String, char[]> decrypt(Map<EncryptionConfig, List<EncryptedRecord>> encryptedRecordMap) {
    Map<String, char[]> resultMap = new HashMap<>();
    for (Entry<EncryptionConfig, List<EncryptedRecord>> entry : encryptedRecordMap.entrySet()) {
      EncryptionConfig encryptionConfig = entry.getKey();
      for (EncryptedRecord encryptedRecord : entry.getValue()) {
        EncryptedRecordData encryptedRecordData = (EncryptedRecordData) encryptedRecord;
        resultMap.put(encryptedRecord.getUuid(),
            encryptionService.getDecryptedValue(EncryptedDataDetail.builder()
                                                    .encryptedData(encryptedRecordData)
                                                    .encryptionConfig(encryptionConfig)
                                                    .build(),
                false));
      }
    }
    return resultMap;
  }
}
