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
  @SuppressWarnings("deprecation")
  public Query<T> batchSize(int value) {
    impl.batchSize(value);
    return this;
  }

  @Override
  public Query<T> cloneQuery() {
    return impl.cloneQuery();
  }

  @Override
  @SuppressWarnings("deprecation")
  public Query<T> comment(String comment) {
    impl.comment(comment);
    return this;
  }

  @Override
  public FieldEnd<? extends CriteriaContainerImpl> criteria(String field) {
    return impl.criteria(field);
  }

  @Override
  @SuppressWarnings("deprecation")
  public Query<T> disableCursorTimeout() {
    impl.disableCursorTimeout();
    return this;
  }

  @Override
  @SuppressWarnings("deprecation")
  public Query<T> disableSnapshotMode() {
    impl.disableSnapshotMode();
    return this;
  }

  @Override
  public Query<T> disableValidation() {
    return impl.disableValidation();
  }

  @Override
  @SuppressWarnings("deprecation")
  public Query<T> enableCursorTimeout() {
    impl.enableCursorTimeout();
    return this;
  }

  @Override
  @SuppressWarnings("deprecation")
  public Query<T> enableSnapshotMode() {
    impl.enableSnapshotMode();
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
  @SuppressWarnings("deprecation")
  public int getBatchSize() {
    return impl.getBatchSize();
  }

  @Override
  @SuppressWarnings("deprecation")
  public DBCollection getCollection() {
    return impl.getCollection();
  }

  @Override
  public Class<T> getEntityClass() {
    return impl.getEntityClass();
  }

  @Override
  @SuppressWarnings("deprecation")
  public DBObject getFieldsObject() {
    return impl.getFieldsObject();
  }

  @Override
  @SuppressWarnings("deprecation")
  public int getLimit() {
    return impl.getLimit();
  }

  @Override
  @SuppressWarnings("deprecation")
  public int getOffset() {
    return impl.getOffset();
  }

  @Override
  @SuppressWarnings("deprecation")
  public DBObject getQueryObject() {
    return impl.getQueryObject();
  }

  @Override
  @SuppressWarnings("deprecation")
  public DBObject getSortObject() {
    return impl.getSortObject();
  }

  @Override
  @SuppressWarnings("deprecation")
  public Query<T> hintIndex(String idxName) {
    impl.hintIndex(idxName);
    return this;
  }

  @Override
  @SuppressWarnings("deprecation")
  public Query<T> limit(int value) {
    impl.limit(value);
    return this;
  }

  @Override
  @SuppressWarnings("deprecation")
  public Query<T> lowerIndexBound(DBObject lowerBound) {
    impl.lowerIndexBound(lowerBound);
    return this;
  }

  @Override
  @SuppressWarnings("deprecation")
  public Query<T> maxScan(int value) {
    impl.maxScan(value);
    return this;
  }

  @Override
  @SuppressWarnings("deprecation")
  public Query<T> maxTime(long maxTime, TimeUnit maxTimeUnit) {
    impl.maxTime(maxTime, maxTimeUnit);
    return this;
  }

  @Override
  @SuppressWarnings("deprecation")
  public Query<T> offset(int value) {
    impl.offset(value);
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
  @SuppressWarnings("deprecation")
  public Query<T> queryNonPrimary() {
    impl.queryNonPrimary();
    return this;
  }

  @Override
  @SuppressWarnings("deprecation")
  public Query<T> queryPrimaryOnly() {
    impl.queryPrimaryOnly();
    return this;
  }

  @Override
  public Query<T> retrieveKnownFields() {
    return impl.retrieveKnownFields();
  }

  @Override
  @SuppressWarnings("deprecation")
  public Query<T> retrievedFields(boolean include, String... fields) {
    impl.retrievedFields(include, fields);
    return this;
  }

  @Override
  @SuppressWarnings("deprecation")
  public Query<T> returnKey() {
    impl.returnKey();
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
  @SuppressWarnings("deprecation")
  public Query<T> upperIndexBound(DBObject upperBound) {
    this.upperIndexBound(upperBound);
    return this;
  }

  @Override
  @SuppressWarnings("deprecation")
  public Query<T> useReadPreference(ReadPreference readPref) {
    impl.useReadPreference(readPref);
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
  @SuppressWarnings("deprecation")
  public long countAll() {
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
  @SuppressWarnings("deprecation")
  public MorphiaIterator<T, T> tail() {
    return impl.tail();
  }

  @Override
  @SuppressWarnings("deprecation")
  public MorphiaIterator<T, T> tail(boolean awaitData) {
    return impl.tail(awaitData);
  }

  @Override
  public Iterator<T> iterator() {
    return impl.iterator();
  }
}
