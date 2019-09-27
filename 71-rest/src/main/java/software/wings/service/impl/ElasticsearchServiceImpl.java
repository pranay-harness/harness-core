package software.wings.service.impl;

import com.google.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.harness.persistence.PersistentEntity;
import lombok.extern.slf4j.Slf4j;
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
import software.wings.search.entities.deployment.DeploymentView;
import software.wings.search.entities.deployment.DeploymentView.DeploymentViewKeys;
import software.wings.search.entities.environment.EnvironmentView;
import software.wings.search.entities.pipeline.PipelineView;
import software.wings.search.entities.service.ServiceView;
import software.wings.search.entities.workflow.WorkflowView;
import software.wings.search.framework.ElasticsearchIndexManager;
import software.wings.search.framework.EntityBaseView.EntityBaseViewKeys;
import software.wings.search.framework.SearchEntity;
import software.wings.search.framework.SearchResponse;
import software.wings.service.intfc.SearchService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class ElasticsearchServiceImpl implements SearchService {
  @Inject private RestHighLevelClient client;
  @Inject private ElasticsearchIndexManager elasticsearchIndexManager;
  @Inject protected Map<Class<? extends PersistentEntity>, SearchEntity<?>> searchEntityMap;
  private static final int MAX_RESULTS = 50;

  public SearchResponse getSearchResults(@NotBlank String searchString, @NotBlank String accountId) throws IOException {
    SearchHits hits = search(searchString, accountId);

    List<ApplicationView> applicationViewList = new ArrayList<>();
    List<PipelineView> pipelineViewList = new ArrayList<>();
    List<WorkflowView> workflowViewList = new ArrayList<>();
    List<ServiceView> serviceViewList = new ArrayList<>();
    List<EnvironmentView> environmentViewList = new ArrayList<>();
    List<DeploymentView> deploymentViewList = new ArrayList<>();
    ObjectMapper mapper = new ObjectMapper();

    for (SearchHit hit : hits) {
      Map<String, Object> result = hit.getSourceAsMap();
      switch (EntityType.valueOf(result.get(EntityBaseViewKeys.type).toString())) {
        case APPLICATION: {
          ApplicationView applicationView = mapper.convertValue(result, ApplicationView.class);
          applicationViewList.add(applicationView);
          break;
        }
        case SERVICE: {
          ServiceView serviceView = mapper.convertValue(result, ServiceView.class);
          serviceViewList.add(serviceView);
          break;
        }
        case ENVIRONMENT: {
          EnvironmentView environmentView = mapper.convertValue(result, EnvironmentView.class);
          environmentViewList.add(environmentView);
          break;
        }
        case WORKFLOW: {
          WorkflowView workflowView = mapper.convertValue(result, WorkflowView.class);
          workflowViewList.add(workflowView);
          break;
        }
        case PIPELINE: {
          PipelineView pipelineView = mapper.convertValue(result, PipelineView.class);
          pipelineViewList.add(pipelineView);
          break;
        }
        case DEPLOYMENT: {
          if (result.get(DeploymentViewKeys.workflowInPipeline) != null
              && result.get(DeploymentViewKeys.workflowInPipeline).equals(false)) {
            DeploymentView deploymentView = mapper.convertValue(result, DeploymentView.class);
            deploymentViewList.add(deploymentView);
          }
          break;
        }
        default:
      }
    }
    return SearchResponse.builder()
        .applications(applicationViewList)
        .pipelines(pipelineViewList)
        .services(serviceViewList)
        .environments(environmentViewList)
        .workflows(workflowViewList)
        .deployments(deploymentViewList)
        .build();
  }

  private SearchHits search(@NotBlank String searchString, @NotBlank String accountId) throws IOException {
    String[] indexNames = getIndexesToSearch();
    SearchRequest searchRequest = new SearchRequest(indexNames);
    BoolQueryBuilder boolQueryBuilder = createQuery(searchString, accountId);
    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder().query(boolQueryBuilder).size(MAX_RESULTS);

    searchRequest.source(searchSourceBuilder);
    org.elasticsearch.action.search.SearchResponse searchResponse =
        client.search(searchRequest, RequestOptions.DEFAULT);

    logger.info("Search results, time taken : {}, number of hits: {}, accountID: {}", searchResponse.getTook(),
        searchResponse.getHits().getTotalHits(), accountId);
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
