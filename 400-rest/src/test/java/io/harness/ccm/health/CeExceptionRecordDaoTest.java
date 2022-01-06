/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ccm.health;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.rule.OwnerRule.HANTANG;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.ccm.commons.entities.events.CeExceptionRecord;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;

import com.google.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(CE)
public class CeExceptionRecordDaoTest extends WingsBaseTest {
  private String accountId = "ACCOUNT_ID";
  private String clusterId = "CLUSTER_ID";

  private io.harness.ccm.commons.entities.events.CeExceptionRecord exception;
  @Inject CeExceptionRecordDao ceExceptionRecordDao;

  @Before
  public void setUp() {
    exception = io.harness.ccm.commons.entities.events.CeExceptionRecord.builder()
                    .accountId(accountId)
                    .clusterId(clusterId)
                    .message("Exception")
                    .build();
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldSaveAndGet() {
    ceExceptionRecordDao.save(exception);
    CeExceptionRecord savedException = ceExceptionRecordDao.getRecentException(accountId, clusterId, 0);
    assertThat(savedException).isEqualToIgnoringGivenFields(exception, "uuid");
  }
}
