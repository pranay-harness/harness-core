package software.wings.search.framework;

import com.google.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.UpdateByQueryRequest;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import software.wings.search.framework.EntityBaseView.EntityBaseViewKeys;
import software.wings.search.framework.EntityInfo.EntityInfoKeys;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Elasticsearch Dao
 *
 * @author utkarsh
 */
@Slf4j
public final class ElasticsearchDao implements SearchDao {
  @Inject RestHighLevelClient client;
  @Inject ElasticsearchIndexManager elasticsearchIndexManager;
  private static final String SCRIPT_LANGUAGE = "painless";
  private static final String COULD_NOT_CONNECT_ERROR_MESSAGE = "Could not connect to elasticsearch";
  private static final String FIELD_TO_UPDATE_PARAMS_KEY = "fieldToUpdate";
  private static final String NEW_ELEMENT_PARAMS_KEY = "newList";
  private static final String ID_TO_BE_DELETED_PARAMS_KEY = "idToBeDeleted";

  public boolean upsertDocument(String entityType, String entityId, String entityJson) {
    String indexName = elasticsearchIndexManager.getIndexName(entityType);
    UpdateRequest updateRequest = new UpdateRequest(indexName, entityId);
    updateRequest.doc(entityJson, XContentType.JSON);
    updateRequest.retryOnConflict(3);
    updateRequest.docAsUpsert(true);
    updateRequest.setRefreshPolicy(RefreshPolicy.WAIT_UNTIL);
    try {
      client.update(updateRequest, RequestOptions.DEFAULT);
      return true;
    } catch (ElasticsearchException e) {
      logger.error(String.format("Error while updating document %s in index %s", entityJson, indexName), e);
    } catch (IOException e) {
      logger.error(COULD_NOT_CONNECT_ERROR_MESSAGE, e);
    }
    return false;
  }

  public boolean appendToListInMultipleDocuments(
      String entityType, String listToUpdate, List<String> documentIds, Map<String, Object> newElement) {
    String indexName = elasticsearchIndexManager.getIndexName(entityType);
    UpdateByQueryRequest request = new UpdateByQueryRequest(indexName);
    request.setRefresh(true);

    Map<String, Object> params = new HashMap<>();
    params.put(FIELD_TO_UPDATE_PARAMS_KEY, listToUpdate);
    params.put(NEW_ELEMENT_PARAMS_KEY, newElement);

    request.setRefresh(true);
    request.setQuery(QueryBuilders.termsQuery(EntityBaseViewKeys.id, documentIds));
    request.setScript(new Script(ScriptType.INLINE, SCRIPT_LANGUAGE,
        "if(ctx._source[params.fieldToUpdate]!=null){ctx._source[params.fieldToUpdate].add(params.newList);} "
            + "else{ctx._source[params.fieldToUpdate] = [params.newList];}",
        params));
    logger.info(request.toString());
    return processUpdateByQuery(request, params, indexName);
  }

  public boolean appendToListInMultipleDocuments(String entityType, String listToUpdate, List<String> documentIds,
      Map<String, Object> newElement, int maxElementsInList) {
    String indexName = elasticsearchIndexManager.getIndexName(entityType);
    UpdateByQueryRequest request = new UpdateByQueryRequest(indexName);
    request.setRefresh(true);

    Map<String, Object> params = new HashMap<>();
    params.put(FIELD_TO_UPDATE_PARAMS_KEY, listToUpdate);
    params.put(NEW_ELEMENT_PARAMS_KEY, newElement);
    params.put("maxElementsInList", maxElementsInList);

    request.setQuery(QueryBuilders.termsQuery(EntityBaseViewKeys.id, documentIds));
    request.setScript(new Script(ScriptType.INLINE, "painless",
        "if (ctx._source[params.fieldToUpdate] != null) {if (ctx._source[params.fieldToUpdate].length == params.maxElementsInList) { ctx._source[params.fieldToUpdate] = ctx._source[params.fieldToUpdate].stream().skip(1).collect(Collectors.toList());} ctx._source[params.fieldToUpdate].add(params.newList);} else {ctx._source[params.fieldToUpdate] = [params.newList];}",
        params));
    return processUpdateByQuery(request, params, indexName);
  }

