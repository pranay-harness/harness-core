package io.harness.ng.core.activityhistory.impl;

import static io.harness.rule.OwnerRule.DEEPAK;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.ng.core.NGCoreBaseTest;
import io.harness.ng.core.activityhistory.ActivityHistoryTestHelper;
import io.harness.ng.core.activityhistory.dto.NGActivityDTO;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.springframework.data.domain.Page;

public class NGActivityServiceImplTest extends NGCoreBaseTest {
  @Inject @InjectMocks NGActivityServiceImpl activityHistoryService;

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void listAllActivityOfAAccountLevelEntity() {
    String accountIdentifier = "accountIdentifier";
    String referredEntityIdentifier = "referredEntityIdentifier";
    for (int i = 0; i < 10; i++) {
      NGActivityDTO activityHistory =
          ActivityHistoryTestHelper.createActivityHistoryDTO(accountIdentifier, null, null, referredEntityIdentifier);
      activityHistoryService.save(activityHistory);
    }
    Page<NGActivityDTO> activityHistories =
        activityHistoryService.list(0, 20, accountIdentifier, null, null, referredEntityIdentifier);
    assertThat(activityHistories.getTotalElements()).isEqualTo(10);
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void listAllActivityOfAOrgLevelEntity() {
    String accountIdentifier = "accountIdentifier";
    String orgIdentifier = "orgIdentifier";
    String referredEntityIdentifier = "referredEntityIdentifier";
    for (int i = 0; i < 10; i++) {
      NGActivityDTO activityHistory = ActivityHistoryTestHelper.createActivityHistoryDTO(
          accountIdentifier, orgIdentifier, null, referredEntityIdentifier);
      activityHistoryService.save(activityHistory);
    }
    Page<NGActivityDTO> activityHistories =
        activityHistoryService.list(0, 20, accountIdentifier, orgIdentifier, null, referredEntityIdentifier);
    assertThat(activityHistories.getTotalElements()).isEqualTo(10);
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void listAllActivityOfAProjectLevelEntity() {
    String accountIdentifier = "accountIdentifier";
    String orgIdentifier = "orgIdentifier";
    String projectIdentifier = "projectIdentifier";
    String referredEntityIdentifier = "referredEntityIdentifier";
    for (int i = 0; i < 10; i++) {
      NGActivityDTO activityHistory = ActivityHistoryTestHelper.createActivityHistoryDTO(
          accountIdentifier, orgIdentifier, projectIdentifier, referredEntityIdentifier);
      activityHistoryService.save(activityHistory);
    }
    Page<NGActivityDTO> activityHistories = activityHistoryService.list(
        0, 20, accountIdentifier, orgIdentifier, projectIdentifier, referredEntityIdentifier);
    assertThat(activityHistories.getTotalElements()).isEqualTo(10);
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void save() {
    String accountIdentifier = "accountIdentifier";
    String orgIdentifier = "orgIdentifier";
    String projectIdentifier = "projectIdentifier";
    String referredEntityIdentifier = "referredEntityIdentifier";
    NGActivityDTO savedActivityHistory = activityHistoryService.save(ActivityHistoryTestHelper.createActivityHistoryDTO(
        accountIdentifier, orgIdentifier, projectIdentifier, referredEntityIdentifier));
    assertThat(savedActivityHistory).isNotNull();
  }
}