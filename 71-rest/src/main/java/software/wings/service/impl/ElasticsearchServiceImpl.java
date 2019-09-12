package software.wings.service.impl;

import com.google.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.harness.persistence.PersistentEntity;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.hibernate.validator.constraints.NotBlank;
import software.wings.beans.EntityType;
import software.wings.search.entities.application.ApplicationView;
import software.wings.search.entities.pipeline.PipelineView;
import software.wings.search.framework.ElasticsearchIndexManager;
import software.wings.search.framework.SearchEntity;
import software.wings.search.framework.SearchResponse;
import software.wings.service.intfc.SearchService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ElasticsearchServiceImpl implements SearchService {
  @Inject private RestHighLevelClient client;
  @Inject private ElasticsearchIndexManager elasticsearchIndexManager;
  @Inject protected Map<Class<? extends PersistentEntity>, SearchEntity<?>> searchEntityMap;

  public software.wings.search.framework.SearchResponse getSearchResults(
      @NotBlank String searchString, @NotBlank String accountId) throws IOException {
    SearchResponse searchResponse = new SearchResponse();
    SearchHits hits = search(searchString, accountId);

    List<ApplicationView> applicationViewList = new ArrayList<>();
    List<PipelineView> pipelineViewList = new ArrayList<>();

    for (SearchHit hit : hits) {
      Map<String, Object> result = hit.getSourceAsMap();

      if (EntityType.APPLICATION.name().equals(result.get("type"))) {
        ObjectMapper mapper = new ObjectMapper();
        ApplicationView applicationView = mapper.convertValue(result, ApplicationView.class);
        applicationViewList.add(applicationView);
      }

      if (EntityType.PIPELINE.name().equals(result.get("type"))) {
        ObjectMapper mapper = new ObjectMapper();
        PipelineView pipelineView = mapper.convertValue(result, PipelineView.class);
        pipelineViewList.add(pipelineView);
      }
    }

    searchResponse.setApplications(applicationViewList);
    searchResponse.setPipelines(pipelineViewList);
    return searchResponse;
  }

  private SearchHits search(@NotBlank String searchString, @NotBlank String accountId) throws IOException {
    String[] indexNames = getIndexesToSearch();
    SearchRequest searchRequest = new SearchRequest(indexNames);
    BoolQueryBuilder boolQueryBuilder = ElasticsearchServiceImpl.createQuery(searchString, accountId);
    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder().query(boolQueryBuilder);
    searchRequest.source(searchSourceBuilder);

    org.elasticsearch.action.search.SearchResponse searchResponse =
        client.search(searchRequest, RequestOptions.DEFAULT);

    return searchResponse.getHits();
  }

  private String[] getIndexesToSearch() {
    return searchEntityMap.values()
        .stream()
        .map(searchEntity -> elasticsearchIndexManager.getIndexName(searchEntity.getType()))
        .toArray(String[] ::new);
  }

  private static BoolQueryBuilder createQuery(@NotBlank String searchString, @NotBlank String accountId) {
    BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();

    QueryBuilder queryBuilder = QueryBuilders.disMaxQuery()
                                    .add(QueryBuilders.matchPhrasePrefixQuery("name", searchString).boost(5))
                                    .add(QueryBuilders.matchPhraseQuery("description", searchString))
                                    .tieBreaker(0.7f);

    boolQueryBuilder.must(queryBuilder).filter(QueryBuilders.termQuery("accountId", accountId));
    return boolQueryBuilder;
  }
}
