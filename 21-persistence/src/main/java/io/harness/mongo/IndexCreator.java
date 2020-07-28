package io.harness.mongo;

import static io.harness.mongo.IndexManagerSession.UNIQUE;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import io.harness.mongo.index.IndexType;
import lombok.Builder;
import lombok.Value;

import java.util.Iterator;
import java.util.Map;

@Value
@Builder
public class IndexCreator {
  private DBCollection collection;
  private BasicDBObject keys;
  private BasicDBObject options;

  public String name() {
    return (String) options.get("name");
  }

  public static boolean subsequenceKeys(BasicDBObject base, BasicDBObject subsequence) {
    if (base.size() <= subsequence.size()) {
      return false;
    }

    Iterator<Map.Entry<String, Object>> iteratorBase = base.entrySet().iterator();
    Iterator<Map.Entry<String, Object>> iteratorSubsequence = subsequence.entrySet().iterator();
    while (iteratorSubsequence.hasNext()) {
      Map.Entry<String, Object> entryBase = iteratorBase.next();
      Map.Entry<String, Object> entrySubsequence = iteratorSubsequence.next();

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
    Iterator<Map.Entry<String, Object>> iterator1 = keys1.entrySet().iterator();
    Iterator<Map.Entry<String, Object>> iterator2 = keys2.entrySet().iterator();
    while (iterator1.hasNext()) {
      if (!iterator2.hasNext()) {
        return false;
      }
      Map.Entry<String, Object> item1 = iterator1.next();
      Map.Entry<String, Object> item2 = iterator2.next();
      if (!item1.getKey().equals(item2.getKey())) {
        return false;
      }
    }
    return !iterator2.hasNext();
  }

  public static boolean compareKeysOrderAndValues(BasicDBObject keys1, BasicDBObject keys2) {
    Iterator<Map.Entry<String, Object>> iterator1 = keys1.entrySet().iterator();
    Iterator<Map.Entry<String, Object>> iterator2 = keys2.entrySet().iterator();
    while (iterator1.hasNext()) {
      if (!iterator2.hasNext()) {
        return false;
      }
      Map.Entry<String, Object> item1 = iterator1.next();
      Map.Entry<String, Object> item2 = iterator2.next();
      if (!item1.getKey().equals(item2.getKey())) {
        return false;
      }
      if (IndexType.fromValue(item1.getValue()) != IndexType.fromValue(item2.getValue())) {
        return false;
      }
    }
    return !iterator2.hasNext();
  }

  public static boolean isUniqueIndex(DBObject index) {
    Boolean flag = (Boolean) index.get(UNIQUE);
    return flag != null && flag.booleanValue();
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
