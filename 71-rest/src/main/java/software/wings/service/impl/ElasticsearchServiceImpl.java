package software.wings.service.impl;

import com.google.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.hibernate.validator.constraints.NotBlank;
import software.wings.beans.EntityType;
import software.wings.search.entities.application.ApplicationView;
import software.wings.search.entities.pipeline.PipelineView;
import software.wings.search.framework.ElasticsearchUtils;
import software.wings.search.framework.SearchResponse;
import software.wings.service.intfc.SearchService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ElasticsearchServiceImpl implements SearchService {
  @Inject private RestHighLevelClient client;

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

  public SearchHits search(@NotBlank String searchString, @NotBlank String accountId) throws IOException {
    SearchRequest searchRequest = new SearchRequest();
    BoolQueryBuilder boolQueryBuilder = ElasticsearchUtils.createQuery(searchString, accountId);
    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder().query(boolQueryBuilder);
    searchRequest.source(searchSourceBuilder);

    org.elasticsearch.action.search.SearchResponse searchResponse =
        client.search(searchRequest, RequestOptions.DEFAULT);

    return searchResponse.getHits();
  }
}