  public boolean appendToListInSingleDocument(
      String entityType, String listToUpdate, String documentId, Map<String, Object> newElement) {
    String indexName = elasticsearchIndexManager.getIndexName(entityType);
    UpdateByQueryRequest request = new UpdateByQueryRequest(indexName);
    request.setRefresh(true);
    String key = listToUpdate + "." + EntityInfoKeys.id;
    Map<String, Object> params = new HashMap<>();
    params.put(FIELD_TO_UPDATE_PARAMS_KEY, listToUpdate);
    params.put(NEW_ELEMENT_PARAMS_KEY, newElement);
    request.setQuery(
        QueryBuilders.boolQuery()
            .must(QueryBuilders.termQuery(EntityBaseViewKeys.id, documentId))
            .mustNot(QueryBuilders.nestedQuery(listToUpdate,
                QueryBuilders.boolQuery().filter(QueryBuilders.termQuery(key, newElement.get(EntityInfoKeys.id))),
                ScoreMode.Max)));
    request.setScript(new Script(ScriptType.INLINE, SCRIPT_LANGUAGE,
        "if(ctx._source[params.fieldToUpdate]!=null){ctx._source[params.fieldToUpdate].add(params.newList);} "
            + "else{ctx._source[params.fieldToUpdate] = [params.newList];}",
        params));
    return processUpdateByQuery(request, params, indexName);
  }

  public boolean appendToListInSingleDocument(String entityType, String listToUpdate, String documentId,
      Map<String, Object> newElement, int maxElementsInList) {
    String indexName = elasticsearchIndexManager.getIndexName(entityType);
    UpdateByQueryRequest request = new UpdateByQueryRequest(indexName);
    request.setRefresh(true);

    String key = listToUpdate + "." + EntityInfoKeys.id;
    Map<String, Object> params = new HashMap<>();
    params.put(FIELD_TO_UPDATE_PARAMS_KEY, listToUpdate);
    params.put(NEW_ELEMENT_PARAMS_KEY, newElement);
    params.put("maxElementsInList", maxElementsInList);
    request.setQuery(
        QueryBuilders.boolQuery()
            .must(QueryBuilders.termQuery(EntityBaseViewKeys.id, documentId))
            .mustNot(QueryBuilders.nestedQuery(listToUpdate,
                QueryBuilders.boolQuery().filter(QueryBuilders.termQuery(key, newElement.get(EntityBaseViewKeys.id))),
                ScoreMode.Max)));
    request.setScript(new Script(ScriptType.INLINE, SCRIPT_LANGUAGE,
        "if (ctx._source[params.fieldToUpdate] != null) {if (ctx._source[params.fieldToUpdate].length == params.maxElementsInList) { ctx._source[params.fieldToUpdate] = ctx._source[params.fieldToUpdate].stream().skip(1).collect(Collectors.toList());} ctx._source[params.fieldToUpdate].add(params.newList);} else {ctx._source[params.fieldToUpdate] = [params.newList];}",
        params));
    return processUpdateByQuery(request, params, indexName);
  }

  public boolean removeFromListInMultipleDocuments(
      String entityType, String listToUpdate, List<String> documentIds, String idToBeDeleted) {
    String indexName = elasticsearchIndexManager.getIndexName(entityType);
    UpdateByQueryRequest request = new UpdateByQueryRequest(indexName);
    request.setRefresh(true);

    Map<String, Object> params = new HashMap<>();
    params.put(FIELD_TO_UPDATE_PARAMS_KEY, listToUpdate);
    params.put(ID_TO_BE_DELETED_PARAMS_KEY, idToBeDeleted);

    request.setQuery(QueryBuilders.termsQuery(EntityBaseViewKeys.id, documentIds));
    request.setScript(new Script(ScriptType.INLINE, SCRIPT_LANGUAGE,
        "if(ctx._source[params.fieldToUpdate]!=null){ctx._source[params.fieldToUpdate] = ctx._source[params.fieldToUpdate].stream().filter(item -> item.id != params.idToBeDeleted).collect(Collectors.toList());}",
        params));
    return processUpdateByQuery(request, params, indexName);
  }

