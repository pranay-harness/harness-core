/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.search.entities.workflow;

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

public class WorkflowElasticsearchRequestHandlerTest extends WingsBaseTest {
  @Mock @Named(AuditTrailFeature.FEATURE_NAME) PremiumFeature auditTrailFeature;
  @Inject @InjectMocks WorkflowElasticsearchRequestHandler workflowSearchRequestHandler;
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
    SearchResponse searchResponse = SearchRequestHandlerTestUtils.getSearchResponse(WorkflowSearchEntity.TYPE);
    when(auditTrailFeature.isAvailableForAccount(accountId)).thenReturn(true);

    List<SearchResult> searchResults =
        workflowSearchRequestHandler.translateHitsToSearchResults(searchResponse.getHits(), accountId);
    assertThat(searchResults).isNotNull();
    assertThat(searchResults.size()).isEqualTo(1);
    assertThat(searchResults.get(0).getType()).isEqualTo(EntityType.WORKFLOW);
  }

  private List<AppPermissionSummary> buildAppPermissionSummary(boolean addDeployments) {
    Map<PermissionAttribute.Action, Set<String>> servicePermissions1 =
        new HashMap<PermissionAttribute.Action, Set<String>>() {
          { put(PermissionAttribute.Action.READ, Sets.newHashSet(serviceId1)); }
        };
    Map<PermissionAttribute.Action, Set<String>> servicePermissions2 =
        new HashMap<PermissionAttribute.Action, Set<String>>() {
          { put(PermissionAttribute.Action.READ, Sets.newHashSet(serviceId3)); }
        };

    Map<PermissionAttribute.Action, Set<String>> pipelinePermissions1 =
        new HashMap<PermissionAttribute.Action, Set<String>>() {
          { put(PermissionAttribute.Action.READ, Sets.newHashSet(pipelineId1)); }
        };
    Map<PermissionAttribute.Action, Set<String>> pipelinePermissions2 =
        new HashMap<PermissionAttribute.Action, Set<String>>() {
          { put(PermissionAttribute.Action.READ, Sets.newHashSet(pipelineId3)); }
        };

    Map<PermissionAttribute.Action, Set<String>> workflowPermissions =
        new HashMap<PermissionAttribute.Action, Set<String>>() {
          { put(PermissionAttribute.Action.READ, Sets.newHashSet(workflowId1)); }
        };

    Map<PermissionAttribute.Action, Set<String>> deploymentPermissions = new HashMap<>();
    if (addDeployments) {
      deploymentPermissions.put(PermissionAttribute.Action.READ, Sets.newHashSet(workflowId1));
    }

    return Arrays.asList(AppPermissionSummary.builder()
                             .workflowPermissions(workflowPermissions)
                             .servicePermissions(servicePermissions1)
                             .pipelinePermissions(pipelinePermissions1)
                             .deploymentPermissions(deploymentPermissions)
                             .build(),
        AppPermissionSummary.builder()
            .servicePermissions(servicePermissions2)
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
    WorkflowSearchResult searchResult1 = new WorkflowSearchResult();
    WorkflowSearchResult searchResult2 = new WorkflowSearchResult();

    searchResult1.setAppId(appId1);
    searchResult1.setAccountId(accountId);
    searchResult1.setId(workflowId1);
    searchResult1.setType(EntityType.WORKFLOW);
    searchResult2.setAppId(appId2);
    searchResult2.setAccountId(accountId);
    searchResult2.setId(workflowId2);
    searchResult2.setType(EntityType.WORKFLOW);

    EntityInfo pipeline1 = new EntityInfo(pipelineId1, "pipeline1");
    EntityInfo pipeline2 = new EntityInfo(pipelineId2, "pipeline2");
    EntityInfo pipeline3 = new EntityInfo(pipelineId3, "pipeline3");
    searchResult1.setPipelines(new HashSet<>(Arrays.asList(pipeline1, pipeline2)));
    searchResult2.setPipelines(new HashSet<>(Collections.singletonList(pipeline3)));

    EntityInfo service1 = new EntityInfo(serviceId1, "service1");
    EntityInfo service2 = new EntityInfo(serviceId2, "service2");
    EntityInfo service3 = new EntityInfo(serviceId3, "service3");
    searchResult1.setServices(new HashSet<>(Arrays.asList(service1, service2)));
    searchResult2.setServices(new HashSet<>(Collections.singletonList(service3)));

    RelatedAuditView audit1 = new RelatedAuditView();
    RelatedAuditView audit2 = new RelatedAuditView();
    searchResult1.setAudits(Arrays.asList(audit1, audit2));

    WorkflowExecution workflowExecution =
        WorkflowExecution.builder().workflowId(workflowId1).workflowType(WorkflowType.PIPELINE).build();
    RelatedDeploymentView deployment = new RelatedDeploymentView(workflowExecution);
    searchResult1.setDeployments(Collections.singletonList(deployment));

    return Arrays.asList(searchResult1, searchResult2);
  }

  @Test
  @Owner(developers = MOHIT)
  @Category(UnitTests.class)
  @Ignore("Investigate to make sure Search Unit Tests are not creating system resources such as Threads")
  public void filterSearchResultsTest() {
    setup();
    UserRequestContext userRequestContext = UserRequestContext.builder().accountId(accountId).build();
    List<AppPermissionSummary> appPermissionSummaries = buildAppPermissionSummary(true);
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
    List<SearchResult> newSearchResults = workflowSearchRequestHandler.filterSearchResults(searchResults);

    assertThat(newSearchResults.size()).isEqualTo(1);

    assertThat(((WorkflowSearchResult) newSearchResults.get(0))
                   .getPipelines()
                   .contains(new EntityInfo(pipelineId1, "pipeline1")))
        .isTrue();
    assertThat(((WorkflowSearchResult) newSearchResults.get(0))
                   .getPipelines()
                   .contains(new EntityInfo(pipelineId2, "pipeline2")))
        .isFalse();
    assertThat(((WorkflowSearchResult) newSearchResults.get(0))
                   .getPipelines()
                   .contains(new EntityInfo(pipelineId3, "pipeline3")))
        .isFalse();

    assertThat(
        ((WorkflowSearchResult) newSearchResults.get(0)).getServices().contains(new EntityInfo(serviceId1, "service1")))
        .isTrue();
    assertThat(
        ((WorkflowSearchResult) newSearchResults.get(0)).getServices().contains(new EntityInfo(serviceId2, "service2")))
        .isFalse();
    assertThat(
        ((WorkflowSearchResult) newSearchResults.get(0)).getServices().contains(new EntityInfo(serviceId3, "service3")))
        .isFalse();

    assertThat(((WorkflowSearchResult) newSearchResults.get(0)).getDeploymentsCount()).isEqualTo(1);
    assertThat(((WorkflowSearchResult) newSearchResults.get(0)).getAudits()).isNotEmpty();

    List<AppPermissionSummary> appPermissionSummaries2 = buildAppPermissionSummary(false);
    userPermissionInfo = UserPermissionInfo.builder()
                             .accountId(accountId)
                             .appPermissionMapInternal(new HashMap<String, AppPermissionSummary>() {
                               {
                                 put(appId1, appPermissionSummaries2.get(0));
                                 put(appId2, appPermissionSummaries2.get(1));
                               }
                             })
                             .accountPermissionSummary(accountPermissionSummary)
                             .build();
    userRequestContext.setUserPermissionInfo(userPermissionInfo);
    user.setUserRequestContext(userRequestContext);
    accountPermissionSummary = AccountPermissionSummary.builder().permissions(new HashSet<>()).build();
    userPermissionInfo.setAccountPermissionSummary(accountPermissionSummary);
    newSearchResults = workflowSearchRequestHandler.filterSearchResults(newSearchResults);

    assertThat(((WorkflowSearchResult) newSearchResults.get(0)).getAudits()).isEmpty();
    assertThat(((WorkflowSearchResult) newSearchResults.get(0)).getDeploymentsCount()).isEqualTo(0);
  }
}
