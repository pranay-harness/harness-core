/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ccm.communication;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.rule.OwnerRule.SHUBHANSHU;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.category.element.UnitTests;
import io.harness.ccm.communication.entities.CECommunications;
import io.harness.ccm.communication.entities.CommunicationType;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;

import com.google.inject.Inject;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@TargetModule(HarnessModule._490_CE_COMMONS)
@OwnedBy(CE)
public class CECommunicationsDaoTest extends WingsBaseTest {
  private String accountId = "ACCOUNT_ID";
  private CECommunications communications;
  private String email = "user@harness.io";
  private CommunicationType type = CommunicationType.WEEKLY_REPORT;
  private boolean enable = true;

  @Inject private CECommunicationsDao communicationsDao;

  @Before
  public void setUp() {
    communications = CECommunications.builder()
                         .accountId(accountId)
                         .emailId(email)
                         .type(CommunicationType.WEEKLY_REPORT)
                         .enabled(enable)
                         .selfEnabled(false)
                         .build();
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testSave() {
    String uuid = communicationsDao.save(communications);
    assertThat(uuid).isNotNull();
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testSaveWithAccountId() {
    String uuid = communicationsDao.save(accountId, email, type, enable);
    assertThat(uuid).isNotNull();
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testGet() {
    String uuid = communicationsDao.save(communications);
    CECommunications result = communicationsDao.get(accountId, email, type);
    assertThat(result).isNotNull();
    assertThat(result.getUuid()).isEqualTo(uuid);
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testList() {
    String uuid = communicationsDao.save(communications);
    List<CECommunications> result = communicationsDao.list(accountId, email);
    assertThat(result).isNotNull();
    assertThat(result.size()).isEqualTo(1);
    assertThat(result.get(0).getUuid()).isEqualTo(uuid);
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testUpdate() {
    String uuid = communicationsDao.save(communications);
    communicationsDao.update(accountId, email, type, false);
    CECommunications result = communicationsDao.get(accountId, email, type);
    assertThat(result).isNotNull();
    assertThat(result.isEnabled()).isEqualTo(false);
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testDelete() {
    String uuid = communicationsDao.save(communications);
    boolean successful = communicationsDao.delete(uuid);
    assertThat(successful).isTrue();
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testGetEntriesEnabledViaEmail() {
    String uuid = communicationsDao.save(communications);
    List<CECommunications> enabledEntries = communicationsDao.getEntriesEnabledViaEmail(accountId);
    assertThat(enabledEntries.size()).isEqualTo(1);
  }
}
