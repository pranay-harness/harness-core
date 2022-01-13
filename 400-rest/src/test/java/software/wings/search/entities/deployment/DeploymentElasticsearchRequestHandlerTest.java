/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.search.entities.deployment;

import static io.harness.rule.OwnerRule.MOHIT;
import static io.harness.rule.OwnerRule.UTKARSH;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.AccountType;
import software.wings.beans.EntityType;
import software.wings.beans.User;
import software.wings.events.TestUtils;
import software.wings.search.SearchRequestHandlerTestUtils;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;

public class DeploymentElasticsearchRequestHandlerTest extends WingsBaseTest {
  @Inject @InjectMocks DeploymentElasticsearchRequestHandler deploymentSearchRequestHandler;
  @Inject private TestUtils eventTestHelper;
  @Inject private HPersistence persistence;

  protected String accountId;
  protected String appId1;
  protected String appId2;
  protected String appId3;
  protected String workflowId1;
  protected String serviceId1;
  protected String pipelineId1;
  protected String envId1;
  protected String deploymentId1;
  protected String workflowId2;
  protected String serviceId2;
  protected String pipelineId2;
  protected String envId2;
  protected String deploymentId2;
  protected String workflowId3;
  protected String serviceId3;
  protected String pipelineId3;
  protected String envId3;
  protected String deploymentId3;
  protected User user;
  protected Account account;

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  @Ignore("Investigate to make sure Search Unit Tests are not creating system resources such as Threads")
  public void translateHitsToSearchResultsTest() {
    Account account = getAccount(AccountType.PAID);
    String accountId = persistence.save(account);
    SearchResponse searchResponse = SearchRequestHandlerTestUtils.getSearchResponse(DeploymentSearchEntity.TYPE);

    List<SearchResult> searchResults =
        deploymentSearchRequestHandler.translateHitsToSearchResults(searchResponse.getHits(), accountId);
    searchResults = deploymentSearchRequestHandler.processSearchResults(searchResults);
    assertThat(searchResults).isNotNull();
    assertThat(searchResults.size()).isEqualTo(2);
    assertThat(searchResults.get(0).getType()).isEqualTo(EntityType.DEPLOYMENT);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  @Ignore("Investigate to make sure Search Unit Tests are not creating system resources such as Threads")
  public void testCreateQuery() {
    String searchString = "value";
    Account account = getAccount(AccountType.PAID);
    String accountId = persistence.save(account);
    account.setUuid(accountId);

    BoolQueryBuilder boolQueryBuilder = deploymentSearchRequestHandler.createQuery(searchString, accountId);
    assertThat(boolQueryBuilder).isNotNull();
    assertThat(boolQueryBuilder.filter().size()).isEqualTo(2);
    assertThat(boolQueryBuilder.must().size()).isEqualTo(1);
  }

  private List<AppPermissionSummary> buildAppPermissionSummary() {
    Map<PermissionAttribute.Action, Set<String>> servicePermissions1 =
        new HashMap<PermissionAttribute.Action, Set<String>>() {
          { put(PermissionAttribute.Action.READ, Sets.newHashSet(serviceId1)); }
        };
    Map<PermissionAttribute.Action, Set<String>> servicePermissions2 =
        new HashMap<PermissionAttribute.Action, Set<String>>() {
          { put(PermissionAttribute.Action.READ, Sets.newHashSet(serviceId3)); }
        };

    Map<PermissionAttribute.Action, Set<AppPermissionSummary.EnvInfo>> envPermissions1 =
        new HashMap<PermissionAttribute.Action, Set<AppPermissionSummary.EnvInfo>>() {
          {
            put(PermissionAttribute.Action.READ,
                Sets.newHashSet(AppPermissionSummary.EnvInfo.builder().envId(envId1).envType("PROD").build()));
          }
        };
    Map<PermissionAttribute.Action, Set<AppPermissionSummary.EnvInfo>> envPermissions2 =
        new HashMap<PermissionAttribute.Action, Set<AppPermissionSummary.EnvInfo>>() {
          {
            put(PermissionAttribute.Action.READ,
                Sets.newHashSet(AppPermissionSummary.EnvInfo.builder().envId(envId3).envType("PROD").build()));
          }
        };

    Map<PermissionAttribute.Action, Set<String>> workflowPermissions1 =
        new HashMap<PermissionAttribute.Action, Set<String>>() {
          { put(PermissionAttribute.Action.READ, Sets.newHashSet(workflowId1)); }
        };
    Map<PermissionAttribute.Action, Set<String>> workflowPermissions2 =
        new HashMap<PermissionAttribute.Action, Set<String>>() {
          { put(PermissionAttribute.Action.READ, Sets.newHashSet(workflowId3)); }
        };

    Map<PermissionAttribute.Action, Set<String>> deploymentPermissions1 =
        new HashMap<PermissionAttribute.Action, Set<String>>() {
          { put(PermissionAttribute.Action.READ, Sets.newHashSet(workflowId1)); }
        };
    Map<PermissionAttribute.Action, Set<String>> deploymentPermissions2 =
        new HashMap<PermissionAttribute.Action, Set<String>>() {
          { put(PermissionAttribute.Action.READ, Sets.newHashSet(pipelineId1)); }
        };

    return Arrays.asList(AppPermissionSummary.builder()
                             .servicePermissions(servicePermissions1)
                             .workflowPermissions(workflowPermissions1)
                             .envPermissions(envPermissions1)
                             .deploymentPermissions(deploymentPermissions1)
                             .build(),
        AppPermissionSummary.builder()
            .servicePermissions(servicePermissions2)
            .workflowPermissions(workflowPermissions2)
            .envPermissions(envPermissions2)
            .deploymentPermissions(deploymentPermissions2)
            .build());
  }

  private void setup() {
    account = eventTestHelper.createAccount();
    user = eventTestHelper.createUser(account);
    accountId = UUID.randomUUID().toString();
    appId1 = UUID.randomUUID().toString();
    appId2 = UUID.randomUUID().toString();
    appId3 = UUID.randomUUID().toString();
    workflowId1 = UUID.randomUUID().toString();
    serviceId1 = UUID.randomUUID().toString();
    pipelineId1 = UUID.randomUUID().toString();
    deploymentId1 = UUID.randomUUID().toString();
    envId1 = UUID.randomUUID().toString();
    workflowId2 = UUID.randomUUID().toString();
    serviceId2 = UUID.randomUUID().toString();
    pipelineId2 = UUID.randomUUID().toString();
    envId2 = UUID.randomUUID().toString();
    deploymentId2 = UUID.randomUUID().toString();
    workflowId3 = UUID.randomUUID().toString();
    serviceId3 = UUID.randomUUID().toString();
    pipelineId3 = UUID.randomUUID().toString();
    envId3 = UUID.randomUUID().toString();
    deploymentId3 = UUID.randomUUID().toString();
  }

  private List<SearchResult> getSearchResult() {
    DeploymentSearchResult searchResult1 = new DeploymentSearchResult();
    DeploymentSearchResult searchResult2 = new DeploymentSearchResult();

    searchResult1.setAppId(appId1);
    searchResult1.setAccountId(accountId);
    searchResult1.setId(deploymentId1);
    searchResult1.setType(EntityType.DEPLOYMENT);
    searchResult1.setWorkflowId(workflowId1);
    searchResult2.setAppId(appId2);
    searchResult2.setAccountId(accountId);
    searchResult2.setId(deploymentId2);
    searchResult2.setType(EntityType.DEPLOYMENT);
    searchResult2.setPipelineId(pipelineId2);

    EntityInfo workflow1 = new EntityInfo(workflowId1, "workflow1");
    EntityInfo workflow2 = new EntityInfo(workflowId2, "workflow2");
    EntityInfo workflow3 = new EntityInfo(workflowId3, "workflow3");
    searchResult1.setWorkflows(new HashSet<>(Arrays.asList(workflow1, workflow2)));
    searchResult2.setWorkflows(new HashSet<>(Collections.singletonList(workflow3)));

    EntityInfo service1 = new EntityInfo(serviceId1, "service1");
    EntityInfo service2 = new EntityInfo(serviceId2, "service2");
    EntityInfo service3 = new EntityInfo(serviceId3, "service3");
    searchResult1.setServices(new HashSet<>(Arrays.asList(service1, service2)));
    searchResult2.setServices(new HashSet<>(Collections.singletonList(service3)));

    EntityInfo env1 = new EntityInfo(envId1, "env1");
    EntityInfo env2 = new EntityInfo(envId2, "env2");
    EntityInfo env3 = new EntityInfo(envId3, "env3");
    searchResult1.setEnvironments(new HashSet<>(Arrays.asList(env1, env2)));
    searchResult2.setEnvironments(new HashSet<>(Collections.singletonList(env3)));

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
    List<SearchResult> newSearchResults = deploymentSearchRequestHandler.filterSearchResults(searchResults);

    assertThat(newSearchResults.size()).isEqualTo(1);

    assertThat(((DeploymentSearchResult) newSearchResults.get(0))
                   .getWorkflows()
                   .contains(new EntityInfo(workflowId1, "workflow1")))
        .isTrue();
    assertThat(((DeploymentSearchResult) newSearchResults.get(0))
                   .getWorkflows()
                   .contains(new EntityInfo(workflowId1, "workflow2")))
        .isFalse();
    assertThat(((DeploymentSearchResult) newSearchResults.get(0))
                   .getWorkflows()
                   .contains(new EntityInfo(workflowId3, "workflow3")))
        .isFalse();

    assertThat(((DeploymentSearchResult) newSearchResults.get(0))
                   .getServices()
                   .contains(new EntityInfo(serviceId1, "service1")))
        .isTrue();
    assertThat(((DeploymentSearchResult) newSearchResults.get(0))
                   .getServices()
                   .contains(new EntityInfo(serviceId2, "service2")))
        .isFalse();
    assertThat(((DeploymentSearchResult) newSearchResults.get(0))
                   .getServices()
                   .contains(new EntityInfo(serviceId3, "service3")))
        .isFalse();

    assertThat(
        ((DeploymentSearchResult) newSearchResults.get(0)).getEnvironments().contains(new EntityInfo(envId1, "env1")))
        .isTrue();
    assertThat(
        ((DeploymentSearchResult) newSearchResults.get(0)).getEnvironments().contains(new EntityInfo(envId2, "env2")))
        .isFalse();
    assertThat(
        ((DeploymentSearchResult) newSearchResults.get(0)).getEnvironments().contains(new EntityInfo(envId3, "env3")))
        .isFalse();
  }
}
