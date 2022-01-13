/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.search.framework;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;

/**
 * Decorator over Elasticsearch Dao
 * to enable updates to be processed
 * synchronously using a single thread.
 *
 * @author utkarsh
 */
@OwnedBy(PL)
@Slf4j
public class SynchronousElasticsearchDao implements SearchDao {
  @Inject private ElasticsearchDao elasticsearchDao;
  private final ExecutorService executorService = Executors.newFixedThreadPool(1, task -> {
    Thread t = new ThreadFactoryBuilder().setNameFormat("sync-elasticsearch-dao").build().newThread(task);
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
    log.info(
        "Add to or create a list with elements {} referenced by key {} in a document having id {} of index type {}",
        newElement, listToUpdate, documentId, entityType);
    Callable<Boolean> appendToListInMultipleDocumentsCallable =
        () -> elasticsearchDao.appendToListInSingleDocument(entityType, listToUpdate, documentId, newElement);
    return processElasticsearchTask(appendToListInMultipleDocumentsCallable);
  }

  @Override
  public boolean addTimestamp(
      String entityType, String listToUpdate, String documentId, long createdAt, int daysToRetain) {
    log.info(
        "Add current timestamp to a list with elements referenced by key {} in a document having id {} of index type {} with days to retain {}",
        listToUpdate, documentId, entityType, daysToRetain);
    Callable<Boolean> addTimestampCallable =
        () -> elasticsearchDao.addTimestamp(entityType, listToUpdate, documentId, createdAt, daysToRetain);
    return processElasticsearchTask(addTimestampCallable);
  }

  @Override
  public boolean addTimestamp(
      String entityType, String listToUpdate, List<String> documentIds, long createdAt, int daysToRetain) {
    Callable<Boolean> addTimestampCallable =
        () -> elasticsearchDao.addTimestamp(entityType, listToUpdate, documentIds, createdAt, daysToRetain);
    return processElasticsearchTask(addTimestampCallable);
  }

  @Override
  public boolean appendToListInSingleDocument(String entityType, String listToUpdate, String documentId,
      Map<String, Object> newElement, int maxElementsInList) {
    log.info(
        "Add to or create list with elements {} referenced by key {} in a document having id {} of index type {} with max documents set to {}",
        newElement, listToUpdate, documentId, entityType, maxElementsInList);
    Callable<Boolean> appendToListInSingleDocumentCallable = ()
        -> elasticsearchDao.appendToListInSingleDocument(
            entityType, listToUpdate, documentId, newElement, maxElementsInList);
    return processElasticsearchTask(appendToListInSingleDocumentCallable);
  }

  @Override
  public List<String> nestedQuery(String entityType, String fieldName, String value) {
    Callable<List<String>> nestedQueryCallable = () -> elasticsearchDao.nestedQuery(entityType, fieldName, value);
    Future<List<String>> nestedQueryFuture = executorService.submit(nestedQueryCallable);
    try {
      return nestedQueryFuture.get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.error("Could not perform the elasticsearch task, interrupted in between", e);
    } catch (ExecutionException e) {
      log.error("Could not perform the elasticsearch task, due to exception", e.getCause());
    } catch (CancellationException e) {
      log.error("Elasticsearch task was cancelled. This should not happen at all", e);
    }
    return new ArrayList<>();
  }

  @Override
  public boolean appendToListInMultipleDocuments(
      String entityType, String listToUpdate, List<String> documentIds, Map<String, Object> newElement) {
    log.info(
        "Add to or create a list with elements {} referenced by key {} in a document having id {} of index type {}",
        newElement, listToUpdate, documentIds, entityType);
    Callable<Boolean> appendToListInMultipleDocumentsCallable =
        () -> elasticsearchDao.appendToListInMultipleDocuments(entityType, listToUpdate, documentIds, newElement);
    return processElasticsearchTask(appendToListInMultipleDocumentsCallable);
  }

  @Override
  public boolean appendToListInMultipleDocuments(String entityType, String listToUpdate, List<String> documentIds,
      Map<String, Object> newElement, int maxElementsInList) {
    log.info(
        "Add to or create a list with elements {} referenced by key {} in a document having id {} of index type {} with max size of {}",
        newElement, listToUpdate, documentIds, entityType, maxElementsInList);
    Callable<Boolean> appendToListInMultipleDocumentsCallable = ()
        -> elasticsearchDao.appendToListInMultipleDocuments(
            entityType, listToUpdate, documentIds, newElement, maxElementsInList);
    return processElasticsearchTask(appendToListInMultipleDocumentsCallable);
  }

  @Override
  public boolean removeFromListInMultipleDocuments(
      String entityType, String listToUpdate, List<String> documentIds, String idToBeRemoved) {
    log.info("Remove entry in a list with id {} referenced by key {} in a document having idd {} of index type {}",
        idToBeRemoved, listToUpdate, documentIds, entityType);
    Callable<Boolean> removeFromListInMultipleDocumentsCallable =
        () -> elasticsearchDao.removeFromListInMultipleDocuments(entityType, listToUpdate, documentIds, idToBeRemoved);
    return processElasticsearchTask(removeFromListInMultipleDocumentsCallable);
  }

  @Override
  public boolean removeFromListInMultipleDocuments(
      String entityType, String listToUpdate, String documentId, String idToBeRemoved) {
    log.info("Remove entry in a list with id {} referenced by key {} in a document having idd {} of index type {}",
        idToBeRemoved, listToUpdate, documentId, entityType);
    Callable<Boolean> removeFromListInMultipleDocumentsCallable =
        () -> elasticsearchDao.removeFromListInMultipleDocuments(entityType, listToUpdate, documentId, idToBeRemoved);
    return processElasticsearchTask(removeFromListInMultipleDocumentsCallable);
  }

  @Override
  public boolean removeFromListInMultipleDocuments(String entityType, String listToUpdate, String idTobeRemoved) {
    log.info("Remove entry in a list with id {} referenced by key {} in documents of index type {}", idTobeRemoved,
        listToUpdate, entityType);
    Callable<Boolean> removeFromListInMultipleDocumentsCallable =
        () -> elasticsearchDao.removeFromListInMultipleDocuments(entityType, listToUpdate, idTobeRemoved);
    return processElasticsearchTask(removeFromListInMultipleDocumentsCallable);
  }

  @Override
  public boolean updateListInMultipleDocuments(
      String entityType, String listToUpdate, String newElement, String elementId, String elementKeyToChange) {
    log.info(
        "Update key {} with value {} for elements with id {} in list referenced by key {} in multiple documents of index type {}",
        entityType, listToUpdate, newElement, elementId, elementKeyToChange);
    Callable<Boolean> updateListInMultipleDocumentsCallable = ()
        -> elasticsearchDao.updateListInMultipleDocuments(
            entityType, listToUpdate, newElement, elementId, elementKeyToChange);
    return processElasticsearchTask(updateListInMultipleDocumentsCallable);
  }

  @Override
  public boolean deleteDocument(String entityType, String documentId) {
    log.info("Delete document in index type {} with id {}", entityType, documentId);
    Callable<Boolean> deleteDocumentCallable = () -> elasticsearchDao.deleteDocument(entityType, documentId);
    return processElasticsearchTask(deleteDocumentCallable);
  }

  private boolean processElasticsearchTask(Callable<Boolean> task) {
    try {
      boolean isSuccessful = false;
      int count = 0;
      while (count < 3 && !isSuccessful) {
        if (count != 0) {
          Thread.sleep(200);
        }
        Future<Boolean> taskFuture = executorService.submit(task);
        isSuccessful = taskFuture.get();
        count++;
      }
      return isSuccessful;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.error("Could not perform the elasticsearch task, interrupted in between", e);
    } catch (ExecutionException e) {
      log.error("Could not perform the elasticsearch task, due to exception", e.getCause());
    } catch (CancellationException e) {
      log.error("Elasticsearch task was cancelled. This should not happen at all", e);
    }
    return false;
  }
}
