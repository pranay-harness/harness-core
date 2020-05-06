package software.wings.service.impl.ci;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Inject;

import software.wings.beans.ServiceSecretKey;
import software.wings.beans.ServiceSecretKey.ServiceSecretKeyKeys;
import software.wings.common.VerificationConstants;
import software.wings.dl.WingsPersistence;

public class CIServiceAuthSecretKeyImpl implements CIServiceAuthSecretKey {
  @Inject private WingsPersistence wingsPersistence;
  @Override
  public String getCIAuthServiceSecretKey() {
    // TODO This is temporary communication until we have delegate microservice, using verification secret temporarily
    final String verificationServiceSecret = System.getenv(VerificationConstants.VERIFICATION_SERVICE_SECRET);
    if (isNotEmpty(verificationServiceSecret)) {
      return verificationServiceSecret;
    }
    return wingsPersistence.createQuery(ServiceSecretKey.class)
        .filter(ServiceSecretKeyKeys.serviceType, ServiceSecretKey.ServiceType.LEARNING_ENGINE)
        .get()
        .getServiceSecret();
  }
}
