package software.wings.service.impl.ci;

import static io.harness.cvng.core.services.CVNextGenConstants.VERIFICATION_SERVICE_SECRET;
import static io.harness.data.structure.HasPredicate.hasSome;

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
    if (hasSome(verificationServiceSecret)) {
      return verificationServiceSecret;
    }
    return wingsPersistence.getDatastore(Store.builder().name("harness").build())
        .createQuery(ServiceSecretKey.class)
        .filter(ServiceSecretKeyKeys.serviceType, ServiceSecretKey.ServiceType.LEARNING_ENGINE)
        .get()
        .getServiceSecret();
  }
}
