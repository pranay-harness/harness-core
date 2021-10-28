package software.wings.service.impl.ci;

import static io.harness.cvng.core.services.CVNextGenConstants.VERIFICATION_SERVICE_SECRET;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.entity.ServiceSecretKey;
import io.harness.entity.ServiceSecretKey.ServiceSecretKeyKeys;
import io.harness.persistence.Store;

import software.wings.app.MainConfiguration;
import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;

public class CIServiceAuthSecretKeyImpl implements CIServiceAuthSecretKey {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private MainConfiguration mainConfiguration;
  @Override
  public String getCIAuthServiceSecretKey() {
    // TODO This is temporary communication until we have delegate microservice, using verification secret temporarily
    final String verificationServiceSecret = mainConfiguration.getVerificationServiceSecretKey();
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
