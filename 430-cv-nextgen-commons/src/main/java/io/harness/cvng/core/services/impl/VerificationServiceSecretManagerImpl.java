/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.services.impl;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.cvng.core.services.CVNextGenConstants;
import io.harness.cvng.core.services.api.VerificationServiceSecretManager;
import io.harness.entity.ServiceSecretKey;
import io.harness.entity.ServiceSecretKey.ServiceSecretKeyKeys;
import io.harness.entity.ServiceSecretKey.ServiceType;
import io.harness.persistence.HPersistence;
import io.harness.utils.Misc;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

public class VerificationServiceSecretManagerImpl implements VerificationServiceSecretManager {
  @Inject private HPersistence hPersistence;

  @Override
  public void initializeServiceSecretKeys() {
    for (ServiceType serviceType : ServiceType.values()) {
      hPersistence.saveIgnoringDuplicateKeys(Lists.newArrayList(
          ServiceSecretKey.builder().serviceType(serviceType).serviceSecret(Misc.generateSecretKey()).build()));
    }
  }
  @VisibleForTesting
  String getEnv(String name) {
    return System.getenv(name);
  }
  @Override
  public String getVerificationServiceSecretKey() {
    final String verificationServiceSecret = getEnv(CVNextGenConstants.VERIFICATION_SERVICE_SECRET);
    if (isNotEmpty(verificationServiceSecret)) {
      return verificationServiceSecret;
    }
    return hPersistence.createQuery(ServiceSecretKey.class)
        .filter(ServiceSecretKeyKeys.serviceType, ServiceType.LEARNING_ENGINE)
        .get()
        .getServiceSecret();
  }
}