  public boolean removeFromListInMultipleDocuments(
      String entityType, String listToUpdate, String documentId, String idToBeDeleted) {
    String indexName = elasticsearchIndexManager.getIndexName(entityType);
    UpdateByQueryRequest request = new UpdateByQueryRequest(indexName);
    request.setRefresh(true);

    Map<String, Object> params = new HashMap<>();
    params.put(FIELD_TO_UPDATE_PARAMS_KEY, listToUpdate);
    params.put(ID_TO_BE_DELETED_PARAMS_KEY, idToBeDeleted);

    request.setQuery(QueryBuilders.termsQuery(EntityBaseViewKeys.id, documentId));
    request.setScript(new Script(ScriptType.INLINE, SCRIPT_LANGUAGE,
        "if(ctx._source[params.fieldToUpdate]!=null){ctx._source[params.fieldToUpdate] = ctx._source[params.fieldToUpdate].stream().filter(item -> item.id != params.idToBeDeleted).collect(Collectors.toList());}",
        params));
    return processUpdateByQuery(request, params, indexName);
  }

  public boolean removeFromListInMultipleDocuments(String entityType, String listToUpdate, String idToBeDeleted) {
    String indexName = elasticsearchIndexManager.getIndexName(entityType);
    UpdateByQueryRequest request = new UpdateByQueryRequest(indexName);
    request.setRefresh(true);

    Map<String, Object> params = new HashMap<>();
    params.put("listKey", listToUpdate);
    params.put(ID_TO_BE_DELETED_PARAMS_KEY, idToBeDeleted);
    String key = listToUpdate + "." + EntityInfoKeys.id;

    request.setQuery(QueryBuilders.nestedQuery(
        listToUpdate, QueryBuilders.boolQuery().filter(QueryBuilders.termQuery(key, idToBeDeleted)), ScoreMode.Max));
    request.setScript(new Script(ScriptType.INLINE, SCRIPT_LANGUAGE,
        "if(ctx._source[params.listKey]!=null){ctx._source[params.listKey] = ctx._source[params.listKey].stream().filter(item -> item.id != params.idToBeDeleted).collect(Collectors.toList());}",
        params));
    return processUpdateByQuery(request, params, indexName);
  }

  public boolean updateListInMultipleDocuments(
      String type, String listToUpdate, String newElement, String elementId, String elementKeyToChange) {
    String indexName = elasticsearchIndexManager.getIndexName(type);
    UpdateByQueryRequest request = new UpdateByQueryRequest(indexName);
    request.setRefresh(true);

    Map<String, Object> params = new HashMap<>();
    params.put("entityType", listToUpdate);
    params.put("newValue", newElement);
    params.put("filterId", elementId);
    params.put(FIELD_TO_UPDATE_PARAMS_KEY, elementKeyToChange);
    String key = listToUpdate + "." + EntityInfoKeys.id;

    request.setQuery(QueryBuilders.nestedQuery(
        listToUpdate, QueryBuilders.boolQuery().filter(QueryBuilders.termQuery(key, elementId)), ScoreMode.Max));
    request.setScript(new Script(ScriptType.INLINE, SCRIPT_LANGUAGE,
        "if (ctx._source[params.entityType] != null){for(item in ctx._source[params.entityType]){ if(item.id==params.filterId){item[params.fieldToUpdate] = params.newValue;}}}",
        params));
    return processUpdateByQuery(request, params, indexName);
  }

  public boolean updateKeyInMultipleDocuments(
      String entityType, String listToUpdate, String newValue, String filterKey, String filterValue) {
    String indexName = elasticsearchIndexManager.getIndexName(entityType);
    UpdateByQueryRequest request = new UpdateByQueryRequest(indexName);

    Map<String, Object> params = new HashMap<>();
    params.put("keyToUpdate", listToUpdate);
    params.put("newValue", newValue);
    params.put("filterKey", filterKey);
    params.put("filterValue", filterValue);

    request.setRefresh(true);
    request.setQuery(QueryBuilders.termQuery(filterKey, filterValue));
    request.setScript(new Script(ScriptType.INLINE, SCRIPT_LANGUAGE,
        "if (ctx._source[params.filterKey] == params.filterValue) {ctx._source[params.keyToUpdate] = params.newValue;}",
        params));
    return processUpdateByQuery(request, params, indexName);
  }

