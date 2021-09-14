/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.services.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KAMAL;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.beans.HostRecordDTO;
import io.harness.cvng.core.services.api.HostRecordService;
import io.harness.rule.Owner;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import java.time.Instant;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class HostRecordServiceImplTest extends CvNextGenTestBase {
  @Inject private HostRecordService hostRecordService;

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testSave_withGet() {
    HostRecordDTO hostRecordDTO = HostRecordDTO.builder()
                                      .accountId(generateUuid())
                                      .startTime(Instant.now())
                                      .endTime(Instant.now())
                                      .hosts(Sets.newHashSet("h1", "h2"))
                                      .verificationTaskId(generateUuid())
                                      .build();
    hostRecordService.save(Lists.newArrayList(hostRecordDTO));
    assertThat(hostRecordService.get(
                   hostRecordDTO.getVerificationTaskId(), hostRecordDTO.getStartTime(), hostRecordDTO.getEndTime()))
        .isEqualTo(Sets.newHashSet("h1", "h2"));
  }
}
