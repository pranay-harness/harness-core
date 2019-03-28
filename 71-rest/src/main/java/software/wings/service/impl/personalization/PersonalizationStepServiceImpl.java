package software.wings.service.impl.personalization;

import static io.harness.persistence.HPersistence.upsertReturnNewOptions;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.persistence.HPersistence;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.peronalization.PersonalizationStep;
import software.wings.service.intfc.personalization.PersonalizationStepService;
import software.wings.sm.StateType;

@Singleton
public class PersonalizationStepServiceImpl implements PersonalizationStepService {
  @Inject private HPersistence persistence;

  String combineAccountAndUser(String accountId, String userId) {
    return accountId + userId;
  }

  @Override
  public PersonalizationStep get(String accountId, String userId) {
    return persistence.createQuery(PersonalizationStep.class)
        .filter(PersonalizationStep.ACCOUNT_USER_ID_KEY, combineAccountAndUser(accountId, userId))
        .get();
  }

  @Override
  public PersonalizationStep addFavoriteStep(StateType step, String accountId, String userId) {
    final Query<PersonalizationStep> query =
        persistence.createQuery(PersonalizationStep.class)
            .filter(PersonalizationStep.ACCOUNT_USER_ID_KEY, combineAccountAndUser(accountId, userId));

    final UpdateOperations<PersonalizationStep> updateOperations =
        persistence.createUpdateOperations(PersonalizationStep.class)
            .addToSet(PersonalizationStep.FAVORITES_KEY, step.name());

    return persistence.upsert(query, updateOperations, upsertReturnNewOptions);
  }

  @Override
  public PersonalizationStep removeFavoriteStep(StateType step, String accountId, String userId) {
    final Query<PersonalizationStep> query =
        persistence.createQuery(PersonalizationStep.class)
            .filter(PersonalizationStep.ACCOUNT_USER_ID_KEY, combineAccountAndUser(accountId, userId));

    final UpdateOperations<PersonalizationStep> updateOperations =
        persistence.createUpdateOperations(PersonalizationStep.class)
            .removeAll(PersonalizationStep.FAVORITES_KEY, step.name());

    return persistence.upsert(query, updateOperations, upsertReturnNewOptions);
  }
}
