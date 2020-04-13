package io.harness.commandlibrary.common.service.impl;

import static io.harness.commandlibrary.common.CommandLibraryConstants.MANAGER_CLIENT_ID;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.commandlibrary.common.service.CommandLibraryService;
import io.harness.exception.InvalidArgumentsException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.ServiceSecretKey;
import software.wings.beans.ServiceSecretKey.ServiceSecretKeyKeys;
import software.wings.beans.ServiceSecretKey.ServiceType;
import software.wings.dl.WingsPersistence;

@Slf4j
@Singleton
public class CommandLibraryServiceImpl implements CommandLibraryService {
  private WingsPersistence wingsPersistence;

  @Inject
  public CommandLibraryServiceImpl(WingsPersistence wingsPersistence) {
    this.wingsPersistence = wingsPersistence;
  }

  @Override
  public String getSecretForClient(String clientId) {
    final ServiceType clientServiceType = getServiceTypeForClient(clientId);
    return wingsPersistence.createQuery(ServiceSecretKey.class)
        .filter(ServiceSecretKeyKeys.serviceType, clientServiceType)
        .get()
        .getServiceSecret();
  }

  private ServiceType getServiceTypeForClient(String clientId) {
    if (MANAGER_CLIENT_ID.equals(clientId)) {
      return ServiceType.MANAGER_TO_COMMAND_LIBRARY_SERVICE;
    }
    throw new InvalidArgumentsException(String.format("no ServiceType found for clientId = [%s]", clientId), null);
  }
}
