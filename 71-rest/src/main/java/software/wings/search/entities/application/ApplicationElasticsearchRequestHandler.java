package software.wings.search.entities.application;

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

public class ApplicationElasticsearchRequestHandler
    extends AbstractElasticsearchRequestHandler implements ElasticsearchRequestHandler {
  @Inject @Named(AuditTrailFeature.FEATURE_NAME) private PremiumFeature auditTrailFeature;

  @Override
  public List<SearchResult> translateHitsToSearchResults(SearchHits searchHits, String accountId) {
    boolean includeAudits = auditTrailFeature.isAvailableForAccount(accountId);
    ObjectMapper mapper = new ObjectMapper();
    List<SearchResult> searchResults = new ArrayList<>();
    for (SearchHit hit : searchHits) {
      Map<String, Object> result = hit.getSourceAsMap();
      ApplicationView applicationView = mapper.convertValue(result, ApplicationView.class);
      ApplicationSearchResult applicationSearchResult =
          new ApplicationSearchResult(applicationView, includeAudits, hit.getScore());
      searchResults.add(applicationSearchResult);
    }
    return searchResults;
  }

  @Override
  public List<SearchResult> filterSearchResults(List<SearchResult> searchResults) {
    List<SearchResult> newSearchResults = new ArrayList<>();
    UserPermissionInfo userPermissionInfo = SearchPermissionUtils.getUserPermission();
    for (SearchResult searchResult : searchResults) {
      AppPermissionSummary appPermission = userPermissionInfo.getAppPermissionMapInternal().get(searchResult.getId());
      if (appPermission == null) {
        continue;
      }

      ApplicationSearchResult applicationSearchResult = (ApplicationSearchResult) searchResult;

      applicationSearchResult.setWorkflows(SearchPermissionUtils.getAllowedEntities(
          applicationSearchResult.getWorkflows(), SearchPermissionUtils.getAllowedWorkflowIds(appPermission)));
      applicationSearchResult.setPipelines(SearchPermissionUtils.getAllowedEntities(
          applicationSearchResult.getPipelines(), SearchPermissionUtils.getAllowedPiplineIds(appPermission)));
      applicationSearchResult.setServices(SearchPermissionUtils.getAllowedEntities(
          applicationSearchResult.getServices(), SearchPermissionUtils.getAllowedServiceIds(appPermission)));
      applicationSearchResult.setEnvironments(SearchPermissionUtils.getAllowedEntities(
          applicationSearchResult.getEnvironments(), SearchPermissionUtils.getAllowedEnvironmentIds(appPermission)));
      if (!SearchPermissionUtils.hasAuditPermissions(userPermissionInfo)) {
        applicationSearchResult.setAudits(new ArrayList<>());
      }

      newSearchResults.add(applicationSearchResult);
    }
    return newSearchResults;
  }
}
