package software.wings.search.entities.environment;

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

public class EnvironmentElasticsearchRequestHandler
    extends AbstractElasticsearchRequestHandler implements ElasticsearchRequestHandler {
  @Inject @Named(AuditTrailFeature.FEATURE_NAME) private PremiumFeature auditTrailFeature;

  @Override
  public List<SearchResult> translateHitsToSearchResults(SearchHits searchHits, String accountId) {
    boolean includeAudits = auditTrailFeature.isAvailableForAccount(accountId);
    ObjectMapper mapper = new ObjectMapper();
    List<SearchResult> searchResults = new ArrayList<>();
    for (SearchHit hit : searchHits) {
      Map<String, Object> result = hit.getSourceAsMap();
      EnvironmentView environmentView = mapper.convertValue(result, EnvironmentView.class);
      EnvironmentSearchResult environmentSearchResult =
          new EnvironmentSearchResult(environmentView, includeAudits, hit.getScore());
      searchResults.add(environmentSearchResult);
    }
    return searchResults;
  }

  public List<SearchResult> filterSearchResults(List<SearchResult> searchResults) {
    List<SearchResult> newSearchResults = new ArrayList<>();
    UserPermissionInfo userPermissionInfo = SearchPermissionUtils.getUserPermission();
    for (SearchResult searchResult : searchResults) {
      EnvironmentSearchResult envSearchResult = (EnvironmentSearchResult) searchResult;
      AppPermissionSummary appPermission =
          userPermissionInfo.getAppPermissionMapInternal().get(envSearchResult.getAppId());
      if (!checkPermission(appPermission, envSearchResult)) {
        continue;
      }

      envSearchResult.setWorkflows(SearchPermissionUtils.getAllowedEntities(
          envSearchResult.getWorkflows(), SearchPermissionUtils.getAllowedWorkflowIds(appPermission)));
      envSearchResult.setPipelines(SearchPermissionUtils.getAllowedEntities(
          envSearchResult.getPipelines(), SearchPermissionUtils.getAllowedPiplineIds(appPermission)));
      if (!SearchPermissionUtils.hasAuditPermissions(userPermissionInfo)) {
        envSearchResult.setAudits(new ArrayList<>());
      }
      envSearchResult.setDeployments(SearchPermissionUtils.getAllowedDeployments(
          envSearchResult.getDeployments(), SearchPermissionUtils.getAllowedDeploymentIds(appPermission)));

      newSearchResults.add(envSearchResult);
    }
    return newSearchResults;
  }

  private boolean checkPermission(AppPermissionSummary appPermission, EnvironmentSearchResult envSearchResult) {
    return appPermission != null
        && SearchPermissionUtils.getAllowedEnvironmentIds(appPermission).contains(envSearchResult.getId());
  }
}
