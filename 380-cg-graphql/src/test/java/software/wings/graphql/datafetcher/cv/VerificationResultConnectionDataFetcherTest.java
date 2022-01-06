/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.cv;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.RAGHU;

import static software.wings.beans.Account.Builder.anAccount;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.User.Builder.anUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.cv.WorkflowVerificationResult;
import io.harness.cv.api.WorkflowVerificationResultService;
import io.harness.rule.Owner;

import software.wings.beans.AccountType;
import software.wings.beans.LicenseInfo;
import software.wings.beans.Service;
import software.wings.graphql.datafetcher.AbstractDataFetcherTestBase;
import software.wings.graphql.datafetcher.AccountThreadLocal;
import software.wings.graphql.schema.query.QLPageQueryParameterImpl;
import software.wings.graphql.schema.type.aggregation.QLExecutionStatusType;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLIdOperator;
import software.wings.graphql.schema.type.aggregation.cv.QLVerificationResultConnection;
import software.wings.graphql.schema.type.aggregation.cv.QLVerificationResultFilter;
import software.wings.security.AppPermissionSummaryForUI;
import software.wings.security.PermissionAttribute;
import software.wings.security.UserPermissionInfo;
import software.wings.security.UserRequestContext;
import software.wings.security.UserThreadLocal;
import software.wings.sm.StateType;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import graphql.execution.MergedSelectionSet;
import graphql.schema.DataFetchingFieldSelectionSet;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.SelectedField;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class VerificationResultConnectionDataFetcherTest extends AbstractDataFetcherTestBase {
  private String accountId;
  private String appName = "My app";
  private String serviceName = "My Service";
  private String envName = "My Env";
  private String appId;
  private String serviceId;
  private String envId;
  private String workflowId;

  @Inject private WorkflowVerificationResultService workflowVerificationResultService;
  @Inject private VerificationResultConnectionDataFetcher verificationResultConnectionDataFetcher;

  private static final SelectedField selectedField = new SelectedField() {
    @Override
    public String getName() {
      return "total";
    }
    @Override
    public String getQualifiedName() {
      return null;
    }
    @Override
    public GraphQLFieldDefinition getFieldDefinition() {
      return null;
    }
    @Override
    public Map<String, Object> getArguments() {
      return null;
    }
    @Override
    public DataFetchingFieldSelectionSet getSelectionSet() {
      return null;
    }
  };

  private static final DataFetchingFieldSelectionSet mockSelectionSet = new DataFetchingFieldSelectionSet() {
    public MergedSelectionSet get() {
      return MergedSelectionSet.newMergedSelectionSet().build();
    }
    public Map<String, Map<String, Object>> getArguments() {
      return Collections.emptyMap();
    }
    public Map<String, GraphQLFieldDefinition> getDefinitions() {
      return Collections.emptyMap();
    }
    public boolean contains(String fieldGlobPattern) {
      return false;
    }
    public SelectedField getField(String fieldName) {
      return null;
    }
    public List<SelectedField> getFields() {
      return Collections.singletonList(selectedField);
    }
    public List<SelectedField> getFields(String fieldGlobPattern) {
      return Collections.emptyList();
    }
  };

  @Before
  public void setUp() throws Exception {
    initMocks(this);
    accountId = accountService
                    .save(anAccount()
                              .withAccountName(generateUuid())
                              .withCompanyName(generateUuid())
                              .withLicenseInfo(LicenseInfo.builder().accountType(AccountType.TRIAL).build())
                              .build(),
                        false)
                    .getUuid();
    AccountThreadLocal.set(accountId);
    appId = appService.save(anApplication().name(appName).accountId(accountId).build()).getUuid();
    serviceId =
        serviceResourceService.save(Service.builder().name(serviceName).accountId(accountId).appId(appId).build())
            .getUuid();
    envId = environmentService.save(anEnvironment().name(envName).accountId(accountId).appId(appId).build()).getUuid();
    workflowId = generateUuid();
    Map<String, AppPermissionSummaryForUI> appPermissionMap = new HashMap<>();
    appPermissionMap.put(appId, AppPermissionSummaryForUI.builder().build());

    Map<String, Set<PermissionAttribute.Action>> servicePermissions = new HashMap<>();
    servicePermissions.put(serviceId, Sets.newHashSet());
    Map<String, Set<PermissionAttribute.Action>> envPermissions = new HashMap<>();
    envPermissions.put(envId, Sets.newHashSet());
    Map<String, Set<PermissionAttribute.Action>> workflowPermissions = new HashMap<>();
    workflowPermissions.put(workflowId, Sets.newHashSet());

    appPermissionMap.get(appId).setServicePermissions(servicePermissions);
    appPermissionMap.get(appId).setEnvPermissions(envPermissions);
    appPermissionMap.get(appId).setWorkflowPermissions(workflowPermissions);
    UserThreadLocal.set(
        anUser()
            .userRequestContext(
                UserRequestContext.builder()
                    .userPermissionInfo(UserPermissionInfo.builder().appPermissionMap(appPermissionMap).build())
                    .build())
            .build());
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  @Ignore("This class is not used anymore")
  public void testAccountFilteredLimitAndOffset() {
    String otherAccountId = generateUuid();
    for (int i = 0; i < 10; i++) {
      workflowVerificationResultService.addWorkflowVerificationResult(WorkflowVerificationResult.builder()
                                                                          .accountId(accountId)
                                                                          .appId(appId)
                                                                          .stateExecutionId(generateUuid())
                                                                          .serviceId(serviceId)
                                                                          .envId(envId)
                                                                          .workflowId(workflowId)
                                                                          .stateType(StateType.PROMETHEUS.name())
                                                                          .executionStatus(ExecutionStatus.SKIPPED)
                                                                          .build());

      workflowVerificationResultService.addWorkflowVerificationResult(WorkflowVerificationResult.builder()
                                                                          .accountId(otherAccountId)
                                                                          .appId(appId)
                                                                          .stateExecutionId(generateUuid())
                                                                          .serviceId(serviceId)
                                                                          .envId(envId)
                                                                          .workflowId(workflowId)
                                                                          .stateType(StateType.PROMETHEUS.name())
                                                                          .executionStatus(ExecutionStatus.SKIPPED)
                                                                          .build());
    }

    QLVerificationResultConnection verificationResults = verificationResultConnectionDataFetcher.fetchConnection(
        Lists.newArrayList(QLVerificationResultFilter.builder().build()),
        QLPageQueryParameterImpl.builder().limit(100).selectionSet(mockSelectionSet).build(), null);
    assertThat(verificationResults.getNodes().size()).isEqualTo(10);

    // test limit
    verificationResults = verificationResultConnectionDataFetcher.fetchConnection(
        Lists.newArrayList(QLVerificationResultFilter.builder().build()),
        QLPageQueryParameterImpl.builder().limit(5).selectionSet(mockSelectionSet).build(), null);
    assertThat(verificationResults.getNodes().size()).isEqualTo(5);

    // test offset
    verificationResults = verificationResultConnectionDataFetcher.fetchConnection(
        Lists.newArrayList(QLVerificationResultFilter.builder().build()),
        QLPageQueryParameterImpl.builder().limit(50).offset(8).selectionSet(mockSelectionSet).build(), null);
    assertThat(verificationResults.getNodes().size()).isEqualTo(2);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  @Ignore("This class is not used anymore")
  public void testAppRBAC() {
    workflowVerificationResultService.addWorkflowVerificationResult(WorkflowVerificationResult.builder()
                                                                        .accountId(accountId)
                                                                        .appId(appId)
                                                                        .stateExecutionId(generateUuid())
                                                                        .serviceId(serviceId)
                                                                        .envId(envId)
                                                                        .workflowId(workflowId)
                                                                        .stateType(StateType.PROMETHEUS.name())
                                                                        .executionStatus(ExecutionStatus.SKIPPED)
                                                                        .build());

    QLVerificationResultConnection verificationResults = verificationResultConnectionDataFetcher.fetchConnection(
        Lists.newArrayList(QLVerificationResultFilter.builder().build()),
        QLPageQueryParameterImpl.builder().limit(100).selectionSet(mockSelectionSet).build(), null);
    assertThat(verificationResults.getNodes().size()).isEqualTo(1);
    UserThreadLocal.get().getUserRequestContext().getUserPermissionInfo().getAppPermissionMap().remove(appId);
    verificationResults = verificationResultConnectionDataFetcher.fetchConnection(
        Lists.newArrayList(QLVerificationResultFilter.builder().build()),
        QLPageQueryParameterImpl.builder().limit(100).selectionSet(mockSelectionSet).build(), null);
    assertThat(verificationResults.getNodes().isEmpty()).isTrue();
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  @Ignore("This class is not used anymore")
  public void testEnvRBAC() {
    workflowVerificationResultService.addWorkflowVerificationResult(WorkflowVerificationResult.builder()
                                                                        .accountId(accountId)
                                                                        .appId(appId)
                                                                        .stateExecutionId(generateUuid())
                                                                        .serviceId(serviceId)
                                                                        .envId(envId)
                                                                        .workflowId(workflowId)
                                                                        .stateType(StateType.PROMETHEUS.name())
                                                                        .executionStatus(ExecutionStatus.SKIPPED)
                                                                        .build());

    QLVerificationResultConnection verificationResults = verificationResultConnectionDataFetcher.fetchConnection(
        Lists.newArrayList(QLVerificationResultFilter.builder().build()),
        QLPageQueryParameterImpl.builder().limit(100).selectionSet(mockSelectionSet).build(), null);
    assertThat(verificationResults.getNodes().size()).isEqualTo(1);
    UserThreadLocal.get()
        .getUserRequestContext()
        .getUserPermissionInfo()
        .getAppPermissionMap()
        .get(appId)
        .getEnvPermissions()
        .remove(envId);
    verificationResults = verificationResultConnectionDataFetcher.fetchConnection(
        Lists.newArrayList(QLVerificationResultFilter.builder().build()),
        QLPageQueryParameterImpl.builder().limit(100).selectionSet(mockSelectionSet).build(), null);
    assertThat(verificationResults.getNodes().isEmpty()).isTrue();
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  @Ignore("This class is not used anymore")
  public void testServiceRBAC() {
    workflowVerificationResultService.addWorkflowVerificationResult(WorkflowVerificationResult.builder()
                                                                        .accountId(accountId)
                                                                        .appId(appId)
                                                                        .stateExecutionId(generateUuid())
                                                                        .serviceId(serviceId)
                                                                        .envId(envId)
                                                                        .workflowId(workflowId)
                                                                        .stateType(StateType.PROMETHEUS.name())
                                                                        .executionStatus(ExecutionStatus.SKIPPED)
                                                                        .build());

    QLVerificationResultConnection verificationResults = verificationResultConnectionDataFetcher.fetchConnection(
        Lists.newArrayList(QLVerificationResultFilter.builder().build()),
        QLPageQueryParameterImpl.builder().limit(100).selectionSet(mockSelectionSet).build(), null);
    assertThat(verificationResults.getNodes().size()).isEqualTo(1);
    UserThreadLocal.get()
        .getUserRequestContext()
        .getUserPermissionInfo()
        .getAppPermissionMap()
        .get(appId)
        .getServicePermissions()
        .remove(serviceId);
    verificationResults = verificationResultConnectionDataFetcher.fetchConnection(
        Lists.newArrayList(QLVerificationResultFilter.builder().build()),
        QLPageQueryParameterImpl.builder().limit(100).selectionSet(mockSelectionSet).build(), null);
    assertThat(verificationResults.getNodes().isEmpty()).isTrue();
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  @Ignore("This class is not used anymore")
  public void testWorkflowRBAC() {
    workflowVerificationResultService.addWorkflowVerificationResult(WorkflowVerificationResult.builder()
                                                                        .accountId(accountId)
                                                                        .appId(appId)
                                                                        .stateExecutionId(generateUuid())
                                                                        .serviceId(serviceId)
                                                                        .envId(envId)
                                                                        .workflowId(workflowId)
                                                                        .stateType(StateType.PROMETHEUS.name())
                                                                        .executionStatus(ExecutionStatus.SKIPPED)
                                                                        .build());

    QLVerificationResultConnection verificationResults = verificationResultConnectionDataFetcher.fetchConnection(
        Lists.newArrayList(QLVerificationResultFilter.builder().build()),
        QLPageQueryParameterImpl.builder().limit(100).selectionSet(mockSelectionSet).build(), null);
    assertThat(verificationResults.getNodes().size()).isEqualTo(1);
    UserThreadLocal.get()
        .getUserRequestContext()
        .getUserPermissionInfo()
        .getAppPermissionMap()
        .get(appId)
        .getWorkflowPermissions()
        .remove(workflowId);
    verificationResults = verificationResultConnectionDataFetcher.fetchConnection(
        Lists.newArrayList(QLVerificationResultFilter.builder().build()),
        QLPageQueryParameterImpl.builder().limit(100).selectionSet(mockSelectionSet).build(), null);
    assertThat(verificationResults.getNodes().isEmpty()).isTrue();
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  @Ignore("This class is not used anymore")
  public void testAppFilter() {
    int numOfApp = 3;
    int numOfServiceEnv = 4;
    int numOfVerificationResults = 5;
    createAppServiceEnv(numOfApp, numOfServiceEnv, numOfVerificationResults);
    QLVerificationResultConnection verificationResults = verificationResultConnectionDataFetcher.fetchConnection(
        Lists.newArrayList(QLVerificationResultFilter.builder().build()),
        QLPageQueryParameterImpl.builder().limit(100000).selectionSet(mockSelectionSet).build(), null);
    assertThat(verificationResults.getNodes().size()).isEqualTo(numOfApp * numOfServiceEnv * numOfVerificationResults);

    // test app equals
    verificationResults = verificationResultConnectionDataFetcher.fetchConnection(
        Lists.newArrayList(
            QLVerificationResultFilter.builder()
                .application(
                    QLIdFilter.builder().operator(QLIdOperator.EQUALS).values(new String[] {"My App-0"}).build())
                .build()),
        QLPageQueryParameterImpl.builder().limit(100000).selectionSet(mockSelectionSet).build(), null);

    assertThat(verificationResults.getNodes().size()).isEqualTo(numOfServiceEnv * numOfVerificationResults);
    verificationResults.getNodes().forEach(
        qlVerificationResult -> assertThat(qlVerificationResult.getAppName()).isEqualTo("My App-0"));

    verificationResults = verificationResultConnectionDataFetcher.fetchConnection(
        Lists.newArrayList(
            QLVerificationResultFilter.builder()
                .application(
                    QLIdFilter.builder().operator(QLIdOperator.EQUALS).values(new String[] {"My App-34"}).build())
                .build()),
        QLPageQueryParameterImpl.builder().limit(100000).selectionSet(mockSelectionSet).build(), null);
    assertThat(verificationResults.getNodes()).isEmpty();

    // test LIKE
    verificationResults = verificationResultConnectionDataFetcher.fetchConnection(
        Lists.newArrayList(
            QLVerificationResultFilter.builder()
                .application(
                    QLIdFilter.builder().operator(QLIdOperator.LIKE).values(new String[] {"pP-0", "aPP-1"}).build())
                .build()),
        QLPageQueryParameterImpl.builder().limit(100000).selectionSet(mockSelectionSet).build(), null);

    assertThat(verificationResults.getNodes().size()).isEqualTo(2 * numOfServiceEnv * numOfVerificationResults);
    verificationResults.getNodes().forEach(verificationResult
        -> assertThat(
            verificationResult.getAppName().equals("My App-0") || verificationResult.getAppName().equals("My App-1"))
               .isTrue());

    verificationResults = verificationResultConnectionDataFetcher.fetchConnection(
        Lists.newArrayList(
            QLVerificationResultFilter.builder()
                .application(
                    QLIdFilter.builder().operator(QLIdOperator.LIKE).values(new String[] {"34", "we-1"}).build())
                .build()),
        QLPageQueryParameterImpl.builder().limit(100000).selectionSet(mockSelectionSet).build(), null);
    assertThat(verificationResults.getNodes()).isEmpty();

    // test IN
    verificationResults = verificationResultConnectionDataFetcher.fetchConnection(
        Lists.newArrayList(QLVerificationResultFilter.builder()
                               .application(QLIdFilter.builder()
                                                .operator(QLIdOperator.IN)
                                                .values(new String[] {"My App-0", "My App-2"})
                                                .build())
                               .build()),
        QLPageQueryParameterImpl.builder().limit(100000).selectionSet(mockSelectionSet).build(), null);

    assertThat(verificationResults.getNodes().size()).isEqualTo(2 * numOfServiceEnv * numOfVerificationResults);
    verificationResults.getNodes().forEach(verificationResult
        -> assertThat(
            verificationResult.getAppName().equals("My App-0") || verificationResult.getAppName().equals("My App-2"))
               .isTrue());

    verificationResults = verificationResultConnectionDataFetcher.fetchConnection(
        Lists.newArrayList(
            QLVerificationResultFilter.builder()
                .application(QLIdFilter.builder().operator(QLIdOperator.IN).values(new String[] {"My App"}).build())
                .build()),
        QLPageQueryParameterImpl.builder().limit(100000).selectionSet(mockSelectionSet).build(), null);
    assertThat(verificationResults.getNodes()).isEmpty();

    // test NOT IN
    verificationResults = verificationResultConnectionDataFetcher.fetchConnection(
        Lists.newArrayList(QLVerificationResultFilter.builder()
                               .application(QLIdFilter.builder()
                                                .operator(QLIdOperator.NOT_IN)
                                                .values(new String[] {"My App-0", "My App-2"})
                                                .build())
                               .build()),
        QLPageQueryParameterImpl.builder().limit(100000).selectionSet(mockSelectionSet).build(), null);

    assertThat(verificationResults.getNodes().size()).isEqualTo(numOfServiceEnv * numOfVerificationResults);
    verificationResults.getNodes().forEach(
        verificationResult -> assertThat(verificationResult.getAppName()).isEqualTo("My App-1"));
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  @Ignore("This class is not used anymore")
  public void testEnvFilter() {
    int numOfApp = 4;
    int numOfServiceEnv = 3;
    int numOfVerificationResults = 8;
    createAppServiceEnv(numOfApp, numOfServiceEnv, numOfVerificationResults);
    QLVerificationResultConnection verificationResults = verificationResultConnectionDataFetcher.fetchConnection(
        Lists.newArrayList(
            QLVerificationResultFilter.builder()
                .environment(QLIdFilter.builder().operator(QLIdOperator.LIKE).values(new String[] {"eNv"}).build())
                .build()),
        QLPageQueryParameterImpl.builder().limit(100000).selectionSet(mockSelectionSet).build(), null);
    assertThat(verificationResults.getNodes().size()).isEqualTo(numOfApp * numOfServiceEnv * numOfVerificationResults);

    verificationResults = verificationResultConnectionDataFetcher.fetchConnection(
        Lists.newArrayList(
            QLVerificationResultFilter.builder()
                .environment(QLIdFilter.builder().operator(QLIdOperator.LIKE).values(new String[] {"eNv-1"}).build())
                .build()),
        QLPageQueryParameterImpl.builder().limit(100000).selectionSet(mockSelectionSet).build(), null);

    assertThat(verificationResults.getNodes().size()).isEqualTo(numOfApp * numOfVerificationResults);
    verificationResults.getNodes().forEach(
        verificationResult -> assertThat(verificationResult.getEnvName()).isEqualTo("My ENv-1"));
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  @Ignore("This class is not used anymore")
  public void testServiceFilter() {
    int numOfApp = 4;
    int numOfServiceEnv = 3;
    int numOfVerificationResults = 8;
    createAppServiceEnv(numOfApp, numOfServiceEnv, numOfVerificationResults);
    QLVerificationResultConnection verificationResults = verificationResultConnectionDataFetcher.fetchConnection(
        Lists.newArrayList(
            QLVerificationResultFilter.builder()
                .service(QLIdFilter.builder().operator(QLIdOperator.LIKE).values(new String[] {"SerV"}).build())
                .build()),
        QLPageQueryParameterImpl.builder().limit(100000).selectionSet(mockSelectionSet).build(), null);
    assertThat(verificationResults.getNodes().size()).isEqualTo(numOfApp * numOfServiceEnv * numOfVerificationResults);

    verificationResults = verificationResultConnectionDataFetcher.fetchConnection(
        Lists.newArrayList(
            QLVerificationResultFilter.builder()
                .service(QLIdFilter.builder().operator(QLIdOperator.LIKE).values(new String[] {"sERvICe-2"}).build())
                .build()),
        QLPageQueryParameterImpl.builder().limit(100000).selectionSet(mockSelectionSet).build(), null);

    assertThat(verificationResults.getNodes().size()).isEqualTo(numOfApp * numOfVerificationResults);
    verificationResults.getNodes().forEach(
        verificationResult -> assertThat(verificationResult.getServiceName()).isEqualTo("My serVice-2"));
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  @Ignore("This class is not used anymore")
  public void testExecutionStatusFilter() {
    int numOfApp = 4;
    int numOfServiceEnv = 3;
    int numOfVerificationResults = 8;
    createAppServiceEnv(numOfApp, numOfServiceEnv, numOfVerificationResults);
    QLVerificationResultConnection verificationResults = verificationResultConnectionDataFetcher.fetchConnection(
        Lists.newArrayList(QLVerificationResultFilter.builder().build()),
        QLPageQueryParameterImpl.builder().limit(100000).selectionSet(mockSelectionSet).build(), null);
    assertThat(verificationResults.getNodes().size()).isEqualTo(numOfApp * numOfServiceEnv * numOfVerificationResults);

    verificationResults = verificationResultConnectionDataFetcher.fetchConnection(
        Lists.newArrayList(QLVerificationResultFilter.builder().build()),
        QLPageQueryParameterImpl.builder().limit(100000).selectionSet(mockSelectionSet).build(), null);

    assertThat(verificationResults.getNodes().size())
        .isEqualTo(numOfApp * numOfServiceEnv * numOfVerificationResults / 2);
    verificationResults.getNodes().forEach(verificationResult
        -> assertThat(verificationResult.getStatus()).isEqualTo(QLExecutionStatusType.FAILED.name()));
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  @Ignore("This class is not used anymore")
  public void testRollbackFilter() {
    int numOfApp = 2;
    int numOfServiceEnv = 3;
    int numOfVerificationResults = 10;
    createAppServiceEnv(numOfApp, numOfServiceEnv, numOfVerificationResults);
    QLVerificationResultConnection verificationResults = verificationResultConnectionDataFetcher.fetchConnection(
        Lists.newArrayList(QLVerificationResultFilter.builder().rollback(true).build()),
        QLPageQueryParameterImpl.builder().limit(100000).selectionSet(mockSelectionSet).build(), null);
    assertThat(verificationResults.getNodes().size())
        .isEqualTo(numOfApp * numOfServiceEnv * numOfVerificationResults / 2);

    verificationResults.getNodes().forEach(verificationResult -> assertThat(verificationResult.getRollback()).isTrue());
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  @Ignore("This class is not used anymore")
  public void testAnalyzedFilter() {
    int numOfApp = 2;
    int numOfServiceEnv = 3;
    int numOfVerificationResults = 10;
    createAppServiceEnv(numOfApp, numOfServiceEnv, numOfVerificationResults);
    QLVerificationResultConnection verificationResults = verificationResultConnectionDataFetcher.fetchConnection(
        Lists.newArrayList(QLVerificationResultFilter.builder().build()),
        QLPageQueryParameterImpl.builder().limit(100000).selectionSet(mockSelectionSet).build(), null);
    assertThat(verificationResults.getNodes().size())
        .isEqualTo(numOfApp * numOfServiceEnv * numOfVerificationResults / 2);

    verificationResults.getNodes().forEach(
        verificationResult -> assertThat(verificationResult.getAnalyzed()).isFalse());
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  @Ignore("This class is not used anymore")
  public void testCompositeFilter() {
    int numOfApp = 3;
    int numOfServiceEnv = 4;
    int numOfVerificationResults = 8;
    createAppServiceEnv(numOfApp, numOfServiceEnv, numOfVerificationResults);
    QLVerificationResultConnection verificationResults = verificationResultConnectionDataFetcher.fetchConnection(
        Lists.newArrayList(
            QLVerificationResultFilter.builder()
                .application(
                    QLIdFilter.builder().operator(QLIdOperator.EQUALS).values(new String[] {"My App-0"}).build())
                .environment(QLIdFilter.builder().operator(QLIdOperator.LIKE).values(new String[] {"EnV-1"}).build())
                .service(QLIdFilter.builder().operator(QLIdOperator.IN).values(new String[] {"mM SeRvIcE-1"}).build())
                .rollback(true)
                .build()),
        QLPageQueryParameterImpl.builder().limit(100000).selectionSet(mockSelectionSet).build(), null);
    verificationResults.getNodes().forEach(verificationResult -> {
      assertThat(verificationResult.getAppName()).isEqualTo("My App-0");
      assertThat(verificationResult.getEnvName()).isEqualTo("My ENv-1");
      assertThat(verificationResult.getServiceName()).isEqualTo("My serVice-1");
      assertThat(verificationResult.getStatus()).isEqualTo(QLExecutionStatusType.SUCCESS.name());
      assertThat(verificationResult.getAnalyzed()).isTrue();
      assertThat(verificationResult.getRollback()).isTrue();
    });
  }

  private void createAppServiceEnv(int numOfApp, int numOfServiceEnv, int numOfVerificationResults) {
    Map<String, AppPermissionSummaryForUI> appPermissionMap =
        UserThreadLocal.get().getUserRequestContext().getUserPermissionInfo().getAppPermissionMap();
    for (int i = 0; i < numOfApp; i++) {
      String appId = appService.save(anApplication().name("My App-" + i).accountId(accountId).build()).getUuid();

      appPermissionMap.put(appId, AppPermissionSummaryForUI.builder().build());

      Map<String, Set<PermissionAttribute.Action>> servicePermissions = new HashMap<>();
      Map<String, Set<PermissionAttribute.Action>> envPermissions = new HashMap<>();
      Map<String, Set<PermissionAttribute.Action>> workflowPermissions = new HashMap<>();
      workflowPermissions.put(workflowId, Sets.newHashSet());

      for (int j = 0; j < numOfServiceEnv; j++) {
        String serviceId =
            serviceResourceService
                .save(Service.builder().name("My serVice-" + j).accountId(accountId).appId(appId).build())
                .getUuid();
        servicePermissions.put(serviceId, Sets.newHashSet());
        String envId =
            environmentService.save(anEnvironment().name("My ENv-" + j).accountId(accountId).appId(appId).build())
                .getUuid();
        envPermissions.put(envId, Sets.newHashSet());

        for (int k = 0; k < numOfVerificationResults; k++) {
          workflowVerificationResultService.addWorkflowVerificationResult(
              WorkflowVerificationResult.builder()
                  .accountId(accountId)
                  .appId(appId)
                  .stateExecutionId(generateUuid())
                  .serviceId(serviceId)
                  .envId(envId)
                  .workflowId(workflowId)
                  .stateType(StateType.PROMETHEUS.name())
                  .executionStatus(k % 2 == 0 ? ExecutionStatus.SUCCESS : ExecutionStatus.FAILED)
                  .rollback(k % 2 == 0)
                  .analyzed(k % 2 == 0)
                  .build());
        }
      }
      appPermissionMap.get(appId).setServicePermissions(servicePermissions);
      appPermissionMap.get(appId).setEnvPermissions(envPermissions);
      appPermissionMap.get(appId).setWorkflowPermissions(workflowPermissions);
    }
  }
}
