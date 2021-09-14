/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.dao;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.entities.NGEncryptedData;

@OwnedBy(PL)
public interface NGEncryptedDataDao {
  NGEncryptedData save(NGEncryptedData encryptedData);

  NGEncryptedData get(String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);

  boolean delete(String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);
}
