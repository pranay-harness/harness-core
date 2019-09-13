package software.wings.search.framework;

public interface SearchDao {
  boolean upsertDocument(String type, String id, String jsonString);

  boolean updateKeyInMultipleDocuments(
      String type, String keyToUpdate, String newValue, String filterKey, String filterValue);

  boolean deleteDocument(String type, String id);
}
