package io.harness.persistence;

import com.mongodb.DBCollection;

public interface HPersistence {
  /**
   * Gets the collection.
   *
   * @param collectionName the collection name
   * @return the collection
   */
  DBCollection getCollection(String collectionName);
}
