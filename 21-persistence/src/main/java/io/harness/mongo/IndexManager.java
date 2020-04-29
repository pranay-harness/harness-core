package io.harness.mongo;

import static io.harness.govern.IgnoreThrowable.ignoredOnPurpose;
import static io.harness.mongo.IndexManager.Mode.INSPECT;
import static io.harness.mongo.IndexManager.Mode.MANUAL;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
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

    public static boolean compareKeys(BasicDBObject keys1, BasicDBObject keys2) {
      Iterator<Entry<String, Object>> iterator1 = keys1.entrySet().iterator();
      Iterator<Entry<String, Object>> iterator2 = keys2.entrySet().iterator();
      while (iterator1.hasNext()) {
        if (!iterator2.hasNext()) {
          return false;
        }
        if (!iterator1.next().getKey().equals(iterator2.next().getKey())) {
          return false;
        }
      }
      return !iterator2.hasNext();
    }

    public boolean sameKeys(BasicDBObject keys) {
      return compareKeys(getKeys(), keys);
    }

    public boolean sameKeySet(IndexCreator other) {
      return getKeys().toMap().keySet().equals(other.getKeys().toMap().keySet());
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
