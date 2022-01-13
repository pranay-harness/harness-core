/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.search.entities.workflow;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.features.AuditTrailFeature;
import software.wings.features.api.PremiumFeature;
import software.wings.search.SearchPermissionUtils;
import software.wings.search.framework.AbstractElasticsearchRequestHandler;
import software.wings.search.framework.ElasticsearchRequestHandler;
import software.wings.search.framework.SearchResult;
import software.wings.security.AppPermissionSummary;
import software.wings.security.UserPermissionInfo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;

@OwnedBy(PL)
public class WorkflowElasticsearchRequestHandler
    extends AbstractElasticsearchRequestHandler implements ElasticsearchRequestHandler {
  @Inject @Named(AuditTrailFeature.FEATURE_NAME) private PremiumFeature auditTrailFeature;

  @Override
  public List<SearchResult> translateHitsToSearchResults(SearchHits searchHits, String accountId) {
    boolean includeAudits = auditTrailFeature.isAvailableForAccount(accountId);
    ObjectMapper mapper = new ObjectMapper();
    List<SearchResult> searchResults = new ArrayList<>();
    for (SearchHit hit : searchHits) {
      Map<String, Object> result = hit.getSourceAsMap();
      WorkflowView workflowView = mapper.convertValue(result, WorkflowView.class);
      WorkflowSearchResult workflowSearchResult = new WorkflowSearchResult(workflowView, includeAudits, hit.getScore());
      searchResults.add(workflowSearchResult);
    }
    return searchResults;
  }

  @Override
  public List<SearchResult> filterSearchResults(List<SearchResult> searchResults) {
    List<SearchResult> newSearchResults = new ArrayList<>();
    UserPermissionInfo userPermissionInfo = SearchPermissionUtils.getUserPermission();
    for (SearchResult searchResult : searchResults) {
      WorkflowSearchResult workflowSearchResult = (WorkflowSearchResult) searchResult;
      AppPermissionSummary appPermission =
          userPermissionInfo.getAppPermissionMapInternal().get(workflowSearchResult.getAppId());
      if (!checkPermission(appPermission, workflowSearchResult)) {
        continue;
      }

      workflowSearchResult.setPipelines(SearchPermissionUtils.getAllowedEntities(
          workflowSearchResult.getPipelines(), SearchPermissionUtils.getAllowedPiplineIds(appPermission)));
      workflowSearchResult.setServices(SearchPermissionUtils.getAllowedEntities(
          workflowSearchResult.getServices(), SearchPermissionUtils.getAllowedServiceIds(appPermission)));
      if (!SearchPermissionUtils.hasAuditPermissions(userPermissionInfo)) {
        workflowSearchResult.setAudits(new ArrayList<>());
      }
      if (!SearchPermissionUtils.hasDeploymentPermissions(
              SearchPermissionUtils.getAllowedDeploymentIds(appPermission), workflowSearchResult.getId())) {
        workflowSearchResult.setDeployments(new ArrayList<>());
      }

      newSearchResults.add(workflowSearchResult);
    }
    return newSearchResults;
  }

  private boolean checkPermission(AppPermissionSummary appPermission, WorkflowSearchResult workflowSearchResult) {
    return appPermission != null
        && SearchPermissionUtils.getAllowedWorkflowIds(appPermission).contains(workflowSearchResult.getId());
  }
}
