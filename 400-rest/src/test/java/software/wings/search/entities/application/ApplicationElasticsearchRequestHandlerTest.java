/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.search.entities.application;

import static io.harness.rule.OwnerRule.MOHIT;
import static io.harness.rule.OwnerRule.UTKARSH;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.AccountType;
import software.wings.beans.EntityType;
import software.wings.beans.User;
import software.wings.events.TestUtils;
import software.wings.features.AuditTrailFeature;
import software.wings.features.api.PremiumFeature;
import software.wings.search.SearchRequestHandlerTestUtils;
import software.wings.search.entities.related.audit.RelatedAuditView;
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

public class ApplicationElasticsearchRequestHandlerTest extends WingsBaseTest {
  @Mock @Named(AuditTrailFeature.FEATURE_NAME) PremiumFeature auditTrailFeature;
  @Inject @InjectMocks ApplicationElasticsearchRequestHandler applicationSearchRequestHandler;
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
    SearchResponse searchResponse = SearchRequestHandlerTestUtils.getSearchResponse(ApplicationSearchEntity.TYPE);
    when(auditTrailFeature.isAvailableForAccount(accountId)).thenReturn(true);

    List<SearchResult> newSearchResults =
        applicationSearchRequestHandler.translateHitsToSearchResults(searchResponse.getHits(), accountId);
    assertThat(newSearchResults).isNotNull();
    assertThat(newSearchResults.size()).isEqualTo(1);
    assertThat(newSearchResults.get(0).getType()).isEqualTo(EntityType.APPLICATION);
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

    Map<PermissionAttribute.Action, Set<String>> pipelinePermissions1 =
        new HashMap<PermissionAttribute.Action, Set<String>>() {
          { put(PermissionAttribute.Action.READ, Sets.newHashSet(pipelineId1)); }
        };
    Map<PermissionAttribute.Action, Set<String>> pipelinePermissions2 =
        new HashMap<PermissionAttribute.Action, Set<String>>() {
          { put(PermissionAttribute.Action.READ, Sets.newHashSet(pipelineId3)); }
        };

    Map<PermissionAttribute.Action, Set<String>> workflowPermissions1 =
        new HashMap<PermissionAttribute.Action, Set<String>>() {
          { put(PermissionAttribute.Action.READ, Sets.newHashSet(workflowId1)); }
        };
    Map<PermissionAttribute.Action, Set<String>> workflowPermissions2 =
        new HashMap<PermissionAttribute.Action, Set<String>>() {
          { put(PermissionAttribute.Action.READ, Sets.newHashSet(workflowId3)); }
        };

    return Arrays.asList(AppPermissionSummary.builder()
                             .servicePermissions(servicePermissions1)
                             .envPermissions(envPermissions1)
                             .workflowPermissions(workflowPermissions1)
                             .pipelinePermissions(pipelinePermissions1)
                             .build(),
        AppPermissionSummary.builder()
            .servicePermissions(servicePermissions2)
            .envPermissions(envPermissions2)
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
    appId3 = UUID.randomUUID().toString();
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
    ApplicationSearchResult searchResult1 = new ApplicationSearchResult();
    ApplicationSearchResult searchResult2 = new ApplicationSearchResult();

    searchResult1.setAccountId(accountId);
    searchResult1.setId(appId1);
    searchResult1.setType(EntityType.APPLICATION);
    searchResult2.setAccountId(accountId);
    searchResult2.setId(appId2);
    searchResult2.setType(EntityType.APPLICATION);

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

    RelatedAuditView audit1 = new RelatedAuditView();
    RelatedAuditView audit2 = new RelatedAuditView();
    searchResult1.setAudits(Arrays.asList(audit1, audit2));

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
                                                    put(appId3, appPermissionSummaries.get(1));
                                                  }
                                                })
                                                .accountPermissionSummary(accountPermissionSummary)
                                                .build();
    userRequestContext.setUserPermissionInfo(userPermissionInfo);
    user.setUserRequestContext(userRequestContext);
    UserThreadLocal.set(user);
    List<SearchResult> searchResults = getSearchResult();
    List<SearchResult> newSearchResults = applicationSearchRequestHandler.filterSearchResults(searchResults);

    assertThat(newSearchResults.size()).isEqualTo(1);

    assertThat(((ApplicationSearchResult) newSearchResults.get(0))
                   .getWorkflows()
                   .contains(new EntityInfo(workflowId1, "workflow1")))
        .isTrue();
    assertThat(((ApplicationSearchResult) newSearchResults.get(0))
                   .getWorkflows()
                   .contains(new EntityInfo(workflowId1, "workflow2")))
        .isFalse();
    assertThat(((ApplicationSearchResult) newSearchResults.get(0))
                   .getWorkflows()
                   .contains(new EntityInfo(workflowId3, "workflow3")))
        .isFalse();

    assertThat(((ApplicationSearchResult) newSearchResults.get(0))
                   .getPipelines()
                   .contains(new EntityInfo(pipelineId1, "pipeline1")))
        .isTrue();
    assertThat(((ApplicationSearchResult) newSearchResults.get(0))
                   .getPipelines()
                   .contains(new EntityInfo(pipelineId2, "pipeline2")))
        .isFalse();
    assertThat(((ApplicationSearchResult) newSearchResults.get(0))
                   .getPipelines()
                   .contains(new EntityInfo(pipelineId3, "pipeline3")))
        .isFalse();

    assertThat(((ApplicationSearchResult) newSearchResults.get(0))
                   .getServices()
                   .contains(new EntityInfo(serviceId1, "service1")))
        .isTrue();
    assertThat(((ApplicationSearchResult) newSearchResults.get(0))
                   .getServices()
                   .contains(new EntityInfo(serviceId2, "service2")))
        .isFalse();
    assertThat(((ApplicationSearchResult) newSearchResults.get(0))
                   .getServices()
                   .contains(new EntityInfo(serviceId3, "service3")))
        .isFalse();

    assertThat(
        ((ApplicationSearchResult) newSearchResults.get(0)).getEnvironments().contains(new EntityInfo(envId1, "env1")))
        .isTrue();
    assertThat(
        ((ApplicationSearchResult) newSearchResults.get(0)).getEnvironments().contains(new EntityInfo(envId2, "env2")))
        .isFalse();
    assertThat(
        ((ApplicationSearchResult) newSearchResults.get(0)).getEnvironments().contains(new EntityInfo(envId3, "env3")))
        .isFalse();

    assertThat(((ApplicationSearchResult) newSearchResults.get(0)).getAudits()).isNotEmpty();

    accountPermissionSummary = AccountPermissionSummary.builder().permissions(new HashSet<>()).build();
    userPermissionInfo.setAccountPermissionSummary(accountPermissionSummary);
    newSearchResults = applicationSearchRequestHandler.filterSearchResults(newSearchResults);

    assertThat(((ApplicationSearchResult) newSearchResults.get(0)).getAudits()).isEmpty();
  }
}
