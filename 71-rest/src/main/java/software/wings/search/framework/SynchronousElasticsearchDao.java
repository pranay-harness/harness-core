package software.wings.search.framework;

import com.google.inject.Inject;

import lombok.extern.slf4j.Slf4j;

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
  public boolean upsertDocument(String type, String id, String jsonString) {
    Callable<Boolean> upsertDocumentCallable = () -> elasticsearchDao.upsertDocument(type, id, jsonString);
    try {
      Future<Boolean> upsertFuture = executorService.submit(upsertDocumentCallable);
      return upsertFuture.get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      logger.error("Could not upsert document, interrupted in between", e);
    } catch (ExecutionException e) {
      logger.error("Could not upsert document, due to exception", e.getCause());
    } catch (CancellationException e) {
      logger.error("Upsert task was cancelled. This should not happen at all", e);
    }
    return false;
  }

  @Override
  public boolean updateKeyInMultipleDocuments(
      String type, String keyToUpdate, String newValue, String filterKey, String filterValue) {
    Callable<Boolean> updateKeyInMultipleDocumentsCallable =
        () -> elasticsearchDao.updateKeyInMultipleDocuments(type, keyToUpdate, newValue, filterKey, filterValue);

    try {
      Future<Boolean> updateFuture = executorService.submit(updateKeyInMultipleDocumentsCallable);
      return updateFuture.get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      logger.error("Could not update key in multiple documents, interrupted in between", e);
    } catch (ExecutionException e) {
      logger.error("Could not update keu in multiple documents, due to exception", e.getCause());
    } catch (CancellationException e) {
      logger.error("Updatekey in multiple documents task was cancelled. This should not happen at all", e);
    }
    return false;
  }

  @Override
  public boolean deleteDocument(String type, String id) {
    Callable<Boolean> deleteDocumentCallable = () -> elasticsearchDao.deleteDocument(type, id);

    try {
      Future<Boolean> deleteFuture = executorService.submit(deleteDocumentCallable);
      return deleteFuture.get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      logger.error("Could not delete document, interrupted in between", e);
    } catch (ExecutionException e) {
      logger.error("Could not delete document, due to exception", e.getCause());
    } catch (CancellationException e) {
      logger.error("Delete task was cancelled. This should not happen at all", e);
    }
    return false;
  }
}
