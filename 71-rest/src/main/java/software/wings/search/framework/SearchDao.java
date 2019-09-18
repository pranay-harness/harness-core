package software.wings.search.framework;

public interface SearchDao {
  boolean upsertDocument(String entityType, String entityId, String entityJson);

  boolean updateKeyInMultipleDocuments(
      String entityType, String keyToUpdate, String newValue, String filterKey, String filterValue);

  boolean updateListInMultipleDocuments(String entityType, String listKey, String newElementValue, String elementId);

  boolean deleteDocument(String entityType, String entityId);
}
