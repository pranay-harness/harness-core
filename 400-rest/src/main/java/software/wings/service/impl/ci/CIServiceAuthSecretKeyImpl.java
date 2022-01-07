/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.ci;

import static io.harness.cvng.core.services.CVNextGenConstants.VERIFICATION_SERVICE_SECRET;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.entity.ServiceSecretKey;
import io.harness.entity.ServiceSecretKey.ServiceSecretKeyKeys;
import io.harness.persistence.Store;

import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;

public class CIServiceAuthSecretKeyImpl implements CIServiceAuthSecretKey {
  @Inject private WingsPersistence wingsPersistence;
  @Override
  public String getCIAuthServiceSecretKey() {
    // TODO This is temporary communication until we have delegate microservice, using verification secret temporarily
    final String verificationServiceSecret = System.getenv(VERIFICATION_SERVICE_SECRET);
    if (isNotEmpty(verificationServiceSecret)) {
      return verificationServiceSecret;
    }
    return wingsPersistence.getDatastore(Store.builder().name("harness").build())
        .createQuery(ServiceSecretKey.class)
        .filter(ServiceSecretKeyKeys.serviceType, ServiceSecretKey.ServiceType.LEARNING_ENGINE)
        .get()
        .getServiceSecret();
  }
}
