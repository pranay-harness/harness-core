package software.wings.service.intfc;

import software.wings.beans.ServiceSecretKey.ServiceType;

/**
 * Created by rsingh on 1/9/18.
 */
public interface LearningEngineService {
  void initializeServiceSecretKeys();

  String getServiceSecretKey(ServiceType serviceType);
}
