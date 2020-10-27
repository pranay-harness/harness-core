package io.harness.ng.core.activityhistory.impl;

import static io.harness.NGCommonEntityConstants.MONGODB_ID;
import static io.harness.ng.core.activityhistory.NGActivityStatus.FAILED;
import static io.harness.ng.core.activityhistory.NGActivityStatus.SUCCESS;
import static io.harness.ng.core.activityhistory.NGActivityType.CONNECTIVITY_CHECK;
import static io.harness.ng.core.activityhistory.dto.TimeGroupType.DAY;
import static io.harness.ng.core.activityhistory.dto.TimeGroupType.HOUR;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.UnknownEnumTypeException;
import io.harness.ng.core.activityhistory.EntityActivityQueryCriteriaHelper;
import io.harness.ng.core.activityhistory.dto.NGActivitySummaryDTO;
import io.harness.ng.core.activityhistory.dto.NGActivitySummaryDTO.NGActivitySummaryKeys;
import io.harness.ng.core.activityhistory.dto.TimeGroupType;
import io.harness.ng.core.activityhistory.entity.NGActivity.ActivityHistoryEntityKeys;
import io.harness.ng.core.activityhistory.repository.NGActivityRepository;
import io.harness.ng.core.activityhistory.service.EntityActivitySummaryService;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.ArithmeticOperators;
import org.springframework.data.mongodb.core.aggregation.ArithmeticOperators.Add;
import org.springframework.data.mongodb.core.aggregation.ArithmeticOperators.Divide;
import org.springframework.data.mongodb.core.aggregation.ArithmeticOperators.Multiply;
import org.springframework.data.mongodb.core.aggregation.BooleanOperators.And;
import org.springframework.data.mongodb.core.aggregation.ComparisonOperators;
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.List;

@Singleton
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@Slf4j
public class EntityActivitySummaryServiceImpl implements EntityActivitySummaryService {
  private NGActivityRepository activityRepository;
  private EntityActivityQueryCriteriaHelper entityActivityQueryCriteriaHelper;
  private static final String intervalField = "intervalField";
  private static final String isSuccessfulActivityField = "isSuccessfulActivity";
  private static final String isFailedActivityField = "isFailedActivity";
  private static final String isConnectivityFailureField = "isConnectivityFailure";

  @Override
  public Page<NGActivitySummaryDTO> listActivitySummary(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String referredEntityIdentifier, TimeGroupType timeGroupType, long start, long end) {
    Criteria activitySummaryCriteria = createActivitySummaryCriteria(
        accountIdentifier, orgIdentifier, projectIdentifier, referredEntityIdentifier, start, end);
    MatchOperation matchStage = Aggregation.match(activitySummaryCriteria);
    ProjectionOperation projectionWhichAddsMinuteInterval = getProjectionWhichAddMinuteInterval(timeGroupType);
    GroupOperation groupByID = getGroupQueryToGroupResultsByIntervals(timeGroupType);
    ProjectionOperation projectingTheStartAndEndTime =
        getProjectionForStartAndEndTime(getTimeUnitToGroupBy(timeGroupType));
    SortOperation sortOperation = getStartingTimeAscendingSortOperation();
    Aggregation aggregation = Aggregation.newAggregation(
        matchStage, projectionWhichAddsMinuteInterval, groupByID, projectingTheStartAndEndTime, sortOperation);
    List<NGActivitySummaryDTO> activitySummaries =
        activityRepository.aggregate(aggregation, NGActivitySummaryDTO.class).getMappedResults();
    return new PageImpl<>(activitySummaries);
  }

  private long getTimeUnitToGroupBy(TimeGroupType timeGroupType) {
    if (timeGroupType == DAY) {
      return 24 * 60 * 60 * 1000;
    } else if (timeGroupType == HOUR) {
      return 60 * 60 * 1000;
    } else {
      throw new UnknownEnumTypeException("Time Group Type", String.valueOf(timeGroupType));
    }
  }

