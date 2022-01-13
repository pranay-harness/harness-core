/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.k8sLabel;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.rule.OwnerRule.SHUBHANSHU;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.category.element.UnitTests;
import io.harness.ccm.cluster.dao.K8sWorkloadDao;
import io.harness.ccm.commons.entities.k8s.K8sWorkload;
import io.harness.rule.Owner;

import software.wings.graphql.datafetcher.AbstractDataFetcherTestBase;
import software.wings.graphql.datafetcher.billing.BillingStatsDefaultKeys;
import software.wings.graphql.schema.query.QLClustersQueryParameters;
import software.wings.graphql.schema.type.QLK8sLabelConnection;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLIdOperator;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataLabelFilter;
import software.wings.graphql.schema.type.aggregation.billing.QLK8sLabelInput;
import software.wings.graphql.schema.type.aggregation.k8sLabel.QLK8sLabelFilter;

import com.google.inject.Inject;
import graphql.execution.MergedSelectionSet;
import graphql.schema.DataFetchingFieldSelectionSet;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.SelectedField;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
public class k8sLabelConnectionDataFetcherTest extends AbstractDataFetcherTestBase {
  @Inject private K8sLabelConnectionDataFetcher k8sLabelConnectionDataFetcher;

  @Inject private K8sWorkloadDao k8sWorkloadDao;
  @Inject private K8sLabelHelper k8sLabelHelper;
  private static final String ACCOUNT_ID = "accountId";
  private static final String CLUSTER_ID = "clusterId";
  private static final String SETTING_ID = "settingId";
  private static final String WORKLOAD_KIND = "kind";
  private static final String NAMESPACE = "namespace";
  private static final String UID = "uid";
  private static final String UUID = "uuid";
  private long creationTime;

  private static final DataFetchingFieldSelectionSet testSelectionSet = new DataFetchingFieldSelectionSet() {
    @Override
    public MergedSelectionSet get() {
      return MergedSelectionSet.newMergedSelectionSet().build();
    }
    @Override
    public Map<String, Map<String, Object>> getArguments() {
      return Collections.emptyMap();
    }
    @Override
    public Map<String, GraphQLFieldDefinition> getDefinitions() {
      return Collections.emptyMap();
    }
    @Override
    public boolean contains(String fieldGlobPattern) {
      return false;
    }
    @Override
    public SelectedField getField(String fieldName) {
      return null;
    }
    @Override
    public List<SelectedField> getFields() {
      return Collections.emptyList();
    }
    @Override
    public List<SelectedField> getFields(String fieldGlobPattern) {
      return Collections.emptyList();
    }
  };

