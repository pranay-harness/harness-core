package software.wings.search.entities.pipeline;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import software.wings.features.AuditTrailFeature;
import software.wings.features.api.PremiumFeature;
import software.wings.search.SearchPermissionUtils;
import software.wings.search.framework.AbstractElasticsearchRequestHandler;
import software.wings.search.framework.ElasticsearchRequestHandler;
import software.wings.search.framework.SearchResult;
import software.wings.security.AppPermissionSummary;
import software.wings.security.UserPermissionInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PipelineElasticsearchRequestHandler
    extends AbstractElasticsearchRequestHandler implements ElasticsearchRequestHandler {
  @Inject @Named(AuditTrailFeature.FEATURE_NAME) private PremiumFeature auditTrailFeature;

  @Override
  public List<SearchResult> translateHitsToSearchResults(SearchHits searchHits, String accountId) {
    boolean includeAudits = auditTrailFeature.isAvailableForAccount(accountId);
    ObjectMapper mapper = new ObjectMapper();
    List<SearchResult> searchResults = new ArrayList<>();
    for (SearchHit hit : searchHits) {
      Map<String, Object> result = hit.getSourceAsMap();
      PipelineView pipelineView = mapper.convertValue(result, PipelineView.class);
      PipelineSearchResult pipelineSearchResult = new PipelineSearchResult(pipelineView, includeAudits, hit.getScore());
      searchResults.add(pipelineSearchResult);
    }
    return searchResults;
  }

  public List<SearchResult> filterSearchResults(List<SearchResult> searchResults) {
    List<SearchResult> newSearchResults = new ArrayList<>();
    UserPermissionInfo userPermissionInfo = SearchPermissionUtils.getUserPermission();
    for (SearchResult searchResult : searchResults) {
      PipelineSearchResult pipelineSearchResult = (PipelineSearchResult) searchResult;
      AppPermissionSummary appPermission =
          userPermissionInfo.getAppPermissionMapInternal().get(pipelineSearchResult.getAppId());
      if (!checkPermission(appPermission, pipelineSearchResult)) {
        continue;
      }

      pipelineSearchResult.setWorkflows(SearchPermissionUtils.getAllowedEntities(
          pipelineSearchResult.getWorkflows(), SearchPermissionUtils.getAllowedWorkflowIds(appPermission)));
      pipelineSearchResult.setServices(SearchPermissionUtils.getAllowedEntities(
          pipelineSearchResult.getServices(), SearchPermissionUtils.getAllowedServiceIds(appPermission)));
      if (!SearchPermissionUtils.hasAuditPermissions(userPermissionInfo)) {
        pipelineSearchResult.setAudits(new ArrayList<>());
      }
      if (!SearchPermissionUtils.hasDeploymentPermissions(
              SearchPermissionUtils.getAllowedDeploymentIds(appPermission), pipelineSearchResult.getId())) {
        pipelineSearchResult.setDeployments(new ArrayList<>());
      }

      newSearchResults.add(pipelineSearchResult);
    }
    return newSearchResults;
  }

  private boolean checkPermission(AppPermissionSummary appPermission, PipelineSearchResult pipelineSearchResult) {
    return appPermission != null
        && SearchPermissionUtils.getAllowedPiplineIds(appPermission).contains(pipelineSearchResult.getId());
  }
}
