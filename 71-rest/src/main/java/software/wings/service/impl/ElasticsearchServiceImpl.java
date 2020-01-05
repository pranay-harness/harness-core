package software.wings.service.impl;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.MultiSearchRequest;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.hibernate.validator.constraints.NotBlank;
import software.wings.beans.EntityType;
import software.wings.features.AuditTrailFeature;
import software.wings.features.api.PremiumFeature;
import software.wings.search.entities.application.ApplicationSearchEntity;
import software.wings.search.entities.application.ApplicationSearchResult;
import software.wings.search.entities.application.ApplicationView;
import software.wings.search.entities.deployment.DeploymentSearchEntity;
import software.wings.search.entities.deployment.DeploymentSearchResult;
import software.wings.search.entities.deployment.DeploymentView;
import software.wings.search.entities.deployment.DeploymentView.DeploymentViewKeys;
import software.wings.search.entities.environment.EnvironmentSearchEntity;
import software.wings.search.entities.environment.EnvironmentSearchResult;
import software.wings.search.entities.environment.EnvironmentView;
import software.wings.search.entities.pipeline.PipelineSearchEntity;
import software.wings.search.entities.pipeline.PipelineSearchResult;
import software.wings.search.entities.pipeline.PipelineView;
import software.wings.search.entities.service.ServiceSearchEntity;
import software.wings.search.entities.service.ServiceSearchResult;
import software.wings.search.entities.service.ServiceView;
import software.wings.search.entities.workflow.WorkflowSearchEntity;
import software.wings.search.entities.workflow.WorkflowSearchResult;
import software.wings.search.entities.workflow.WorkflowView;
import software.wings.search.framework.ElasticsearchClient;
import software.wings.search.framework.ElasticsearchIndexManager;
import software.wings.search.framework.EntityBaseView.EntityBaseViewKeys;
import software.wings.search.framework.SearchEntity;
import software.wings.search.framework.SearchResult;
import software.wings.search.framework.SearchResults;
import software.wings.service.intfc.SearchService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
public class ElasticsearchServiceImpl implements SearchService {
  @Inject private ElasticsearchClient elasticsearchClient;
  @Inject @Named(AuditTrailFeature.FEATURE_NAME) private PremiumFeature auditTrailFeature;
  @Inject private ElasticsearchIndexManager elasticsearchIndexManager;
  @Inject private Set<SearchEntity<?>> searchEntities;
  private static final int MAX_RESULTS_PER_ENTITY = 20;
  private static final int BOOST_VALUE = 15;
  private static final int SLOP_DISTANCE_VALUE = 10;

  @Override
  public SearchResults getSearchResults(@NotBlank String searchString, @NotBlank String accountId) throws IOException {
    List<SearchHits> searchHitsList = search(searchString, accountId);
    LinkedHashMap<String, List<SearchResult>> searchResult = new LinkedHashMap<>();

    List<SearchResult> applicationSearchResults = new ArrayList<>();
    List<SearchResult> pipelineSearchResults = new ArrayList<>();
    List<SearchResult> workflowSearchResults = new ArrayList<>();
    List<SearchResult> serviceSearchResults = new ArrayList<>();
    List<SearchResult> environmentSearchResults = new ArrayList<>();
    List<SearchResult> deploymentSearchResults = new ArrayList<>();
    ObjectMapper mapper = new ObjectMapper();

    boolean includeAudits = auditTrailFeature.isAvailableForAccount(accountId);
    for (SearchHits hits : searchHitsList) {
      for (SearchHit hit : hits) {
        Map<String, Object> result = hit.getSourceAsMap();
        switch (EntityType.valueOf(result.get(EntityBaseViewKeys.type).toString())) {
          case APPLICATION:
            ApplicationView applicationView = mapper.convertValue(result, ApplicationView.class);
            ApplicationSearchResult applicationSearchResult =
                new ApplicationSearchResult(applicationView, includeAudits, hit.getScore());
            applicationSearchResults.add(applicationSearchResult);
            break;
          case SERVICE:
            ServiceView serviceView = mapper.convertValue(result, ServiceView.class);
            ServiceSearchResult serviceSearchResult =
                new ServiceSearchResult(serviceView, includeAudits, hit.getScore());
            serviceSearchResults.add(serviceSearchResult);
            break;
          case ENVIRONMENT:
            EnvironmentView environmentView = mapper.convertValue(result, EnvironmentView.class);
            EnvironmentSearchResult environmentSearchResult =
                new EnvironmentSearchResult(environmentView, includeAudits, hit.getScore());
            environmentSearchResults.add(environmentSearchResult);
            break;
          case WORKFLOW:
            WorkflowView workflowView = mapper.convertValue(result, WorkflowView.class);
            WorkflowSearchResult workflowSearchResult =
                new WorkflowSearchResult(workflowView, includeAudits, hit.getScore());
            workflowSearchResults.add(workflowSearchResult);
            break;
          case PIPELINE:
            PipelineView pipelineView = mapper.convertValue(result, PipelineView.class);
            PipelineSearchResult pipelineSearchResult =
                new PipelineSearchResult(pipelineView, includeAudits, hit.getScore());
            pipelineSearchResults.add(pipelineSearchResult);
            break;
          case DEPLOYMENT:
            DeploymentView deploymentView = mapper.convertValue(result, DeploymentView.class);
            DeploymentSearchResult deploymentSearchResult = new DeploymentSearchResult(deploymentView, hit.getScore());
            deploymentSearchResults.add(deploymentSearchResult);
            break;
          default:
        }
      }
    }

    searchResult.put(ApplicationSearchEntity.TYPE, applicationSearchResults);
    searchResult.put(ServiceSearchEntity.TYPE, serviceSearchResults);
    searchResult.put(EnvironmentSearchEntity.TYPE, environmentSearchResults);
    searchResult.put(WorkflowSearchEntity.TYPE, workflowSearchResults);
    searchResult.put(PipelineSearchEntity.TYPE, pipelineSearchResults);
    sortDeploymentsWithSameNameByCreatedAtValue(deploymentSearchResults);
    searchResult.put(DeploymentSearchEntity.TYPE, deploymentSearchResults);

    return new SearchResults(searchResult);
  }

