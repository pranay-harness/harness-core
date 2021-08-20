package io.harness.mongo;

import static io.harness.mongo.IndexManagerSession.UNIQUE;

import com.mongodb.DBObject;
import lombok.experimental.UtilityClass;

@UtilityClass
public class IndexUtils {
  public static boolean isUniqueIndex(DBObject index) {
    Boolean flag = (Boolean) index.get(UNIQUE);
    return flag != null && flag.booleanValue();
  }
}
