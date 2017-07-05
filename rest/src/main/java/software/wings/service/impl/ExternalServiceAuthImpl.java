package software.wings.service.impl;

import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.ErrorCode.INVALID_ARGUMENT;

import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.ExternalServiceAuthToken;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.ExternalAuthService;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.executable.ValidateOnExecution;

/**
 * Application Service Implementation class.
 *
 * @author Rishi
 */
@ValidateOnExecution
@Singleton
public class ExternalServiceAuthImpl implements ExternalAuthService {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public ExternalServiceAuthToken save(ExternalServiceAuthToken authToken) {
    return wingsPersistence.saveAndGet(ExternalServiceAuthToken.class, authToken);
  }

  @Override
  public ExternalServiceAuthToken get(String uuid) {
    ExternalServiceAuthToken authToken = wingsPersistence.get(ExternalServiceAuthToken.class, uuid);
    if (authToken == null) {
      throw new WingsException(INVALID_ARGUMENT, "args", "ExternalServiceAuthToken doesn't exist");
    }
    return authToken;
  }

  @Override
  public ExternalServiceAuthToken update(ExternalServiceAuthToken authToken) {
    Query<ExternalServiceAuthToken> query =
        wingsPersistence.createQuery(ExternalServiceAuthToken.class).field(ID_KEY).equal(authToken.getUuid());
    UpdateOperations<ExternalServiceAuthToken> operations =
        wingsPersistence.createUpdateOperations(ExternalServiceAuthToken.class);
    wingsPersistence.update(query, operations);
    return wingsPersistence.get(ExternalServiceAuthToken.class, authToken.getUuid());
  }

  @Override
  public void delete(String authTokenId) {
    ExternalServiceAuthToken authToken = wingsPersistence.get(ExternalServiceAuthToken.class, authTokenId);
    if (authToken == null) {
      throw new WingsException(INVALID_ARGUMENT, "args", "ExternalServiceAuthToken doesn't exist");
    }

    wingsPersistence.delete(ExternalServiceAuthToken.class, authTokenId);
  }
}
