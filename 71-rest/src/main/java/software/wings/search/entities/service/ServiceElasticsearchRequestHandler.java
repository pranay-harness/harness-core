package software.wings.search.entities.service;

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

public class ServiceElasticsearchRequestHandler
    extends AbstractElasticsearchRequestHandler implements ElasticsearchRequestHandler {
  @Inject @Named(AuditTrailFeature.FEATURE_NAME) private PremiumFeature auditTrailFeature;

  @Override
  public List<SearchResult> translateHitsToSearchResults(SearchHits searchHits, String accountId) {
    boolean includeAudits = auditTrailFeature.isAvailableForAccount(accountId);
    ObjectMapper mapper = new ObjectMapper();
    List<SearchResult> searchResults = new ArrayList<>();
    for (SearchHit hit : searchHits) {
      Map<String, Object> result = hit.getSourceAsMap();
      ServiceView serviceView = mapper.convertValue(result, ServiceView.class);
      ServiceSearchResult serviceSearchResult = new ServiceSearchResult(serviceView, includeAudits, hit.getScore());
      searchResults.add(serviceSearchResult);
    }
    return searchResults;
  }

  @Override
  public List<SearchResult> filterSearchResults(List<SearchResult> searchResults) {
    List<SearchResult> newSearchResults = new ArrayList<>();
    UserPermissionInfo userPermissionInfo = SearchPermissionUtils.getUserPermission();
    for (SearchResult searchResult : searchResults) {
      ServiceSearchResult serviceSearchResult = (ServiceSearchResult) searchResult;
      AppPermissionSummary appPermission =
          userPermissionInfo.getAppPermissionMapInternal().get(serviceSearchResult.getAppId());
      if (!checkPermission(appPermission, serviceSearchResult)) {
        continue;
      }

      serviceSearchResult.setWorkflows(SearchPermissionUtils.getAllowedEntities(
          serviceSearchResult.getWorkflows(), SearchPermissionUtils.getAllowedWorkflowIds(appPermission)));
      serviceSearchResult.setPipelines(SearchPermissionUtils.getAllowedEntities(
          serviceSearchResult.getPipelines(), SearchPermissionUtils.getAllowedPiplineIds(appPermission)));
      if (!SearchPermissionUtils.hasAuditPermissions(userPermissionInfo)) {
        serviceSearchResult.setAudits(new ArrayList<>());
      }
      serviceSearchResult.setDeployments(SearchPermissionUtils.getAllowedDeployments(
          serviceSearchResult.getDeployments(), SearchPermissionUtils.getAllowedDeploymentIds(appPermission)));

      newSearchResults.add(serviceSearchResult);
    }
    return newSearchResults;
  }

  private boolean checkPermission(AppPermissionSummary appPermission, ServiceSearchResult serviceSearchResult) {
    return appPermission != null
        && SearchPermissionUtils.getAllowedServiceIds(appPermission).contains(serviceSearchResult.getId());
  }
}
