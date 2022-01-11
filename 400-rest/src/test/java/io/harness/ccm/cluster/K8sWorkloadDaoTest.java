/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ccm.cluster;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.rule.OwnerRule.SHUBHANSHU;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.category.element.UnitTests;
import io.harness.ccm.cluster.dao.K8sWorkloadDao;
import io.harness.ccm.cluster.entities.K8sLabelFilter;
import io.harness.ccm.commons.entities.k8s.K8sWorkload;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;

import com.google.inject.Inject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@TargetModule(HarnessModule._490_CE_COMMONS)
@OwnedBy(CE)
public class K8sWorkloadDaoTest extends WingsBaseTest {
  @Inject private K8sWorkloadDao k8sWorkloadDao;
  private static final String ACCOUNT_ID = "accountId";
  private static final String CLUSTER_ID = "clusterId";
  private static final String SETTING_ID = "settingId";
  private static final String WORKLOAD_KIND = "kind";
  private static final String NAMESPACE = "namespace";
  private static final String UID = "uid";
  private static final String UUID = "uuid";
  private static final long currentTime = System.currentTimeMillis();

  @Before
  public void setUp() {
    Map<String, String> labels = new HashMap<>();
    labels.put("key", "value");
    k8sWorkloadDao.save(getTestWorkload("testWorkload", labels));
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void shouldListAllWorkloadsWithLabelFilter() {
    Map<String, List<String>> labels = new HashMap<>();
    labels.put("key", Arrays.asList("value"));
    List<K8sWorkload> workloads = k8sWorkloadDao.list(ACCOUNT_ID, CLUSTER_ID, labels);
    assertThat(workloads).hasSize(1);
    assertThat(workloads.get(0).getName()).isEqualTo("testWorkload");
    assertThat(workloads.get(0).getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(workloads.get(0).getClusterId()).isEqualTo(CLUSTER_ID);
    assertThat(workloads.get(0).getSettingId()).isEqualTo(SETTING_ID);
    assertThat(workloads.get(0).getUuid()).isEqualTo(UUID);
    assertThat(workloads.get(0).getUid()).isEqualTo(UID);
    assertThat(workloads.get(0).getKind()).isEqualTo(WORKLOAD_KIND);
    assertThat(workloads.get(0).getNamespace()).isEqualTo(NAMESPACE);
  }

  // should not list any workloads as label{ "different key" : "different value"} is not present in test workload
  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void shouldNotListWorkloadsWithLabelFilter() {
    Map<String, List<String>> labels = new HashMap<>();
    labels.put("different key", Arrays.asList("different value"));
    List<K8sWorkload> workloads = k8sWorkloadDao.list(ACCOUNT_ID, CLUSTER_ID, labels);
    assertThat(workloads).hasSize(0);
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void shouldListAllWorkloadsWithWorkloadNameAndLabelFilter() {
    Set<String> workloadNames = new HashSet<>();
    workloadNames.add("testWorkload");
    String labelName = "key";
    List<K8sWorkload> workloads = k8sWorkloadDao.list(ACCOUNT_ID, workloadNames, labelName);
    assertThat(workloads).hasSize(1);
    assertThat(workloads.get(0).getName()).isEqualTo("testWorkload");
    assertThat(workloads.get(0).getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(workloads.get(0).getClusterId()).isEqualTo(CLUSTER_ID);
    assertThat(workloads.get(0).getSettingId()).isEqualTo(SETTING_ID);
    assertThat(workloads.get(0).getUuid()).isEqualTo(UUID);
    assertThat(workloads.get(0).getUid()).isEqualTo(UID);
    assertThat(workloads.get(0).getKind()).isEqualTo(WORKLOAD_KIND);
    assertThat(workloads.get(0).getNamespace()).isEqualTo(NAMESPACE);
  }

  // should not list any workloads as workload name of test workload is different
  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void shouldNotListWorkloadsWithWorkloadNameAndLabelFilter() {
    Set<String> workloadNames = new HashSet<>();
    workloadNames.add("differentTestWorkload");
    String labelName = "key";
    List<K8sWorkload> workloads = k8sWorkloadDao.list(ACCOUNT_ID, workloadNames, labelName);
    assertThat(workloads).hasSize(0);
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void shouldListLabelKeys() {
    K8sLabelFilter filter = K8sLabelFilter.builder()
                                .accountId(ACCOUNT_ID)
                                .startTime(currentTime - 100)
                                .endTime(currentTime + 100)
                                .limit(1)
                                .offset(0)
                                .build();
    List<String> labelKeys = k8sWorkloadDao.listLabelKeys(filter);
    assertThat(labelKeys).hasSize(0);
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void shouldListLabelValues() {
    K8sLabelFilter filter = K8sLabelFilter.builder()
                                .accountId(ACCOUNT_ID)
                                .startTime(currentTime - 100)
                                .endTime(currentTime + 100)
                                .limit(1)
                                .offset(0)
                                .labelName("key")
                                .searchString("val")
                                .build();
    List<String> labelKeys = k8sWorkloadDao.listLabelValues(filter);
    assertThat(labelKeys).hasSize(0);
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
        .lastUpdatedAt(currentTime)
        .build();
  }
}
