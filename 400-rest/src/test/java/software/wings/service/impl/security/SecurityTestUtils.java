/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.security;

import io.harness.data.structure.UUIDGenerator;
import io.harness.security.encryption.EncryptableSettingWithEncryptionDetails;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.EntityType;
import software.wings.beans.ServiceVariable;
import software.wings.beans.ServiceVariable.OverrideType;
import software.wings.beans.ServiceVariable.Type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author marklu on 10/18/19
 */
class SecurityTestUtils {
  static List<EncryptableSettingWithEncryptionDetails> getEncryptableSettingWithEncryptionDetailsList(
      String accountId, List<EncryptedDataDetail> encryptedDataDetailList) {
    List<EncryptableSettingWithEncryptionDetails> encryptableSettingWithEncryptionDetails = new ArrayList<>();
    for (EncryptedDataDetail encryptedDataDetail : encryptedDataDetailList) {
      encryptableSettingWithEncryptionDetails.add(
          getEncryptedDataDetail(getServiceVariable(accountId), encryptedDataDetail));
    }
    return encryptableSettingWithEncryptionDetails;
  }

  static EncryptableSettingWithEncryptionDetails getEncryptedDataDetail(
      ServiceVariable serviceVariable, EncryptedDataDetail encryptedDataDetail) {
    return EncryptableSettingWithEncryptionDetails.builder()
        .encryptableSetting(serviceVariable)
        .encryptedDataDetails(Collections.singletonList(encryptedDataDetail))
        .build();
  }

  static ServiceVariable getServiceVariable(String accountId) {
    ServiceVariable serviceVariable = ServiceVariable.builder()
                                          .accountId(accountId)
                                          .entityType(EntityType.SERVICE)
                                          .name(UUIDGenerator.generateUuid())
                                          .encryptedValue(UUIDGenerator.generateUuid())
                                          .value(UUIDGenerator.generateUuid().toCharArray())
                                          .type(Type.ENCRYPTED_TEXT)
                                          .overrideType(OverrideType.ALL)
                                          .decrypted(false)
                                          .build();
    serviceVariable.setUuid(UUIDGenerator.generateUuid());
    return serviceVariable;
  }
}