  private SortOperation getStartingTimeAscendingSortOperation() {
    return new SortOperation(Sort.by(NGActivitySummaryKeys.startTime).ascending());
  }

  private ProjectionOperation getProjectionWhichAddMinuteInterval(TimeGroupType timeGroupType) {
    long time = getTimeUnitToGroupBy(timeGroupType);
    And successFulActivityCriteria = getCriteriaForSuccessFulNonHearbeatActivity();
    And failedActivityCriteria = getCriteriaForFailedNonHearbeatActivity();
    And failedConnectivityCheck = getCriteriaForFailedConnectivityCheck();

    return Aggregation.project()
        .and(ArithmeticOperators.valueOf(Divide.valueOf(ActivityHistoryEntityKeys.activityTime).divideBy(time)).floor())
        .as(intervalField)
        .and(ConditionalOperators.when(successFulActivityCriteria).then(1).otherwise(0))
        .as(isSuccessfulActivityField)
        .and(ConditionalOperators.Cond.when(failedActivityCriteria).then(1).otherwise(0))
        .as(isFailedActivityField)
        .and(ConditionalOperators.when(failedConnectivityCheck).then(1).otherwise(0))
        .as(isConnectivityFailureField);
  }

  private ProjectionOperation getProjectionForStartAndEndTime(long timeUnit) {
    return Aggregation.project()
        .and(Multiply.valueOf(MONGODB_ID).multiplyBy(timeUnit))
        .as(NGActivitySummaryKeys.startTime)
        .and(ArithmeticOperators.valueOf(Add.valueOf(MONGODB_ID).add(1)).multiplyBy(timeUnit))
        .as(NGActivitySummaryKeys.endTime)
        .andInclude(NGActivitySummaryKeys.successfulActivitiesCount, NGActivitySummaryKeys.failedActivitiesCount,
            NGActivitySummaryKeys.heartBeatFailuresCount);
  }

  private And getCriteriaForSuccessFulNonHearbeatActivity() {
    return And.and(
        ComparisonOperators.valueOf(ActivityHistoryEntityKeys.type).notEqualToValue(CONNECTIVITY_CHECK.toString()),
        ComparisonOperators.valueOf(ActivityHistoryEntityKeys.activityStatus).equalToValue(SUCCESS.toString()));
  }

  private And getCriteriaForFailedNonHearbeatActivity() {
    return And.and(
        ComparisonOperators.valueOf(ActivityHistoryEntityKeys.type).notEqualToValue(CONNECTIVITY_CHECK.toString()),
        ComparisonOperators.valueOf(ActivityHistoryEntityKeys.activityStatus).equalToValue(FAILED.toString()));
  }

  private And getCriteriaForFailedConnectivityCheck() {
    return And.and(
        ComparisonOperators.valueOf(ActivityHistoryEntityKeys.type).equalToValue(CONNECTIVITY_CHECK.toString()),
        ComparisonOperators.valueOf(ActivityHistoryEntityKeys.activityStatus).equalToValue(FAILED.toString()));
  }

  private GroupOperation getGroupQueryToGroupResultsByIntervals(TimeGroupType timeGroupType) {
    return group(intervalField)
        .sum(isSuccessfulActivityField)
        .as(NGActivitySummaryKeys.successfulActivitiesCount)
        .sum(isFailedActivityField)
        .as(NGActivitySummaryKeys.failedActivitiesCount)
        .sum(isConnectivityFailureField)
        .as(NGActivitySummaryKeys.heartBeatFailuresCount);
  }

  private Criteria createActivitySummaryCriteria(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String referredEntityIdentifier, long startTime, long endTime) {
    Criteria criteria = new Criteria();
    entityActivityQueryCriteriaHelper.populateEntityFQNFilterInCriteria(
        criteria, accountIdentifier, orgIdentifier, projectIdentifier, referredEntityIdentifier);
    entityActivityQueryCriteriaHelper.addTimeFilterInTheCriteria(criteria, startTime, endTime);
    return criteria;
  }
}
