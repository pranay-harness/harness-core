/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.tasklet.support;

import static io.harness.rule.OwnerRule.HITESH;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.batch.processing.service.intfc.WorkloadRepository;
import io.harness.category.element.UnitTests;
import io.harness.ccm.commons.entities.k8s.K8sWorkload;
import io.harness.rule.Owner;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class K8SWorkloadServiceTest extends CategoryTest {
  @InjectMocks private K8SWorkloadService k8SWorkloadService;
  @Mock private WorkloadRepository workloadRepository;

  private static final String CLUSTER_ID = "clusterId";
  private static final String ACCOUNT_ID = "accountId";
  private static final String WORKLOAD_NAME = "workloadName";
  private static final Map<String, String> LABELS = ImmutableMap.of("key1", "value1", "key2", "value2");

  @Before
  public void setup() throws IOException {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testGetK8sWorkloadLabel() {
    when(workloadRepository.getWorkload(ACCOUNT_ID, CLUSTER_ID, ImmutableSet.of(WORKLOAD_NAME)))
        .thenReturn(ImmutableList.of(getK8sWorkload()));
    k8SWorkloadService.updateK8sWorkloadLabelCache(ACCOUNT_ID, CLUSTER_ID, ImmutableSet.of(WORKLOAD_NAME));
    Map<String, String> k8sWorkloadLabel =
        k8SWorkloadService.getK8sWorkloadLabel(ACCOUNT_ID, CLUSTER_ID, WORKLOAD_NAME);
    assertThat(k8sWorkloadLabel).containsExactlyEntriesOf(LABELS);
  }

  private K8sWorkload getK8sWorkload() {
    return K8sWorkload.builder().accountId(ACCOUNT_ID).clusterId(CLUSTER_ID).name(WORKLOAD_NAME).labels(LABELS).build();
  }
}
