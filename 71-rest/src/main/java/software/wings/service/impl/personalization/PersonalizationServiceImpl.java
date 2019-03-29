package software.wings.service.impl.personalization;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.persistence.HPersistence.returnNewOptions;
import static io.harness.persistence.HPersistence.upsertReturnNewOptions;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.persistence.HPersistence;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.peronalization.Personalization;
import software.wings.service.intfc.personalization.PersonalizationService;
import software.wings.sm.StateType;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

@Singleton
public class PersonalizationServiceImpl implements PersonalizationService {
  private static final int RECENT_OPTIMIZATION_AMOUNT = 50;

  @Inject private HPersistence persistence;
  @Inject private ExecutorService executorService;

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

  private Query<Personalization> prepareQuery(String accountId, String userId) {
    return persistence.createQuery(Personalization.class)
        .filter(Personalization.ACCOUNT_ID_KEY, accountId)
        .filter(Personalization.USER_ID_KEY, userId);
  }

  @Override
  public Personalization fetch(String accountId, String userId, List<String> objects) {
    final Query<Personalization> query = prepareQuery(accountId, userId);
    if (isNotEmpty(objects)) {
      query.project(Personalization.ACCOUNT_ID_KEY, true);
      query.project(Personalization.USER_ID_KEY, true);

      for (String object : objects) {
        query.project(object, true);
      }
    }

    final Personalization personalization = query.get();
    if (personalization.getSteps() != null) {
      normalizeRecent(personalization.getSteps().getRecent());
    }

    return personalization;
  }

  @Override
  public Personalization addFavoriteStep(StateType step, String accountId, String userId) {
    final Query<Personalization> query = prepareQuery(accountId, userId);

    final UpdateOperations<Personalization> updateOperations =
        persistence.createUpdateOperations(Personalization.class)
            .addToSet(Personalization.STEPS_FAVORITES_KEY, step.name());

    return persistence.upsert(query, updateOperations, upsertReturnNewOptions);
  }

  @Override
  public Personalization removeFavoriteStep(StateType step, String accountId, String userId) {
    final Query<Personalization> query = prepareQuery(accountId, userId);

    final UpdateOperations<Personalization> updateOperations =
        persistence.createUpdateOperations(Personalization.class)
            .removeAll(Personalization.STEPS_FAVORITES_KEY, step.name());

    return persistence.upsert(query, updateOperations, upsertReturnNewOptions);
  }

  @Override
  public Personalization addRecentStep(StateType step, String accountId, String userId) {
    final Query<Personalization> query = prepareQuery(accountId, userId);

    final UpdateOperations<Personalization> updateOperations =
        persistence.createUpdateOperations(Personalization.class).push(Personalization.STEPS_RECENT_KEY, step.name());

    final Personalization Personalization = persistence.upsert(query, updateOperations, upsertReturnNewOptions);

    if (Personalization.getSteps().getRecent().size() > RECENT_OPTIMIZATION_AMOUNT) {
      normalizeRecent(Personalization.getSteps().getRecent());
      executorService.submit(() -> {
        // Note that this create a race between the obtaining the values and updating with the optimized version.
        // The impact for the customer is considered ignorable.
        final UpdateOperations<Personalization> updateRecentOperations =
            persistence.createUpdateOperations(Personalization.class)
                .set(Personalization.STEPS_RECENT_KEY, Personalization.getSteps().getRecent());

        persistence.findAndModify(query, updateRecentOperations, returnNewOptions);
      });
    }

    return Personalization;
  }
}
