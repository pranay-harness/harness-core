package io.harness.mongo;

import static io.harness.govern.IgnoreThrowable.ignoredOnPurpose;
import static io.harness.mongo.IndexManager.Mode.INSPECT;
import static io.harness.mongo.IndexManager.Mode.MANUAL;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import io.harness.mongo.index.IndexType;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.mapping.MappedClass;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

@UtilityClass
@Slf4j
public class IndexManager {
  public enum Mode { AUTO, MANUAL, INSPECT }

  @Value
  @Builder
  public static class IndexCreator {
    private DBCollection collection;
    private BasicDBObject keys;
    private BasicDBObject options;

    public static boolean subsequenceKeys(BasicDBObject base, BasicDBObject subsequence) {
      if (base.size() <= subsequence.size()) {
        return false;
      }

      Iterator<Entry<String, Object>> iteratorBase = base.entrySet().iterator();
      Iterator<Entry<String, Object>> iteratorSubsequence = subsequence.entrySet().iterator();
      while (iteratorSubsequence.hasNext()) {
        Entry<String, Object> entryBase = iteratorBase.next();
        Entry<String, Object> entrySubsequence = iteratorSubsequence.next();

        if (!entryBase.getKey().equals(entrySubsequence.getKey())) {
          return false;
        }
        if (!entryBase.getValue().equals(entrySubsequence.getValue())) {
          return false;
        }
      }

      return true;
    }

    public static boolean compareKeysOrder(BasicDBObject keys1, BasicDBObject keys2) {
      Iterator<Entry<String, Object>> iterator1 = keys1.entrySet().iterator();
      Iterator<Entry<String, Object>> iterator2 = keys2.entrySet().iterator();
      while (iterator1.hasNext()) {
        if (!iterator2.hasNext()) {
          return false;
        }
        Entry<String, Object> item1 = iterator1.next();
        Entry<String, Object> item2 = iterator2.next();
        if (!item1.getKey().equals(item2.getKey())) {
          return false;
        }
      }
      return !iterator2.hasNext();
    }

    public static boolean compareKeysOrderAndValues(BasicDBObject keys1, BasicDBObject keys2) {
      Iterator<Entry<String, Object>> iterator1 = keys1.entrySet().iterator();
      Iterator<Entry<String, Object>> iterator2 = keys2.entrySet().iterator();
      while (iterator1.hasNext()) {
        if (!iterator2.hasNext()) {
          return false;
        }
        Entry<String, Object> item1 = iterator1.next();
        Entry<String, Object> item2 = iterator2.next();
        if (!item1.getKey().equals(item2.getKey())) {
          return false;
        }
        if (IndexType.fromValue(item1.getValue()) != IndexType.fromValue(item2.getValue())) {
          return false;
        }
      }
      return !iterator2.hasNext();
    }

    public boolean sameKeysOrder(BasicDBObject keys) {
      return compareKeysOrder(getKeys(), keys);
    }

    public boolean sameKeysOrderAndValues(BasicDBObject keys) {
      return compareKeysOrderAndValues(getKeys(), keys);
    }

    public boolean sameKeySet(IndexCreator other) {
      return getKeys().toMap().keySet().equals(other.getKeys().toMap().keySet());
    }

    public boolean isSubsequence(IndexCreator other) {
      return subsequenceKeys(getKeys(), other.getKeys());
    }
  }

  public void ensureIndexes(Mode mode, AdvancedDatastore datastore, Morphia morphia) {
    try {
      IndexManagerSession session = new IndexManagerSession(mode == null ? MANUAL : mode);
      if (session.ensureIndexes(datastore, morphia) && mode == INSPECT) {
        throw new IndexManagerInspectException();
      }
    } catch (IndexManagerReadOnlyException exception) {
      ignoredOnPurpose(exception);
      logger.warn("The user has read only access.");
    }
  }

  public static Map<String, IndexCreator> indexCreators(MappedClass mc, DBCollection collection) {
    return IndexManagerSession.indexCreators(mc, collection);
  }
}
