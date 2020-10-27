package io.harness.ng.core.activityhistory.impl;

import static io.harness.rule.OwnerRule.DEEPAK;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.category.element.UnitTests;
import io.harness.ng.core.NGCoreTestBase;
import io.harness.ng.core.activityhistory.ActivityHistoryTestHelper;
import io.harness.ng.core.activityhistory.NGActivityStatus;
import io.harness.ng.core.activityhistory.NGActivityType;
import io.harness.ng.core.activityhistory.dto.NGActivityDTO;
import io.harness.ng.core.activityhistory.dto.NGActivitySummaryDTO;
import io.harness.ng.core.activityhistory.dto.TimeGroupType;
import io.harness.ng.core.activityhistory.service.NGActivityService;
import io.harness.rule.Owner;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.core.aggregation.DateOperators.Timezone;

import java.util.Date;
import java.util.List;
import java.util.Random;

@Singleton
@Slf4j
public class EntityActivitySummaryServiceImplTest extends NGCoreTestBase {
  @Inject @InjectMocks EntityActivitySummaryServiceImpl entityActivitySummaryService;
  @Inject NGActivityService ngActivityService;

  private static final long DAY_IN_MS = 1000 * 60 * 60 * 24;
  private static final String accountIdentifier = "accountIdentifier";
  private static final String orgIdentifier = "orgIdentifier";
  private static final String projectIdentifier = "projectIdentifier";
  private static final String connectorIdentifier = "connectorIdentifier";

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void listActivitySummaryForDays() {
    long currentTime = System.currentTimeMillis();
    long sevenDaysAgo = currentTime - 7 * DAY_IN_MS;
    long twoDaysAgo = currentTime - 2 * DAY_IN_MS;
    createDataForEachDay(sevenDaysAgo, twoDaysAgo);
    Page<NGActivitySummaryDTO> activities = entityActivitySummaryService.listActivitySummary(accountIdentifier,
        orgIdentifier, projectIdentifier, connectorIdentifier, TimeGroupType.DAY, sevenDaysAgo, twoDaysAgo);
    assertThat(activities).isNotNull();
  }

  private long getRandomNumberUsingNextInt(long minimum, long maximum) {
    long diff = maximum - minimum;
    return new Random().nextLong() % diff + minimum;
  }

  private void createDataForEachDay(long startTime, long endTime) {
    for (long time = startTime, dayCount = 0; time < endTime; time += DAY_IN_MS, dayCount += 1) {
      long numberOfHeartBeatFailures = 8 + dayCount;
      long numberOfSuccessfulEntityUsage = 10 + dayCount;
      long numberOfFailedEntityUsage = 4 + dayCount;

      for (long i = 0; i < numberOfSuccessfulEntityUsage; i++) {
        long activityTime = getRandomNumberUsingNextInt(time, time + DAY_IN_MS);
        NGActivityDTO activity =
            ActivityHistoryTestHelper.createActivityHistoryDTO(accountIdentifier, orgIdentifier, projectIdentifier,
                connectorIdentifier, NGActivityStatus.SUCCESS, activityTime, NGActivityType.ENTITY_USAGE);
        ngActivityService.save(activity);
      }

      for (long i = 0; i < numberOfFailedEntityUsage; i++) {
        long activityTime = getRandomNumberUsingNextInt(time, time + DAY_IN_MS);
        NGActivityDTO activity = ActivityHistoryTestHelper.createActivityHistoryDTO(accountIdentifier, orgIdentifier,
            projectIdentifier, connectorIdentifier, NGActivityStatus.FAILED, activityTime, NGActivityType.ENTITY_USAGE);
        ngActivityService.save(activity);
      }

      for (long i = 0; i < numberOfHeartBeatFailures; i++) {
        long activityTime = getRandomNumberUsingNextInt(time, time + DAY_IN_MS);
        NGActivityDTO activity =
            ActivityHistoryTestHelper.createActivityHistoryDTO(accountIdentifier, orgIdentifier, projectIdentifier,
                connectorIdentifier, NGActivityStatus.FAILED, activityTime, NGActivityType.CONNECTIVITY_CHECK);
        ngActivityService.save(activity);
      }
    }
  }
}