/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.search.entities.deployment;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.search.SearchPermissionUtils;
import software.wings.search.entities.deployment.DeploymentView.DeploymentViewKeys;
import software.wings.search.framework.AbstractElasticsearchRequestHandler;
import software.wings.search.framework.ElasticsearchRequestHandler;
import software.wings.search.framework.SearchResult;
import software.wings.security.AppPermissionSummary;
import software.wings.security.UserPermissionInfo;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;

@OwnedBy(PL)
public class DeploymentElasticsearchRequestHandler
    extends AbstractElasticsearchRequestHandler implements ElasticsearchRequestHandler {
  @Override
  public BoolQueryBuilder createQuery(String searchString, String accountId) {
    return super.createQuery(searchString, accountId)
        .filter(QueryBuilders.termQuery(DeploymentViewKeys.workflowInPipeline, false));
  }

  @Override
  public List<SearchResult> processSearchResults(List<SearchResult> searchResults) {
    int start = 0;
    while (start < searchResults.size()) {
      int end = getLastIndexWithSameDeploymentName(searchResults, start);
      if (end > start) {
        sortInterval(searchResults.subList(start, end));
      }
      start = end;
    }
    return searchResults;
  }

  @Override
  public List<SearchResult> translateHitsToSearchResults(SearchHits searchHits, String accountId) {
    ObjectMapper mapper = new ObjectMapper();
    List<SearchResult> searchResults = new ArrayList<>();
    for (SearchHit hit : searchHits) {
      Map<String, Object> result = hit.getSourceAsMap();
      DeploymentView deploymentView = mapper.convertValue(result, DeploymentView.class);
      DeploymentSearchResult deploymentSearchResult = new DeploymentSearchResult(deploymentView, hit.getScore());
      searchResults.add(deploymentSearchResult);
    }
    return searchResults;
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

  @Override
  public List<SearchResult> filterSearchResults(List<SearchResult> searchResults) {
    List<SearchResult> newSearchResults = new ArrayList<>();
    UserPermissionInfo userPermissionInfo = SearchPermissionUtils.getUserPermission();

    for (SearchResult searchResult : searchResults) {
      DeploymentSearchResult deploymentSearchResult = (DeploymentSearchResult) searchResult;
      AppPermissionSummary appPermission =
          userPermissionInfo.getAppPermissionMapInternal().get(deploymentSearchResult.getAppId());

      if (!checkPermission(appPermission, deploymentSearchResult)) {
        continue;
      }

      deploymentSearchResult.setServices(SearchPermissionUtils.getAllowedEntities(
          deploymentSearchResult.getServices(), SearchPermissionUtils.getAllowedServiceIds(appPermission)));
      deploymentSearchResult.setWorkflows(SearchPermissionUtils.getAllowedEntities(
          deploymentSearchResult.getWorkflows(), SearchPermissionUtils.getAllowedWorkflowIds(appPermission)));
      deploymentSearchResult.setEnvironments(SearchPermissionUtils.getAllowedEntities(
          deploymentSearchResult.getEnvironments(), SearchPermissionUtils.getAllowedEnvironmentIds(appPermission)));

      newSearchResults.add(deploymentSearchResult);
    }

    return newSearchResults;
  }

  private boolean checkPermission(AppPermissionSummary appPermission, DeploymentSearchResult deploymentSearchResult) {
    String workflowPipelineId = null;
    if (deploymentSearchResult.getWorkflowId() != null) {
      workflowPipelineId = deploymentSearchResult.getWorkflowId();
    }
    if (deploymentSearchResult.getPipelineId() != null) {
      workflowPipelineId = deploymentSearchResult.getPipelineId();
    }
    return appPermission != null
        && SearchPermissionUtils.getAllowedDeploymentIds(appPermission).contains(workflowPipelineId);
  }
}
