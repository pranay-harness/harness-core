package software.wings.search.framework;

import com.google.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.UpdateByQueryRequest;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import software.wings.search.framework.EntityInfo.EntityInfoKeys;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public final class ElasticsearchDao implements SearchDao {
  @Inject RestHighLevelClient client;
  @Inject ElasticsearchIndexManager elasticsearchIndexManager;

  public boolean upsertDocument(String entityType, String entityId, String entityJson) {
    String indexName = elasticsearchIndexManager.getIndexName(entityType);
    UpdateRequest updateRequest = new UpdateRequest(indexName, entityId);
    updateRequest.doc(entityJson, XContentType.JSON);
    updateRequest.retryOnConflict(3);
    updateRequest.docAsUpsert(true);
    try {
      client.update(updateRequest, RequestOptions.DEFAULT);
      return true;
    } catch (ElasticsearchException e) {
      logger.error(String.format("Error while updating document %s in index %s", entityJson, indexName), e);
    } catch (IOException e) {
      logger.error("Could not connect to elasticsearch", e);
    }
    return false;
  }

  public boolean updateListInMultipleDocuments(String type, String listKey, String newElementValue, String elementId) {
    String indexName = elasticsearchIndexManager.getIndexName(type);
    UpdateByQueryRequest request = new UpdateByQueryRequest(indexName);
    request.setConflicts("proceed");
    Map<String, Object> params = new HashMap<>();
    params.put("entityType", listKey);
    params.put("newValue", newElementValue);
    params.put("filterId", elementId);

    String key = listKey + "." + EntityInfoKeys.id;
    request.setQuery(QueryBuilders.nestedQuery(
        listKey, QueryBuilders.boolQuery().filter(QueryBuilders.termQuery(key, elementId)), ScoreMode.Max));
    request.setScript(new Script(ScriptType.INLINE, "painless",
        "if (ctx._source[params.entityType] != null){for(item in ctx._source[params.entityType]){ if(item.id==params.filterId){item.name = params.newValue;}}}",
        params));
    return processUpdateByQuery(request, params, indexName);
  }

  public boolean updateKeyInMultipleDocuments(
      String entityType, String keyToUpdate, String newValue, String filterKey, String filterValue) {
    String indexName = elasticsearchIndexManager.getIndexName(entityType);
    UpdateByQueryRequest request = new UpdateByQueryRequest(indexName);
    request.setConflicts("proceed");
    Map<String, Object> params = new HashMap<>();
    params.put("keyToUpdate", keyToUpdate);
    params.put("newValue", newValue);
    params.put("filterKey", filterKey);
    params.put("filterValue", filterValue);
    request.setQuery(QueryBuilders.termQuery(filterKey, filterValue));
    request.setScript(new Script(ScriptType.INLINE, "painless",
        "if (ctx._source[params.filterKey] == params.filterValue) {ctx._source[params.keyToUpdate] = params.newValue;}",
        params));
    return processUpdateByQuery(request, params, indexName);
  }

  public boolean deleteDocument(String entityType, String entityId) {
    String indexName = elasticsearchIndexManager.getIndexName(entityType);
    DeleteRequest deleteRequest = new DeleteRequest(indexName, entityId);
    try {
      client.delete(deleteRequest, RequestOptions.DEFAULT);
      return true;
    } catch (ElasticsearchException e) {
      logger.error(String.format("Error while trying to delete document %s in index %s", entityId, indexName), e);
    } catch (IOException e) {
      logger.error("Could not connect to elasticsearch", e);
    }
    return false;
  }

  private boolean processUpdateByQuery(
      UpdateByQueryRequest updateByQueryRequest, Map<String, Object> params, String indexName) {
    try {
      BulkByScrollResponse bulkResponse = client.updateByQuery(updateByQueryRequest, RequestOptions.DEFAULT);
      if (bulkResponse.getSearchFailures().isEmpty() && bulkResponse.getBulkFailures().isEmpty()) {
        if (bulkResponse.getUpdated() == 0) {
          logger.warn(
              String.format("No documents were updated with params %s in index %s", params.toString(), indexName));
        }
        return true;
      }
      logger.error(String.format("Failed to update index %s by query with params %s", indexName, params.toString()));
    } catch (IOException e) {
      logger.error("Could not connect to elasticsearch", e);
    }
    return false;
  }
}