package software.wings.dl;

import static java.lang.String.format;

import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.ReadPreference;
import org.bson.types.CodeWScope;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.ArraySlice;
import org.mongodb.morphia.query.CountOptions;
import org.mongodb.morphia.query.Criteria;
import org.mongodb.morphia.query.CriteriaContainer;
import org.mongodb.morphia.query.CriteriaContainerImpl;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Meta;
import org.mongodb.morphia.query.MorphiaIterator;
import org.mongodb.morphia.query.MorphiaKeyIterator;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class HQuery<T> implements Query<T> {
  private static final Logger logger = LoggerFactory.getLogger(MongoHelper.class);

  private Query<T> impl;

  HQuery(Query<T> impl) {
    this.impl = impl;
  }

  @Override
  public CriteriaContainer and(Criteria... criteria) {
    return impl.and(criteria);
  }

  @Override
  public Query<T> batchSize(int value) {
    logger.error("Do not use batchSize it is deprecated");
    return this;
  }

  @Override
  public Query<T> cloneQuery() {
    return impl.cloneQuery();
  }

  @Override
  public Query<T> comment(String comment) {
    logger.error("Do not use comment it is deprecated");
    return this;
  }

  @Override
  public FieldEnd<? extends CriteriaContainerImpl> criteria(String field) {
    return impl.criteria(field);
  }

  @Override
  public Query<T> disableCursorTimeout() {
    logger.error("Do not use disableCursorTimeout it is deprecated");
    return this;
  }

  @Override
  public Query<T> disableSnapshotMode() {
    logger.error("Do not use disableSnapshotMode it is deprecated");
    return this;
  }

  @Override
  public Query<T> disableValidation() {
    return impl.disableValidation();
  }

  @Override
  public Query<T> enableCursorTimeout() {
    logger.error("Do not use enableCursorTimeout it is deprecated");
    return this;
  }

  @Override
  public Query<T> enableSnapshotMode() {
    logger.error("Do not use enableSnapshotMode it is deprecated");
    return this;
  }

  @Override
  public Query<T> enableValidation() {
    return impl.enableValidation();
  }

  @Override
  public Map<String, Object> explain() {
    return impl.explain();
  }

  @Override
  public Map<String, Object> explain(FindOptions options) {
    return impl.explain(options);
  }

  @Override
  public FieldEnd<? extends Query<T>> field(String field) {
    return impl.field(field);
  }

  @Override
  public Query<T> filter(String condition, Object value) {
    return impl.filter(condition, value);
  }

  @Override
  public int getBatchSize() {
    logger.error("Do not use enableSnapshotMode it is deprecated");
    return impl.getBatchSize();
  }

  @Override
  public DBCollection getCollection() {
    logger.error("Do not use getCollection it is deprecated");
    return impl.getCollection();
  }

  @Override
  public Class<T> getEntityClass() {
    return impl.getEntityClass();
  }

  @Override
  public DBObject getFieldsObject() {
    logger.error("Do not use getFieldsObject it is deprecated");
    return impl.getFieldsObject();
  }

  @Override
  public int getLimit() {
    logger.error("Do not use getLimit it is deprecated");
    return impl.getLimit();
  }

  @Override
  public int getOffset() {
    logger.error("Do not use getOffset it is deprecated");
    return impl.getOffset();
  }

  @Override
  public DBObject getQueryObject() {
    logger.error("Do not use getQueryObject it is deprecated");
    return impl.getQueryObject();
  }

  @Override
  public DBObject getSortObject() {
    logger.error("Do not use getSortObject it is deprecated");
    return impl.getSortObject();
  }

  @Override
  public Query<T> hintIndex(String idxName) {
    logger.error("Do not use hintIndex it is deprecated");
    return this;
  }

  @Override
  public Query<T> limit(int value) {
    logger.error("Do not use limit it is deprecated");
    return this;
  }

  @Override
  public Query<T> lowerIndexBound(DBObject lowerBound) {
    logger.error("Do not use lowerIndexBound it is deprecated");
    return this;
  }

  @Override
  public Query<T> maxScan(int value) {
    logger.error("Do not use maxScan it is deprecated");
    return this;
  }

  @Override
  public Query<T> maxTime(long maxTime, TimeUnit maxTimeUnit) {
    logger.error("Do not use maxTime it is deprecated");
    return this;
  }

  @Override
  public Query<T> offset(int value) {
    logger.error("Do not use offset it is deprecated");
    return this;
  }

  @Override
  public CriteriaContainer or(Criteria... criteria) {
    return impl.or(criteria);
  }

  @Override
  public Query<T> order(String sort) {
    return impl.order(sort);
  }

  @Override
  public Query<T> order(Meta sort) {
    return impl.order(sort);
  }

  @Override
  public Query<T> order(Sort... sorts) {
    return impl.order(sorts);
  }

  @Override
  public Query<T> project(String field, boolean include) {
    return impl.project(field, include);
  }

  @Override
  public Query<T> project(String field, ArraySlice slice) {
    return impl.project(field, slice);
  }

  @Override
  public Query<T> project(Meta meta) {
    return impl.project(meta);
  }

  @Override
  public Query<T> queryNonPrimary() {
    logger.error("Do not use queryNonPrimary it is deprecated");
    return this;
  }

  @Override
  public Query<T> queryPrimaryOnly() {
    logger.error("Do not use queryPrimaryOnly it is deprecated");
    return this;
  }

  @Override
  public Query<T> retrieveKnownFields() {
    return impl.retrieveKnownFields();
  }

  @Override
  public Query<T> retrievedFields(boolean include, String... fields) {
    logger.error("Do not use retrievedFields it is deprecated");
    return this;
  }

  @Override
  public Query<T> returnKey() {
    logger.error("Do not use returnKey it is deprecated");
    return this;
  }

  @Override
  public Query<T> search(String text) {
    return impl.search(text);
  }

  @Override
  public Query<T> search(String text, String language) {
    return impl.search(text, language);
  }

  @Override
  public Query<T> upperIndexBound(DBObject upperBound) {
    logger.error("Do not use upperIndexBound it is deprecated");
    return this;
  }

  @Override
  public Query<T> useReadPreference(ReadPreference readPref) {
    logger.error("Do not use useReadPreference it is deprecated");
    return this;
  }

  @Override
  public Query<T> where(String js) {
    return impl.where(js);
  }

  @Override
  public Query<T> where(CodeWScope js) {
    return impl.where(js);
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
  public List<Key<T>> asKeyList() {
    final List<Key<T>> list = impl.asKeyList();
    checkKeyListSize(list);
    return list;
  }

  @Override
  public List<T> asList() {
    final List<T> list = impl.asList();
    checkListSize(list);
    return list;
  }

  @Override
  public List<Key<T>> asKeyList(FindOptions options) {
    final List<Key<T>> list = impl.asKeyList(options);
    checkKeyListSize(list);
    return list;
  }

  @Override
  public List<T> asList(FindOptions options) {
    final List<T> list = impl.asList(options);
    checkListSize(list);
    return list;
  }

  @Override
  public long countAll() {
    logger.error("Do not use countAll it is deprecated");
    return impl.countAll();
  }

  @Override
  public long count() {
    return impl.count();
  }

  @Override
  public long count(CountOptions options) {
    return impl.count(options);
  }

  @Override
  public MorphiaIterator<T, T> fetch() {
    return impl.fetch();
  }

  @Override
  public MorphiaIterator<T, T> fetch(FindOptions options) {
    return impl.fetch(options);
  }

  @Override
  public MorphiaIterator<T, T> fetchEmptyEntities() {
    return impl.fetchEmptyEntities();
  }

  @Override
  public MorphiaIterator<T, T> fetchEmptyEntities(FindOptions options) {
    return impl.fetchEmptyEntities(options);
  }

  @Override
  public MorphiaKeyIterator<T> fetchKeys() {
    return impl.fetchKeys();
  }

  @Override
  public MorphiaKeyIterator<T> fetchKeys(FindOptions options) {
    return impl.fetchKeys(options);
  }

  @Override
  public T get() {
    return impl.get();
  }

  @Override
  public T get(FindOptions options) {
    return impl.get(options);
  }

  @Override
  public Key<T> getKey() {
    return impl.getKey();
  }

  @Override
  public Key<T> getKey(FindOptions options) {
    return impl.getKey(options);
  }

  @Override
  public MorphiaIterator<T, T> tail() {
    logger.error("Do not use tail it is deprecated");
    return impl.tail();
  }

  @Override
  public MorphiaIterator<T, T> tail(boolean awaitData) {
    logger.error("Do not use tail it is deprecated");
    return impl.tail(awaitData);
  }

  @Override
  public Iterator<T> iterator() {
    return impl.iterator();
  }
}
