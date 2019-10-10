package software.wings.search.framework;

import com.google.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import software.wings.app.MainConfiguration;

import java.io.IOException;

/**
 * Class for managing elasticsearch indexes
 *
 * @author utkarsh
 */
@Slf4j
public class ElasticsearchIndexManager {
  @Inject MainConfiguration mainConfiguration;
  @Inject RestHighLevelClient restHighLevelClient;

  public String getIndexName(String type) {
    String indexSuffix = mainConfiguration.getElasticsearchConfig().getIndexSuffix();
    return type.concat(indexSuffix);
  }

  boolean isIndexPresent(String type) throws IOException {
    String indexName = getIndexName(type);
    GetIndexRequest getIndexRequest = new GetIndexRequest(indexName);
    return restHighLevelClient.indices().exists(getIndexRequest, RequestOptions.DEFAULT);
  }

  public CreateIndexResponse createIndex(String type, String configuration) throws IOException {
    String indexName = getIndexName(type);
    CreateIndexRequest createIndexRequest = new CreateIndexRequest(indexName);
    createIndexRequest.source(configuration, XContentType.JSON);
    return restHighLevelClient.indices().create(createIndexRequest, RequestOptions.DEFAULT);
  }

  AcknowledgedResponse deleteIndex(String type) throws IOException {
    String indexName = getIndexName(type);
    DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest(indexName);
    return restHighLevelClient.indices().delete(deleteIndexRequest, RequestOptions.DEFAULT);
  }
}