  public boolean deleteDocument(String entityType, String documentId) {
    String indexName = elasticsearchIndexManager.getIndexName(entityType);
    DeleteRequest deleteRequest = new DeleteRequest(indexName, documentId);
    try {
      client.delete(deleteRequest, RequestOptions.DEFAULT);
      return true;
    } catch (ElasticsearchException e) {
      logger.error(String.format("Error while trying to delete document %s in index %s", documentId, indexName), e);
    } catch (IOException e) {
      logger.error(COULD_NOT_CONNECT_ERROR_MESSAGE, e);
    }
    return false;
  }

  public boolean addTimestamp(String entityType, String fieldName, String documentId, int daysToRetain) {
    String indexName = elasticsearchIndexManager.getIndexName(entityType);
    UpdateByQueryRequest request = new UpdateByQueryRequest(indexName);
    request.setRefresh(true);
    Map<String, Object> params = new HashMap<>();
    long currentTimestampValue = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
    long newTimestampValue = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) - daysToRetain * 86400;

    params.put(FIELD_TO_UPDATE_PARAMS_KEY, fieldName);
    params.put("newTimestampValue", newTimestampValue);
    params.put("currentTimestampValue", currentTimestampValue);

    request.setQuery(QueryBuilders.termQuery(EntityBaseViewKeys.id, documentId));
    request.setScript(new Script(ScriptType.INLINE, SCRIPT_LANGUAGE,
        "if (ctx._source[params.fieldToUpdate] != null) { ctx._source[params.fieldToUpdate] = ctx._source[params.fieldToUpdate].stream().filter(item -> item >= params.newTimestampValue).collect(Collectors.toList()); ctx._source[params.fieldToUpdate].add(params.currentTimestampValue); } else { ctx._source[params.fieldToUpdate] = [params.currentTimestampValue]; }",
        params));
    return processUpdateByQuery(request, params, indexName);
  }

  public boolean addTimestamp(String entityType, String fieldName, List<String> documentIds, int daysToRetain) {
    String indexName = elasticsearchIndexManager.getIndexName(entityType);
    UpdateByQueryRequest request = new UpdateByQueryRequest(indexName);
    request.setRefresh(true);
    Map<String, Object> params = new HashMap<>();
    long currentTimestampValue = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
    long newTimestampValue = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) - daysToRetain * 86400;
    params.put(FIELD_TO_UPDATE_PARAMS_KEY, fieldName);
    params.put("newTimestampValue", newTimestampValue);
    params.put("currentTimestampValue", currentTimestampValue);

    request.setQuery(QueryBuilders.termsQuery(EntityBaseViewKeys.id, documentIds));
    request.setScript(new Script(ScriptType.INLINE, "painless",
        "if (ctx._source[params.fieldToUpdate] != null) { ctx._source[params.fieldToUpdate] = ctx._source[params.fieldToUpdate].stream().filter(item -> item >= params.newTimestampValue).collect(Collectors.toList()); ctx._source[params.fieldToUpdate].add(params.currentTimestampValue); } else { ctx._source[params.fieldToUpdate] = [params.currentTimestampValue]; }",
        params));
    return processUpdateByQuery(request, params, indexName);
  }

  public List<String> nestedQuery(String entityType, String fieldName, String value) {
    final int MAX_RESULTS = 500;
    String indexName = elasticsearchIndexManager.getIndexName(entityType);
    SearchRequest searchRequest = new SearchRequest(indexName);
    String path = fieldName + "." + EntityBaseViewKeys.id;
    NestedQueryBuilder nestedQueryBuilder =
        QueryBuilders.nestedQuery(fieldName, QueryBuilders.termQuery(path, value), ScoreMode.Max);
    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder().query(nestedQueryBuilder).size(MAX_RESULTS);
    searchRequest.source(searchSourceBuilder);
    try {
      SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
      List<String> requiredIds = new ArrayList<>();

      for (SearchHit searchHit : searchResponse.getHits()) {
        requiredIds.add(searchHit.getId());
      }
      return requiredIds;
    } catch (IOException e) {
      logger.error(COULD_NOT_CONNECT_ERROR_MESSAGE, e);
    }
    return new ArrayList<>();
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
      logger.error(COULD_NOT_CONNECT_ERROR_MESSAGE, e);
    }
    return false;
  }
}