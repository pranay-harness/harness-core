package io.harness.dashboard;

import static io.harness.rule.OwnerRule.RUSHABH;
import static junit.framework.TestCase.fail;

import com.google.inject.Inject;

import io.harness.beans.PageRequest.PageRequestBuilder;
import io.harness.beans.PageResponse;
import io.harness.category.element.UnitTests;
import io.harness.exception.WingsException;
import io.harness.rule.Owner;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;

public class DashboardSettingsServiceTest extends WingsBaseTest {
  @Inject DashboardSettingsService dashboardSettingsService;

  String accountId = "ACCOUNTID";

  @Test
  @Owner(developers = RUSHABH)
  @Category(UnitTests.class)
  public void testDashboardCreate() {
    DashboardSettings dashboardSettings = getDashboardSettings(accountId, 1);

    DashboardSettings settings = dashboardSettingsService.createDashboardSettings(accountId, dashboardSettings);

    validateSettings(dashboardSettings, settings);

    settings = dashboardSettingsService.get(accountId, settings.getUuid());

    validateSettings(dashboardSettings, settings);
  }

  @Test
  @Owner(developers = RUSHABH)
  @Category(UnitTests.class)
  public void testDashboardCreateUpdate() {
    DashboardSettings dashboardSettings = getDashboardSettings(accountId, 1);

    DashboardSettings settings = dashboardSettingsService.createDashboardSettings(accountId, dashboardSettings);

    validateSettings(dashboardSettings, settings);

    settings.setData("dataUpdated");
    settings.setName("updatedName");
    settings.setDescription("updatedDescription");

    DashboardSettings updatedSettings = dashboardSettingsService.updateDashboardSettings(accountId, settings);

    validateSettings(updatedSettings, settings);

    settings.setAccountId("FakeAccountID");
    updatedSettings = dashboardSettingsService.updateDashboardSettings(accountId, settings);

    Assertions.assertThat(updatedSettings.getAccountId()).isEqualTo(accountId);
  }

  @Test
  @Owner(developers = RUSHABH)
  @Category(UnitTests.class)
  public void testDashboardInvalidUpdate() {
    DashboardSettings dashboardSettings = getDashboardSettings(accountId, 1);

    DashboardSettings settings = dashboardSettingsService.createDashboardSettings(accountId, dashboardSettings);

    validateSettings(dashboardSettings, settings);

    settings.setData("dataUpdated");
    settings.setName("updatedName");
    settings.setDescription("updatedDescription");
    settings.setUuid(null);
    try {
      DashboardSettings updatedSettings = dashboardSettingsService.updateDashboardSettings(accountId, settings);
      fail();
    } catch (WingsException e) {
      Assertions.assertThat(e.getMessage()).isEqualTo("Invalid Dashboard update request");
    }
  }

  @Test
  @Owner(developers = RUSHABH)
  @Category(UnitTests.class)
  public void testDashboardDelete() {
    DashboardSettings dashboardSettings = getDashboardSettings(accountId, 1);

    DashboardSettings settings = dashboardSettingsService.createDashboardSettings(accountId, dashboardSettings);

    validateSettings(dashboardSettings, settings);

    settings = dashboardSettingsService.get(accountId, settings.getUuid());

    validateSettings(dashboardSettings, settings);

    Assertions.assertThat(dashboardSettingsService.deleteDashboardSettings(accountId, settings.getUuid())).isTrue();
    Assertions.assertThat(dashboardSettingsService.get(accountId, settings.getUuid())).isNull();
    Assertions.assertThat(dashboardSettingsService.deleteDashboardSettings(accountId, settings.getUuid())).isFalse();
    Assertions.assertThat(dashboardSettingsService.deleteDashboardSettings("Fake", accountId)).isFalse();
  }

  @Test
  @Owner(developers = RUSHABH)
  @Category(UnitTests.class)
  public void testGetDashboardSummary() {
    for (int i = 0; i < 10; i++) {
      DashboardSettings dashboardSettings = getDashboardSettings(accountId, i);
      DashboardSettings settings = dashboardSettingsService.createDashboardSettings(accountId, dashboardSettings);
    }

    PageResponse<DashboardSettings> pageResponse = dashboardSettingsService.getDashboardSettingSummary(
        accountId, PageRequestBuilder.aPageRequest().withOffset("0").withLimit("20").build());

    Assertions.assertThat(pageResponse.getTotal()).isEqualTo(10);

    Assertions.assertThat(pageResponse.getResponse().get(0).getData()).isNullOrEmpty();
  }

  private void validateSettings(DashboardSettings source, DashboardSettings target) {
    Assertions.assertThat(target.getAccountId()).isEqualTo(source.getAccountId());
    Assertions.assertThat(target.getData()).isEqualTo(source.getData());
    Assertions.assertThat(target.getDescription()).isEqualTo(source.getDescription());
    Assertions.assertThat(target.getName()).isEqualTo(source.getName());
    Assertions.assertThat(target.getUuid()).isNotEmpty();
  }

  private DashboardSettings getDashboardSettings(String accountId, int value) {
    return DashboardSettings.builder()
        .accountId(accountId)
        .data("fakedata" + value)
        .description("fakedescription" + value)
        .name("dashboard" + value)
        .build();
  }
}
