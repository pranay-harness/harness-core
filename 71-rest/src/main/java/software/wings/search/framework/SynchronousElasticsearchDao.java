package software.wings.search.framework;

import com.google.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Slf4j
public class SynchronousElasticsearchDao implements SearchDao {
  @Inject private ElasticsearchDao elasticsearchDao;
  private final ExecutorService executorService = Executors.newFixedThreadPool(1, task -> {
    Thread t = Executors.defaultThreadFactory().newThread(task);
    t.setDaemon(true);
    return t;
  });

  @Override
  public boolean upsertDocument(String entityType, String entityId, String entityJson) {
    Callable<Boolean> upsertDocumentCallable = () -> elasticsearchDao.upsertDocument(entityType, entityId, entityJson);
    return processElasticsearchTask(upsertDocumentCallable);
  }

  @Override
  public boolean updateKeyInMultipleDocuments(
      String entityType, String keyToUpdate, String newValue, String filterKey, String filterValue) {
    Callable<Boolean> updateKeyInMultipleDocumentsCallable =
        () -> elasticsearchDao.updateKeyInMultipleDocuments(entityType, keyToUpdate, newValue, filterKey, filterValue);
    return processElasticsearchTask(updateKeyInMultipleDocumentsCallable);
  }

  @Override
  public boolean appendToListInSingleDocument(
      String entityType, String listToUpdate, String documentId, Map<String, Object> newElement) {
    logger.info(
        "Add to or create a list with elements {} referenced by key {} in a document having id {} of index type {}",
        newElement, listToUpdate, documentId, entityType);
    Callable<Boolean> appendToListInMultipleDocumentsCallable =
        () -> elasticsearchDao.appendToListInSingleDocument(entityType, listToUpdate, documentId, newElement);
    return processElasticsearchTask(appendToListInMultipleDocumentsCallable);
  }

  @Override
  public boolean addTimestamp(String entityType, String listToUpdate, String documentId, int daysToRetain) {
    logger.info(
        "Add current timestamp to a list with elements referenced by key {} in a document having id {} of index type {} with days to retain {}",
        listToUpdate, documentId, entityType, daysToRetain);
    Callable<Boolean> addTimestampCallable =
        () -> elasticsearchDao.addTimestamp(entityType, listToUpdate, documentId, daysToRetain);
    return processElasticsearchTask(addTimestampCallable);
  }

  @Override
  public boolean appendToListInSingleDocument(
      String entityType, String listKey, String documentId, Map<String, Object> newElement, int maxElementsInList) {
    logger.info(
        "Add to or create list with elements {} referenced by key {} in a document having id {} of index type {} with max documents set to {}",
        newElement, listKey, documentId, entityType, maxElementsInList);
    Callable<Boolean> appendToListInSingleDocumentCallable = ()
        -> elasticsearchDao.appendToListInSingleDocument(
            entityType, listKey, documentId, newElement, maxElementsInList);
    return processElasticsearchTask(appendToListInSingleDocumentCallable);
  }

  @Override
  public List<String> nestedQuery(String entityType, String fieldName, String value) {
    Callable<List<String>> nestedQueryCallable = () -> elasticsearchDao.nestedQuery(entityType, fieldName, value);
    return processElasticsearchTask(nestedQueryCallable);
  }

  @Override
  public boolean appendToListInMultipleDocuments(
      String entityType, String listToUpdate, List<String> documentIds, Map<String, Object> newElement) {
    logger.info(
        "Add to or create a list with elements {} referenced by key {} in a document having id {} of index type {}",
        newElement, listToUpdate, documentIds, entityType);
    Callable<Boolean> appendToListInMultipleDocumentsCallable =
        () -> elasticsearchDao.appendToListInMultipleDocuments(entityType, listToUpdate, documentIds, newElement);
    return processElasticsearchTask(appendToListInMultipleDocumentsCallable);
  }

  @Override
  public boolean removeFromListInMultipleDocuments(
      String entityType, String listToUpdate, List<String> documentIds, String idToBeRemoved) {
    logger.info("Remove entry in a list with id {} referenced by key {} in a document having idd {} of index type {}",
        idToBeRemoved, listToUpdate, documentIds, entityType);
    Callable<Boolean> removeFromListInMultipleDocumentsCallable =
        () -> elasticsearchDao.removeFromListInMultipleDocuments(entityType, listToUpdate, documentIds, idToBeRemoved);
    return processElasticsearchTask(removeFromListInMultipleDocumentsCallable);
  }

  @Override
  public boolean removeFromListInMultipleDocuments(
      String entityType, String listToUpdate, String documentId, String idToBeRemoved) {
    logger.info("Remove entry in a list with id {} referenced by key {} in a document having idd {} of index type {}",
        idToBeRemoved, listToUpdate, documentId, entityType);
    Callable<Boolean> removeFromListInMultipleDocumentsCallable =
        () -> elasticsearchDao.removeFromListInMultipleDocuments(entityType, listToUpdate, documentId, idToBeRemoved);
    return processElasticsearchTask(removeFromListInMultipleDocumentsCallable);
  }

  @Override
  public boolean removeFromListInMultipleDocuments(String entityType, String listToUpdate, String idTobeRemoved) {
    logger.info("Remove entry in a list with id {} referenced by key {} in documents of index type {}", idTobeRemoved,
        listToUpdate, entityType);
    Callable<Boolean> removeFromListInMultipleDocumentsCallable =
        () -> elasticsearchDao.removeFromListInMultipleDocuments(entityType, listToUpdate, idTobeRemoved);
    return processElasticsearchTask(removeFromListInMultipleDocumentsCallable);
  }

  @Override
  public boolean updateListInMultipleDocuments(
      String entityType, String listToUpdate, String newElement, String elementId, String elementKeyToChange) {
    logger.info(
        "Update key {} with value {} for elements with id {} in list referenced by key {} in multiple documents of index type {}",
        entityType, listToUpdate, newElement, elementId, elementKeyToChange);
    Callable<Boolean> updateListInMultipleDocumentsCallable = ()
        -> elasticsearchDao.updateListInMultipleDocuments(
            entityType, listToUpdate, newElement, elementId, elementKeyToChange);
    return processElasticsearchTask(updateListInMultipleDocumentsCallable);
  }

  @Override
  public boolean deleteDocument(String entityType, String documentId) {
    logger.info("Delete document in index type {} with id {}", entityType, documentId);
    Callable<Boolean> deleteDocumentCallable = () -> elasticsearchDao.deleteDocument(entityType, documentId);
    return processElasticsearchTask(deleteDocumentCallable);
  }

  private <T> T processElasticsearchTask(Callable<T> task) {
    T response = null;
    try {
      int count = 0;
      while (count < 3 && response == null) {
        if (count != 0) {
          Thread.sleep(200);
        }
        Future<T> taskFuture = executorService.submit(task);
        response = taskFuture.get();
        count++;
      }
      return response;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      logger.error("Could not perform the elasticsearch task, interrupted in between", e);
    } catch (ExecutionException e) {
      logger.error("Could not perform the elasticsearch task, due to exception", e.getCause());
    } catch (CancellationException e) {
      logger.error("Elasticsearch task was cancelled. This should not happen at all", e);
    }
    return response;
  }
}
