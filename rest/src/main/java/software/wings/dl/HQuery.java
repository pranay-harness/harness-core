package software.wings.dl;

import static java.lang.String.format;
import static software.wings.dl.HQuery.QueryChecks.AUTHORITY;
import static software.wings.dl.HQuery.QueryChecks.COUNT;
import static software.wings.dl.HQuery.QueryChecks.VALIDATE;

import com.google.common.collect.Sets;

import com.mongodb.DBCollection;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.Criteria;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.MorphiaIterator;
import org.mongodb.morphia.query.MorphiaKeyIterator;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.QueryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * The type H query.
 *
 * @param <T> the type parameter
 */
public class HQuery<T> extends QueryImpl<T> {
  private static final Logger logger = LoggerFactory.getLogger(HQuery.class);

  public enum QueryChecks { VALIDATE, AUTHORITY, COUNT }

  public static final Set<QueryChecks> allChecks = EnumSet.<QueryChecks>of(VALIDATE, AUTHORITY, COUNT);
  public static final Set<QueryChecks> excludeValidate = EnumSet.<QueryChecks>of(AUTHORITY, COUNT);
  public static final Set<QueryChecks> excludeAuthority = EnumSet.<QueryChecks>of(VALIDATE, COUNT);
  public static final Set<QueryChecks> excludeCount = EnumSet.<QueryChecks>of(AUTHORITY, VALIDATE);

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
        logger.error(format("Key list query returns %d items.", list.size()), new Exception(""));
      }
    }
  }

  private void checkListSize(List<T> list) {
    if (!queryChecks.contains(COUNT)) {
      return;
    }

    if (list.size() > 1000) {
      if (logger.isErrorEnabled()) {
        logger.error(format("List query returns %d items.", list.size()), new Exception(""));
      }
    }
  }

  @Override
  public List<Key<T>> asKeyList(FindOptions options) {
    enforceHarnessRules();
    final List<Key<T>> list = super.asKeyList(options);
    checkKeyListSize(list);
    return list;
  }

  @Override
  public List<T> asList(FindOptions options) {
    enforceHarnessRules();
    final List<T> list = super.asList(options);
    checkListSize(list);
    return list;
  }

  @Override
  public MorphiaIterator<T, T> fetchEmptyEntities(FindOptions options) {
    enforceHarnessRules();
    return super.fetchEmptyEntities(options);
  }

  @Override
  public MorphiaKeyIterator<T> fetchKeys(FindOptions options) {
    enforceHarnessRules();
    return super.fetchKeys(options);
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
      logger.warn("QUERY-ENFORCEMENT: appId or accountId must be present in List(Object/Key)/Get/Count/Search query",
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