  @Before
  public void setUp() {
    Map<String, String> labels = new HashMap<>();
    labels.put("key", "value");
    creationTime = System.currentTimeMillis();
    k8sWorkloadDao.save(getTestWorkload("testWorkload", labels));
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testK8sLabelsConnectionDataFetcher() {
    List<QLK8sLabelFilter> filters = new ArrayList<>();
    QLK8sLabelConnection qlk8sLabelConnection = k8sLabelConnectionDataFetcher.fetchConnection(filters,
        QLClustersQueryParameters.builder()
            .limit(1)
            .offset(0)
            .accountId(ACCOUNT_ID)
            .selectionSet(testSelectionSet)
            .build(),
        null);

    assertThat(qlk8sLabelConnection.getNodes().get(0).getName()).isEqualTo("key");
    assertThat(qlk8sLabelConnection.getNodes().get(0).getValues()[0]).isEqualTo("value");
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testK8sLabelsConnectionDataFetcherWithFilters() {
    List<QLK8sLabelFilter> filters = new ArrayList<>();
    filters.add(
        QLK8sLabelFilter.builder()
            .namespace(QLIdFilter.builder().operator(QLIdOperator.IN).values(new String[] {NAMESPACE}).build())
            .cluster(QLIdFilter.builder().operator(QLIdOperator.IN).values(new String[] {CLUSTER_ID}).build())
            .workloadName(QLIdFilter.builder().operator(QLIdOperator.IN).values(new String[] {"testWorkload"}).build())
            .build());
    QLK8sLabelConnection qlk8sLabelConnection = k8sLabelConnectionDataFetcher.fetchConnection(filters,
        QLClustersQueryParameters.builder()
            .limit(1)
            .offset(0)
            .accountId(ACCOUNT_ID)
            .selectionSet(testSelectionSet)
            .build(),
        null);

    assertThat(qlk8sLabelConnection.getNodes().get(0).getName()).isEqualTo("key");
    assertThat(qlk8sLabelConnection.getNodes().get(0).getValues()[0]).isEqualTo("value");
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testK8sLabelsConnectionDataFetcherWithFailingFilterConditions() {
    List<QLK8sLabelFilter> filters = new ArrayList<>();
    filters.add(
        QLK8sLabelFilter.builder()
            .workloadName(
                QLIdFilter.builder().operator(QLIdOperator.IN).values(new String[] {"Different Workload"}).build())
            .build());
    QLK8sLabelConnection qlk8sLabelConnection = k8sLabelConnectionDataFetcher.fetchConnection(filters,
        QLClustersQueryParameters.builder()
            .limit(1)
            .offset(0)
            .accountId(ACCOUNT_ID)
            .selectionSet(testSelectionSet)
            .build(),
        null);

    assertThat(qlk8sLabelConnection.getNodes()).hasSize(0);
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testGetWorkloadNamesFromLabels() {
    Set<String> workloadNames = k8sLabelHelper.getWorkloadNamesWithNamespacesFromLabels(
        ACCOUNT_ID, CLUSTER_ID, getTestLabelFilter("key", "value"));
    assertThat(workloadNames).hasSize(1);
    assertThat(workloadNames.contains("testWorkload" + BillingStatsDefaultKeys.TOKEN + NAMESPACE)).isTrue();
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testGetWorkloadNamesFromLabelsWithFailingFilterConditions() {
    Set<String> workloadNames = k8sLabelHelper.getWorkloadNamesWithNamespacesFromLabels(
        ACCOUNT_ID, CLUSTER_ID, getTestLabelFilter("different key", "value"));
    assertThat(workloadNames).hasSize(0);
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testGetLabelLinks() {
    Set<String> workloadNames = new HashSet<>();
    workloadNames.add("testWorkload");
    Set<K8sWorkload> workloads = k8sLabelHelper.getLabelLinks(ACCOUNT_ID, workloadNames, "key");
    assertThat(workloads).hasSize(1);
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testGetLabelLinksWithFailingFilterConditions() {
    Set<String> workloadNames = new HashSet<>();
    workloadNames.add("different testWorkload");
    Set<K8sWorkload> workloads = k8sLabelHelper.getLabelLinks(ACCOUNT_ID, workloadNames, "different key");
    assertThat(workloads).hasSize(0);
  }

  private K8sWorkload getTestWorkload(String workloadName, Map<String, String> labels) {
    return K8sWorkload.builder()
        .accountId(ACCOUNT_ID)
        .clusterId(CLUSTER_ID)
        .settingId(SETTING_ID)
        .kind(WORKLOAD_KIND)
        .labels(labels)
        .name(workloadName)
        .namespace(NAMESPACE)
        .uid(UID)
        .uuid(UUID)
        .createdAt(creationTime)
        .lastUpdatedAt(creationTime)
        .build();
  }

  private QLBillingDataLabelFilter getTestLabelFilter(String labelName, String labelValue) {
    QLK8sLabelInput input = QLK8sLabelInput.builder().name(labelName).values(Arrays.asList(labelValue)).build();
    return QLBillingDataLabelFilter.builder().labels(Arrays.asList(input)).build();
  }
}
