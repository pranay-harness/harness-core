package software.wings.dl;

import static java.lang.String.format;

import com.google.common.base.Throwables;
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

import java.util.List;
import java.util.Set;

/**
 * The type H query.
 *
 * @param <T> the type parameter
 */
public class HQuery<T> extends QueryImpl<T> {
  private static final Logger logger = LoggerFactory.getLogger(HQuery.class);
  private static Set<String> requiredFilterArgs = Sets.newHashSet("accountId", "appId");
  private boolean exemptedRequest;

  public boolean isExemptedRequest() {
    return exemptedRequest;
  }

  public void setExemptedRequest(boolean exemptedRequest) {
    this.exemptedRequest = exemptedRequest;
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
    if (list.size() > 5000) {
      if (logger.isErrorEnabled()) {
        logger.error(format("Key list query returns %d items.", list.size()), new Exception(""));
      }
    }
  }

  private void checkListSize(List<T> list) {
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
    boolean requiredArgFound = exemptedRequest
        || this.getChildren().stream().map(Criteria::getFieldName).anyMatch(requiredFilterArgs::contains);
    if (!requiredArgFound) {
      logger.warn(
          "HQUERY-ENFORCEMENT: appId or accountId must be present in any List(Object/Key)/Get/Count/Search query. ST: [{}]",
          Throwables.getStackTraceAsString(new Throwable()));
    }
  }
}
