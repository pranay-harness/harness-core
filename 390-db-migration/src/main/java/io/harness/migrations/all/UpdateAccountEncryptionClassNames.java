/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import static com.mongodb.DBCollection.ID_FIELD_NAME;

import io.harness.migrations.Migration;
import io.harness.security.SimpleEncryption;

import software.wings.beans.Account;
import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UpdateAccountEncryptionClassNames implements Migration {
  private static String fieldName = "className";
  private static Class collectionClass = Account.class;
  private static String oldClassName = "software.wings.security.encryption.SimpleEncryption";
  private static Class clazz = SimpleEncryption.class;

  @Inject WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    final DBCollection collection = wingsPersistence.getCollection(collectionClass);

    String js = String.format("return searchInObject(obj);"
            + "function searchInObject(obj){"
            + "  for(var k in obj){"
            + "    if(typeof obj[k] == 'object' && obj[k] !== null) {"
            + "      if(searchInObject(obj[k])){"
            + "        return true;"
            + "      }"
            + "    } else {"
            + "      if(k == '%s' && obj[k] == '%s') {"
            + "        return true;"
            + "      }"
            + "    }"
            + "  }"
            + "  return false;"
            + "}",
        fieldName, oldClassName);

    final DBCursor cursor = collection.find(new BasicDBObject().append("$where", js));

    try {
      while (cursor.hasNext()) {
        final DBObject object = cursor.next();

        final BasicDBObject query = new BasicDBObject();
        query.append(ID_FIELD_NAME, object.get(ID_FIELD_NAME));

        final BasicDBObject updateSet = new BasicDBObject();
        appendUpdateSet("", object, query, updateSet);

        collection.findAndModify(query, updateSet);
      }
    } finally {
      cursor.close();
    }
  }

  private void appendUpdateSet(String prefix, DBObject object, BasicDBObject filterSet, BasicDBObject updateSet) {
    for (String key : object.keySet()) {
      final Object value = object.get(key);
      final String field = prefix + key;
      if (value instanceof String && fieldName.equals(key)) {
        String className = (String) object.get(key);
        if (oldClassName.equals(className)) {
          filterSet.append(field, oldClassName);
          updateSet.append("$set", new BasicDBObject(field, clazz.getName()));
        }
      }

      if (value instanceof DBObject) {
        appendUpdateSet(field + ".", (DBObject) value, filterSet, updateSet);
      }
    }
  }
}
