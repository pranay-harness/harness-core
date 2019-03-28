package software.wings.service.impl.personalization;

import static io.harness.persistence.HPersistence.returnNewOptions;
import static io.harness.persistence.HPersistence.upsertReturnNewOptions;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.persistence.HPersistence;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.peronalization.PersonalizationStep;
import software.wings.service.intfc.personalization.PersonalizationStepService;
import software.wings.sm.StateType;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.ExecutorService;

@Singleton
public class PersonalizationStepServiceImpl implements PersonalizationStepService {
  private static final int RECENT_OPTIMIZATION_AMOUNT = 50;

  @Inject private HPersistence persistence;
  @Inject private ExecutorService executorService;

  String combineAccountAndUser(String accountId, String userId) {
    return accountId + userId;
  }

  public static void normalizeRecent(LinkedList<String> recent) {
    if (recent == null) {
      return;
    }

    Set<String> seen = new HashSet<>();

    final Iterator<String> stringIterator = recent.descendingIterator();
    while (stringIterator.hasNext()) {
      final String next = stringIterator.next();
      if (seen.size() >= MAX_ALLOWED_RECENT || seen.contains(next)) {
        stringIterator.remove();
        continue;
      }
      seen.add(next);
    }
  }

  @Override
  public PersonalizationStep fetch(String accountId, String userId) {
    final PersonalizationStep personalizationStep =
        persistence.createQuery(PersonalizationStep.class)
            .filter(PersonalizationStep.ACCOUNT_USER_ID_KEY, combineAccountAndUser(accountId, userId))
            .get();

    normalizeRecent(personalizationStep.getRecent());

    return personalizationStep;
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

  @Override
  public PersonalizationStep addRecentStep(StateType step, String accountId, String userId) {
    final Query<PersonalizationStep> query =
        persistence.createQuery(PersonalizationStep.class)
            .filter(PersonalizationStep.ACCOUNT_USER_ID_KEY, combineAccountAndUser(accountId, userId));

    final UpdateOperations<PersonalizationStep> updateOperations =
        persistence.createUpdateOperations(PersonalizationStep.class).push(PersonalizationStep.RECENT_KEY, step.name());

    final PersonalizationStep personalizationStep = persistence.upsert(query, updateOperations, upsertReturnNewOptions);

    if (personalizationStep.getRecent().size() > RECENT_OPTIMIZATION_AMOUNT) {
      normalizeRecent(personalizationStep.getRecent());
      executorService.submit(() -> {
        // Note that this create a race between the obtaining the values and updating with the optimized version.
        // The impact for the customer is considered ignorable.
        final UpdateOperations<PersonalizationStep> updateRecentOperations =
            persistence.createUpdateOperations(PersonalizationStep.class)
                .set(PersonalizationStep.RECENT_KEY, personalizationStep.getRecent());

        persistence.findAndModify(query, updateRecentOperations, returnNewOptions);
      });
    }

    return personalizationStep;
  }
}
