/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.VUK;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.TaskGroup;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.SelectorCapability;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.TaskType;
import software.wings.beans.alert.NoEligibleDelegatesAlert;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
public class NoEligibleDelegatesAlertTest extends WingsBaseTest {
  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testMatches() {
    String accountId1 = generateUuid();
    String accountId2 = generateUuid();

    List<ExecutionCapability> executionCapabilities = ImmutableList.of(
        SelectorCapability.builder().selectors(Stream.of("a", "b").collect(Collectors.toSet())).build());

    NoEligibleDelegatesAlert n1 = NoEligibleDelegatesAlert.builder().accountId(accountId1).build();
    NoEligibleDelegatesAlert n2 = NoEligibleDelegatesAlert.builder().accountId(accountId2).build();

    NoEligibleDelegatesAlert n3 = NoEligibleDelegatesAlert.builder()
                                      .accountId(accountId1)
                                      .taskGroup(TaskGroup.HTTP)
                                      .taskType(TaskType.HTTP)
                                      .appId("appId")
                                      .envId("envId")
                                      .infraMappingId("infraMappingId")
                                      .executionCapabilities(executionCapabilities)
                                      .build();

    NoEligibleDelegatesAlert n4 = NoEligibleDelegatesAlert.builder()
                                      .accountId(accountId1)
                                      .taskGroup(TaskGroup.HTTP)
                                      .taskType(TaskType.HTTP)
                                      .appId("appId")
                                      .envId("envId")
                                      .infraMappingId("infraMappingId")
                                      .executionCapabilities(executionCapabilities)
                                      .build();

    assertThat(n1.matches(n2)).isFalse();
    assertThat(n3.matches(n4)).isTrue();
    assertThat(n1.matches(n3)).isFalse();
  }
}
