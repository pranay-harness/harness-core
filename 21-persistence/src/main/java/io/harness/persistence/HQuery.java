package io.harness.persistence;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static io.harness.persistence.HQuery.QueryChecks.AUTHORITY;
import static io.harness.persistence.HQuery.QueryChecks.COUNT;
import static io.harness.persistence.HQuery.QueryChecks.VALIDATE;

import com.google.common.collect.Sets;

import com.mongodb.DBCollection;
import io.harness.logging.AutoLogContext;
import io.harness.mongo.CollectionLogContext;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.CountOptions;
import org.mongodb.morphia.query.Criteria;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.MorphiaIterator;
import org.mongodb.morphia.query.MorphiaKeyIterator;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.QueryImpl;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * The type H query.
 *
 * @param <T> the type parameter
 */
@Slf4j
public class HQuery<T> extends QueryImpl<T> {
  public enum QueryChecks { VALIDATE, AUTHORITY, COUNT }

  public static final Set<QueryChecks> allChecks = EnumSet.<QueryChecks>of(VALIDATE, AUTHORITY, COUNT);
  public static final Set<QueryChecks> excludeValidate = EnumSet.<QueryChecks>of(AUTHORITY, COUNT);
  public static final Set<QueryChecks> excludeAuthority = EnumSet.<QueryChecks>of(VALIDATE, COUNT);
  public static final Set<QueryChecks> excludeCount = EnumSet.<QueryChecks>of(AUTHORITY, VALIDATE);
  public static final Set<QueryChecks> excludeAuthorityCount = EnumSet.<QueryChecks>of(QueryChecks.VALIDATE);

  private Set<QueryChecks> queryChecks = allChecks;

  private static Set<String> requiredFilterArgs = Sets.newHashSet("accountId", "accounts", "appId", "accountIds");

  public void setQueryChecks(Set<QueryChecks> queryChecks) {
    this.queryChecks = queryChecks;
    if (queryChecks.contains(VALIDATE)) {
      enableValidation();
    } else {
      disableValidation();
    }
  }

  /**
   * Creates a Query for the given type and collection
   *
   * @param clazz the type to return
   * @param coll  the collection to query
   * @param ds    the Datastore to use
   */
  public HQuery(Class<T> clazz, DBCollection coll, Datastore ds) {
    super(clazz, coll, ds);
  }

  private void checkKeyListSize(List<Key<T>> list) {
    if (!queryChecks.contains(COUNT)) {
      return;
    }

    if (list.size() > 5000) {
      if (logger.isErrorEnabled()) {
        logger.error("Key list query returns {} items.", list.size(), new Exception(""));
      }
    }
  }

  private void checkListSize(List<T> list) {
    if (!queryChecks.contains(COUNT)) {
      return;
    }

    if (list.size() > 1000) {
      if (logger.isErrorEnabled()) {
        logger.error("List query returns {} items.", list.size(), new Exception(""));
      }
    }
  }

  @Override
  @SuppressWarnings("deprecation")
  public T get(FindOptions options) {
    try (AutoLogContext ignore = new CollectionLogContext(super.getCollection().getName(), OVERRIDE_ERROR)) {
      return HPersistence.retry(() -> super.get(options));
    }
  }

  @Override
  public Key<T> getKey(FindOptions options) {
    return HPersistence.retry(() -> super.getKey(options));
  }

  @Override
  public List<Key<T>> asKeyList(FindOptions options) {
    enforceHarnessRules();
    return HPersistence.retry(() -> {
      final List<Key<T>> list = super.asKeyList(options);
      checkKeyListSize(list);
      return list;
    });
  }

  @Override
  @SuppressWarnings("deprecation")
  public List<T> asList(FindOptions options) {
    try (AutoLogContext ignore = new CollectionLogContext(super.getCollection().getName(), OVERRIDE_ERROR)) {
      enforceHarnessRules();
      return HPersistence.retry(() -> {
        final List<T> list = super.asList(options);
        checkListSize(list);
        return list;
      });
    }
  }

  @Override
  public long count() {
    return HPersistence.retry(() -> super.count());
  }

  @Override
  public long count(final CountOptions options) {
    return HPersistence.retry(() -> super.count(options));
  }

  @Override
  public MorphiaIterator<T, T> fetch() {
    enforceHarnessRules();
    return super.fetch();
  }

  @Override
  public MorphiaIterator<T, T> fetchEmptyEntities(FindOptions options) {
    enforceHarnessRules();
    return HPersistence.retry(() -> { return super.fetchEmptyEntities(options); });
  }

  @Override
  public MorphiaKeyIterator<T> fetchKeys(FindOptions options) {
    enforceHarnessRules();
    return HPersistence.retry(() -> { return super.fetchKeys(options); });
  }

  @Override
  public Query<T> search(String search) {
    enforceHarnessRules();
    return super.search(search);
  }

  @Override
  public Query<T> search(String search, String language) {
    enforceHarnessRules();
    return super.search(search, language);
  }

  private void enforceHarnessRules() {
    if (!queryChecks.contains(AUTHORITY)) {
      return;
    }

    if (!this.getChildren().stream().map(Criteria::getFieldName).anyMatch(requiredFilterArgs::contains)) {
      logger.error("QUERY-ENFORCEMENT: appId or accountId must be present in List(Object/Key)/Get/Count/Search query",
          new Exception(""));
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass() || !super.equals(o)) {
      return false;
    }
    HQuery<?> hQuery = (HQuery<?>) o;
    return Objects.equals(queryChecks, hQuery.queryChecks);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), queryChecks);
  }
}
