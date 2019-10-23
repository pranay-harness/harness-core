package software.wings.search.framework;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.UpdateByQueryRequest;

import java.io.IOException;

/**
 * This wrapper over the RestHighLevelClient is required
 * to separate the non-mockable third party dependencies.
 *
 * @author utkarsh
 */
@Singleton
public class ElasticsearchClient {
  @Inject private RestHighLevelClient client;

  IndexResponse index(IndexRequest indexRequest) throws IOException, ElasticsearchException {
    return client.index(indexRequest, RequestOptions.DEFAULT);
  }

  UpdateResponse update(UpdateRequest updateRequest) throws IOException, ElasticsearchException {
    return client.update(updateRequest, RequestOptions.DEFAULT);
  }

  DeleteResponse delete(DeleteRequest deleteRequest) throws IOException, ElasticsearchException {
    return client.delete(deleteRequest, RequestOptions.DEFAULT);
  }

  BulkByScrollResponse updateByQuery(UpdateByQueryRequest updateByQueryRequest)
      throws IOException, ElasticsearchException {
    return client.updateByQuery(updateByQueryRequest, RequestOptions.DEFAULT);
  }

  public SearchResponse search(SearchRequest searchRequest) throws IOException, ElasticsearchException {
    return client.search(searchRequest, RequestOptions.DEFAULT);
  }

  CreateIndexResponse createIndex(CreateIndexRequest createIndexRequest) throws IOException, ElasticsearchException {
    return client.indices().create(createIndexRequest, RequestOptions.DEFAULT);
  }

  boolean indexExists(GetIndexRequest getIndexRequest) throws IOException, ElasticsearchException {
    return client.indices().exists(getIndexRequest, RequestOptions.DEFAULT);
  }

  AcknowledgedResponse deleteIndex(DeleteIndexRequest deleteIndexRequest) throws IOException, ElasticsearchException {
    return client.indices().delete(deleteIndexRequest, RequestOptions.DEFAULT);
  }

  AcknowledgedResponse updateAliases(IndicesAliasesRequest indicesAliasesRequest)
      throws IOException, ElasticsearchException {
    return client.indices().updateAliases(indicesAliasesRequest, RequestOptions.DEFAULT);
  }
}
