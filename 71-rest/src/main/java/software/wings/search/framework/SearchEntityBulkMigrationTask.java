package software.wings.search.framework;

import io.harness.persistence.PersistentEntity;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.rest.RestStatus;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.Callable;

@AllArgsConstructor
@Slf4j
public class SearchEntityBulkMigrationTask<T extends PersistentEntity> implements Callable<Boolean> {
  private ElasticsearchClient elasticsearchClient;
  private SearchEntity<T> searchEntity;
  private T object;
  private String newIndexName;

  private boolean insertDocument(String entityId, String entityJson) {
    IndexRequest indexRequest = new IndexRequest(newIndexName);
    indexRequest.id(entityId);
    indexRequest.source(entityJson, XContentType.JSON);
    try {
      IndexResponse indexResponse = elasticsearchClient.index(indexRequest);
      return indexResponse.status() == RestStatus.OK || indexResponse.status() == RestStatus.CREATED;
    } catch (IOException e) {
      logger.error("Could not connect to elasticsearch", e);
    }
    return false;
  }

  private boolean upsertEntityBaseView(EntityBaseView entityBaseView) {
    if (entityBaseView != null) {
      Optional<String> jsonString = SearchEntityUtils.convertToJson(entityBaseView);
      if (!jsonString.isPresent()) {
        return false;
      }
      return insertDocument(entityBaseView.getId(), jsonString.get());
    }
    return true;
  }

  @Override
  public Boolean call() {
    EntityBaseView entityBaseView = searchEntity.getView(object);
    if (entityBaseView != null) {
      return upsertEntityBaseView(entityBaseView);
    } else {
      logger.warn("object {} created nullBaseView", object.toString());
    }
    return true;
  }
}
