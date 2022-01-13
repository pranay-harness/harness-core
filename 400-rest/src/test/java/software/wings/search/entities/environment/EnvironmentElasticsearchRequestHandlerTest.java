/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.search.entities.environment;

import static io.harness.rule.OwnerRule.MOHIT;
import static io.harness.rule.OwnerRule.UTKARSH;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.harness.beans.WorkflowType;
import io.harness.category.element.UnitTests;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.AccountType;
import software.wings.beans.EntityType;
import software.wings.beans.User;
import software.wings.beans.WorkflowExecution;
import software.wings.events.TestUtils;
import software.wings.features.AuditTrailFeature;
import software.wings.features.api.PremiumFeature;
import software.wings.search.SearchRequestHandlerTestUtils;
import software.wings.search.entities.related.audit.RelatedAuditView;
import software.wings.search.entities.related.deployment.RelatedDeploymentView;
import software.wings.search.framework.EntityInfo;
import software.wings.search.framework.SearchResult;
import software.wings.security.AccountPermissionSummary;
import software.wings.security.AppPermissionSummary;
import software.wings.security.PermissionAttribute;
import software.wings.security.UserPermissionInfo;
import software.wings.security.UserRequestContext;
import software.wings.security.UserThreadLocal;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.elasticsearch.action.search.SearchResponse;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class EnvironmentElasticsearchRequestHandlerTest extends WingsBaseTest {
  @Mock @Named(AuditTrailFeature.FEATURE_NAME) PremiumFeature auditTrailFeature;
  @Inject @InjectMocks EnvironmentElasticsearchRequestHandler environmentSearchRequestHandler;
  @Inject private TestUtils eventTestHelper;
  @Inject private HPersistence persistence;

  protected String accountId;
  protected String appId1;
  protected String appId2;
  protected String workflowId1;
  protected String serviceId1;
  protected String pipelineId1;
  protected String envId1;
  protected String workflowId2;
  protected String serviceId2;
  protected String pipelineId2;
  protected String envId2;
  protected String workflowId3;
  protected String serviceId3;
  protected String pipelineId3;
  protected String envId3;
  protected User user;
  protected Account account;

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  @Ignore("Investigate to make sure Search Unit Tests are not creating system resources such as Threads")
  public void translateHitsToSearchResultsTest() {
    Account account = getAccount(AccountType.PAID);
    String accountId = persistence.save(account);
    SearchResponse searchResponse = SearchRequestHandlerTestUtils.getSearchResponse(EnvironmentSearchEntity.TYPE);
    when(auditTrailFeature.isAvailableForAccount(accountId)).thenReturn(true);

    List<SearchResult> searchResults =
        environmentSearchRequestHandler.translateHitsToSearchResults(searchResponse.getHits(), accountId);
    assertThat(searchResults).isNotNull();
    assertThat(searchResults.size()).isEqualTo(1);
    assertThat(searchResults.get(0).getType()).isEqualTo(EntityType.ENVIRONMENT);
  }

  private List<AppPermissionSummary> buildAppPermissionSummary() {
    Map<PermissionAttribute.Action, Set<String>> workflowPermissions1 =
        new HashMap<PermissionAttribute.Action, Set<String>>() {
          { put(PermissionAttribute.Action.READ, Sets.newHashSet(workflowId1)); }
        };
    Map<PermissionAttribute.Action, Set<String>> workflowPermissions2 =
        new HashMap<PermissionAttribute.Action, Set<String>>() {
          { put(PermissionAttribute.Action.READ, Sets.newHashSet(workflowId3)); }
        };

    Map<PermissionAttribute.Action, Set<String>> pipelinePermissions1 =
        new HashMap<PermissionAttribute.Action, Set<String>>() {
          { put(PermissionAttribute.Action.READ, Sets.newHashSet(pipelineId1)); }
        };
    Map<PermissionAttribute.Action, Set<String>> pipelinePermissions2 =
        new HashMap<PermissionAttribute.Action, Set<String>>() {
          { put(PermissionAttribute.Action.READ, Sets.newHashSet(pipelineId3)); }
        };

    Map<PermissionAttribute.Action, Set<AppPermissionSummary.EnvInfo>> envPermissions =
        new HashMap<PermissionAttribute.Action, Set<AppPermissionSummary.EnvInfo>>() {
          {
            put(PermissionAttribute.Action.READ,
                Sets.newHashSet(AppPermissionSummary.EnvInfo.builder().envId(envId1).envType("PROD").build()));
          }
        };

    Map<PermissionAttribute.Action, Set<String>> deploymentPermissions =
        new HashMap<PermissionAttribute.Action, Set<String>>() {
          { put(PermissionAttribute.Action.READ, Sets.newHashSet(workflowId1)); }
        };

    return Arrays.asList(AppPermissionSummary.builder()
                             .deploymentPermissions(deploymentPermissions)
                             .workflowPermissions(workflowPermissions1)
                             .envPermissions(envPermissions)
                             .pipelinePermissions(pipelinePermissions1)
                             .build(),
        AppPermissionSummary.builder()
            .workflowPermissions(workflowPermissions2)
            .pipelinePermissions(pipelinePermissions2)
            .build());
  }

  private void setup() {
    account = eventTestHelper.createAccount();
    user = eventTestHelper.createUser(account);
    accountId = UUID.randomUUID().toString();
    appId1 = UUID.randomUUID().toString();
    appId2 = UUID.randomUUID().toString();
    workflowId1 = UUID.randomUUID().toString();
    serviceId1 = UUID.randomUUID().toString();
    pipelineId1 = UUID.randomUUID().toString();
    envId1 = UUID.randomUUID().toString();
    workflowId2 = UUID.randomUUID().toString();
    serviceId2 = UUID.randomUUID().toString();
    pipelineId2 = UUID.randomUUID().toString();
    envId2 = UUID.randomUUID().toString();
    workflowId3 = UUID.randomUUID().toString();
    serviceId3 = UUID.randomUUID().toString();
    pipelineId3 = UUID.randomUUID().toString();
    envId3 = UUID.randomUUID().toString();
  }

  private List<SearchResult> getSearchResult() {
    EnvironmentSearchResult searchResult1 = new EnvironmentSearchResult();
    EnvironmentSearchResult searchResult2 = new EnvironmentSearchResult();

    searchResult1.setAppId(appId1);
    searchResult1.setAccountId(accountId);
    searchResult1.setId(envId1);
    searchResult1.setType(EntityType.ENVIRONMENT);
    searchResult2.setAppId(appId2);
    searchResult2.setAccountId(accountId);
    searchResult2.setId(envId2);
    searchResult2.setType(EntityType.ENVIRONMENT);

    EntityInfo workflow1 = new EntityInfo(workflowId1, "workflow1");
    EntityInfo workflow2 = new EntityInfo(workflowId2, "workflow2");
    EntityInfo workflow3 = new EntityInfo(workflowId3, "workflow3");
    searchResult1.setWorkflows(new HashSet<>(Arrays.asList(workflow1, workflow2)));
    searchResult2.setWorkflows(new HashSet<>(Collections.singletonList(workflow3)));

    EntityInfo pipeline1 = new EntityInfo(pipelineId1, "pipeline1");
    EntityInfo pipeline2 = new EntityInfo(pipelineId2, "pipeline2");
    EntityInfo pipeline3 = new EntityInfo(pipelineId3, "pipeline3");
    searchResult1.setPipelines(new HashSet<>(Arrays.asList(pipeline1, pipeline2)));
    searchResult2.setPipelines(new HashSet<>(Collections.singletonList(pipeline3)));

    RelatedAuditView audit1 = new RelatedAuditView();
    RelatedAuditView audit2 = new RelatedAuditView();
    searchResult1.setAudits(Arrays.asList(audit1, audit2));

    WorkflowExecution workflowExecution1 =
        WorkflowExecution.builder().workflowId(workflowId1).workflowType(WorkflowType.ORCHESTRATION).build();
    WorkflowExecution workflowExecution2 =
        WorkflowExecution.builder().workflowId(pipelineId2).workflowType(WorkflowType.PIPELINE).build();
    RelatedDeploymentView deployment1 = new RelatedDeploymentView(workflowExecution1);
    RelatedDeploymentView deployment2 = new RelatedDeploymentView(workflowExecution2);
    searchResult1.setDeployments(Arrays.asList(deployment1, deployment2));

    return Arrays.asList(searchResult1, searchResult2);
  }

  @Test
  @Owner(developers = MOHIT)
  @Category(UnitTests.class)
  @Ignore("Investigate to make sure Search Unit Tests are not creating system resources such as Threads")
  public void filterSearchResultsTest() {
    setup();
    UserRequestContext userRequestContext = UserRequestContext.builder().accountId(accountId).build();
    List<AppPermissionSummary> appPermissionSummaries = buildAppPermissionSummary();
    Set<PermissionAttribute.PermissionType> permissions = new HashSet<>();
    permissions.add(PermissionAttribute.PermissionType.AUDIT_VIEWER);
    AccountPermissionSummary accountPermissionSummary =
        AccountPermissionSummary.builder().permissions(permissions).build();
    UserPermissionInfo userPermissionInfo = UserPermissionInfo.builder()
                                                .accountId(accountId)
                                                .appPermissionMapInternal(new HashMap<String, AppPermissionSummary>() {
                                                  {
                                                    put(appId1, appPermissionSummaries.get(0));
                                                    put(appId2, appPermissionSummaries.get(1));
                                                  }
                                                })
                                                .accountPermissionSummary(accountPermissionSummary)
                                                .build();
    userRequestContext.setUserPermissionInfo(userPermissionInfo);
    user.setUserRequestContext(userRequestContext);
    UserThreadLocal.set(user);
    List<SearchResult> searchResults = getSearchResult();
    List<SearchResult> newSearchResults = environmentSearchRequestHandler.filterSearchResults(searchResults);

    assertThat(newSearchResults.size()).isEqualTo(1);

    assertThat(((EnvironmentSearchResult) newSearchResults.get(0))
                   .getWorkflows()
                   .contains(new EntityInfo(workflowId1, "workflow1")))
        .isTrue();
    assertThat(((EnvironmentSearchResult) newSearchResults.get(0))
                   .getWorkflows()
                   .contains(new EntityInfo(workflowId2, "workflow2")))
        .isFalse();
    assertThat(((EnvironmentSearchResult) newSearchResults.get(0))
                   .getWorkflows()
                   .contains(new EntityInfo(workflowId3, "workflow3")))
        .isFalse();

    assertThat(((EnvironmentSearchResult) newSearchResults.get(0))
                   .getPipelines()
                   .contains(new EntityInfo(pipelineId1, "pipeline1")))
        .isTrue();
    assertThat(((EnvironmentSearchResult) newSearchResults.get(0))
                   .getPipelines()
                   .contains(new EntityInfo(pipelineId2, "pipeline2")))
        .isFalse();
    assertThat(((EnvironmentSearchResult) newSearchResults.get(0))
                   .getPipelines()
                   .contains(new EntityInfo(pipelineId3, "pipeline3")))
        .isFalse();

    assertThat(((EnvironmentSearchResult) newSearchResults.get(0)).getAudits()).isNotEmpty();
    assertThat(((EnvironmentSearchResult) newSearchResults.get(0)).getDeployments().size()).isEqualTo(1);
    accountPermissionSummary = AccountPermissionSummary.builder().permissions(new HashSet<>()).build();
    userPermissionInfo.setAccountPermissionSummary(accountPermissionSummary);
    newSearchResults = environmentSearchRequestHandler.filterSearchResults(newSearchResults);

    assertThat(((EnvironmentSearchResult) newSearchResults.get(0)).getAudits()).isEmpty();
  }
}