  private MultiSearchRequest getSearchRequest(@NotBlank String searchString, @NotBlank String accountId) {
    MultiSearchRequest multiSearchRequest = new MultiSearchRequest();
    Iterator<SearchEntity<?>> iterator = searchEntities.iterator();

    while (iterator.hasNext()) {
      SearchEntity searchEntity = iterator.next();
      String indexName = elasticsearchIndexManager.getAliasName(searchEntity.getType());
      SearchRequest searchRequest = new SearchRequest(indexName);
      BoolQueryBuilder boolQueryBuilder;
      if (!searchEntity.getType().equals(DeploymentSearchEntity.TYPE)) {
        boolQueryBuilder = createQuery(searchString, accountId);
      } else {
        boolQueryBuilder = createDeploymentQuery(searchString, accountId);
      }
      SearchSourceBuilder searchSourceBuilder =
          new SearchSourceBuilder().query(boolQueryBuilder).size(MAX_RESULTS_PER_ENTITY);
      searchRequest.source(searchSourceBuilder);
      searchRequest.indicesOptions(IndicesOptions.LENIENT_EXPAND_OPEN);
      multiSearchRequest.add(searchRequest);
    }
    return multiSearchRequest;
  }

  private List<SearchHits> search(@NotBlank String searchString, @NotBlank String accountId) throws IOException {
    MultiSearchResponse multiSearchResponse =
        elasticsearchClient.multiSearch(getSearchRequest(searchString, accountId));
    List<SearchHits> searchHits = new ArrayList<>();
    long totalHits = 0;
    for (MultiSearchResponse.Item item : multiSearchResponse.getResponses()) {
      if (item.getResponse() != null && item.getResponse().getHits() != null
          && item.getResponse().getHits().getTotalHits() != null) {
        totalHits += item.getResponse().getHits().getTotalHits().value;
        searchHits.add(item.getResponse().getHits());
      }
    }
    logger.info("Search results, time taken : {}, number of hits: {}, accountID: {}", multiSearchResponse.getTook(),
        totalHits, accountId);
    return searchHits;
  }

  private static BoolQueryBuilder createQuery(@NotBlank String searchString, @NotBlank String accountId) {
    BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
    QueryBuilder queryBuilder =
        QueryBuilders.disMaxQuery()
            .add(QueryBuilders.matchQuery(EntityBaseViewKeys.name, searchString)
                     .operator(Operator.AND)
                     .boost(BOOST_VALUE))
            .add(QueryBuilders.matchPhrasePrefixQuery(EntityBaseViewKeys.description, searchString)
                     .slop(SLOP_DISTANCE_VALUE))
            .tieBreaker(0.0f);
    boolQueryBuilder.must(queryBuilder).filter(QueryBuilders.termQuery(EntityBaseViewKeys.accountId, accountId));
    return boolQueryBuilder;
  }

  private static BoolQueryBuilder createDeploymentQuery(@NotBlank String searchString, @NotBlank String accountId) {
    return createQuery(searchString, accountId)
        .filter(QueryBuilders.termQuery(DeploymentViewKeys.workflowInPipeline, false));
  }

  private static int getLastIndexWithSameDeploymentName(List<SearchResult> searchResults, int startIndex) {
    int endIndex = startIndex + 1;

    while (endIndex < searchResults.size()
        && searchResults.get(endIndex).getName().equals(searchResults.get(startIndex).getName())) {
      endIndex++;
    }
    return endIndex;
  }

  private static void sortInterval(List<SearchResult> searchResults) {
    searchResults.sort(Comparator.comparingLong(SearchResult::getCreatedAt).reversed());
  }

  private static void sortDeploymentsWithSameNameByCreatedAtValue(List<SearchResult> searchResults) {
    int start = 0;
    while (start < searchResults.size()) {
      int end = getLastIndexWithSameDeploymentName(searchResults, start);
      if (end > start) {
        sortInterval(searchResults.subList(start, end));
      }
      start = end;
    }
  }
}
