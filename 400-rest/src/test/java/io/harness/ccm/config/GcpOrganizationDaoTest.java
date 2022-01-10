/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ccm.config;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.rule.OwnerRule.HANTANG;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.ccm.config.GcpOrganization.GcpOrganizationKeys;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;

import com.google.inject.Inject;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(CE)
public class GcpOrganizationDaoTest extends WingsBaseTest {
  private String accountId = "ACCOUNT_ID";
  private String organizationName1 = "ORGANIZATION_NAME_1";
  private String organizationName2 = "ORGANIZATION_NAME_2";
  private GcpOrganization gcpOrganization1;
  private GcpOrganization gcpOrganization2;
  @Inject private GcpOrganizationDao gcpOrganizationDao;

  @Before
  public void setUp() {
    gcpOrganization1 = GcpOrganization.builder().accountId(accountId).organizationName(organizationName1).build();
    gcpOrganization2 = GcpOrganization.builder().accountId(accountId).organizationName(organizationName2).build();
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldUpsert() {
    GcpOrganization actualGcpOrganization1 = gcpOrganizationDao.upsert(gcpOrganization1);
    assertThat(actualGcpOrganization1)
        .isEqualToIgnoringGivenFields(actualGcpOrganization1, GcpOrganizationKeys.uuid, GcpOrganizationKeys.createdAt,
            GcpOrganizationKeys.lastUpdatedAt);
    GcpOrganization actualGcpOrganization2 = gcpOrganizationDao.upsert(gcpOrganization2);
    assertThat(actualGcpOrganization2.getUuid()).isEqualTo(actualGcpOrganization1.getUuid());
    assertThat(actualGcpOrganization2)
        .isEqualToIgnoringGivenFields(actualGcpOrganization2, GcpOrganizationKeys.uuid, GcpOrganizationKeys.createdAt,
            GcpOrganizationKeys.lastUpdatedAt);
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void testSave() {
    String uuid = gcpOrganizationDao.save(gcpOrganization1);
    assertThat(uuid).isNotNull();
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void testGet() {
    GcpOrganization upsertedGcpOrganization = gcpOrganizationDao.upsert(gcpOrganization1);
    GcpOrganization gcpOrganization = gcpOrganizationDao.get(upsertedGcpOrganization.getUuid());
    assertThat(gcpOrganization.getUuid()).isEqualTo(upsertedGcpOrganization.getUuid());
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldList() {
    gcpOrganizationDao.save(gcpOrganization1);
    gcpOrganizationDao.save(gcpOrganization2);
    List<GcpOrganization> gcpOrganizations = gcpOrganizationDao.list(accountId);
    assertThat(gcpOrganizations).hasSize(2);
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldDelete() {
    GcpOrganization organization = gcpOrganizationDao.upsert(gcpOrganization1);
    gcpOrganizationDao.delete(accountId, organization.getUuid());
    List<GcpOrganization> gcpOrganizations = gcpOrganizationDao.list(accountId);
    assertThat(gcpOrganizations).hasSize(0);
  }
}
