package io.harness.ccm.config;

import static io.harness.rule.OwnerRule.HANTANG;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;

import java.util.List;

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
  public void testSave() {
    String uuid = gcpOrganizationDao.save(gcpOrganization1);
    assertThat(uuid).isNotNull();
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void testGet() {
    String uuid = gcpOrganizationDao.save(gcpOrganization1);
    GcpOrganization gcpOrganization = gcpOrganizationDao.get(uuid);
    assertThat(gcpOrganization.getUuid()).isEqualTo(uuid);
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void testList() {
    gcpOrganizationDao.save(gcpOrganization1);
    gcpOrganizationDao.save(gcpOrganization2);
    List<GcpOrganization> gcpOrganizations = gcpOrganizationDao.list(accountId);
    assertThat(gcpOrganizations).hasSize(2);
  }
}
