package io.harness.ng.core.activityhistory.impl;

import static io.harness.ng.core.activityhistory.NGActivityStatus.FAILED;
import static io.harness.ng.core.activityhistory.NGActivityType.CONNECTIVITY_CHECK;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.UnexpectedException;
import io.harness.ng.core.activityhistory.EntityActivityQueryCriteriaHelper;
import io.harness.ng.core.activityhistory.NGActivityStatus;
import io.harness.ng.core.activityhistory.dto.ConnectivityCheckSummaryDTO;
import io.harness.ng.core.activityhistory.dto.ConnectivityCheckSummaryDTO.ConnectivityCheckSummaryKeys;
import io.harness.ng.core.activityhistory.dto.NGActivityDTO;
import io.harness.ng.core.activityhistory.dto.NGActivityListDTO;
import io.harness.ng.core.activityhistory.entity.NGActivity;
import io.harness.ng.core.activityhistory.entity.NGActivity.ActivityHistoryEntityKeys;
import io.harness.ng.core.activityhistory.mapper.NGActivityDTOToEntityMapper;
import io.harness.ng.core.activityhistory.mapper.NGActivityEntityToDTOMapper;
import io.harness.ng.core.activityhistory.repository.NGActivityRepository;
import io.harness.ng.core.activityhistory.service.NGActivityService;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.List;
import java.util.stream.Collectors;

@Singleton
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@Slf4j
public class NGActivityServiceImpl implements NGActivityService {
  private NGActivityRepository activityRepository;
  private EntityActivityQueryCriteriaHelper entityActivityQueryCriteriaHelper;
  NGActivityEntityToDTOMapper activityEntityToDTOMapper;
  NGActivityDTOToEntityMapper activityDTOToEntityMapper;

  @Override
  public NGActivityListDTO list(int page, int size, String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String referredEntityIdentifier, long start, long end, NGActivityStatus status) {
    List<NGActivityDTO> allActivitiesOtherThanConnectivityCheck = getAllActivitiesOtherThanConnectivityCheck(
        page, size, accountIdentifier, orgIdentifier, projectIdentifier, referredEntityIdentifier, start, end, status);
    ConnectivityCheckSummaryDTO connectivityCheckSummary = getConnectivityCheckSummary(
        accountIdentifier, orgIdentifier, projectIdentifier, referredEntityIdentifier, start, end);
    return NGActivityListDTO.builder()
        .activityHistoriesForEntityUsage(allActivitiesOtherThanConnectivityCheck)
        .connectivityCheckSummary(connectivityCheckSummary)
        .build();
  }

  private ConnectivityCheckSummaryDTO getConnectivityCheckSummary(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String referredEntityIdentifier, long start, long end) {
    Criteria connectivityCheckCriteria = createConnectivityCheckCriteria(
        accountIdentifier, orgIdentifier, projectIdentifier, referredEntityIdentifier, start, end);
    MatchOperation matchStage = Aggregation.match(connectivityCheckCriteria);
    GroupOperation groupByID = group().count().as(ConnectivityCheckSummaryKeys.failureCount);
    Aggregation aggregation = Aggregation.newAggregation(matchStage, groupByID);
    return activityRepository.aggregate(aggregation, ConnectivityCheckSummaryDTO.class).getUniqueMappedResult();
  }

  private Criteria createConnectivityCheckCriteria(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String referredEntityIdentifier, long start, long end) {
    Criteria criteria = new Criteria();
    criteria.and(ActivityHistoryEntityKeys.type).is(String.valueOf(CONNECTIVITY_CHECK));
    criteria.and(ActivityHistoryEntityKeys.activityStatus).is(String.valueOf(FAILED));
    entityActivityQueryCriteriaHelper.populateEntityFQNFilterInCriteria(
        criteria, accountIdentifier, orgIdentifier, projectIdentifier, referredEntityIdentifier);
    entityActivityQueryCriteriaHelper.addTimeFilterInTheCriteria(criteria, start, end);
    return criteria;
  }

  private List<NGActivityDTO> getAllActivitiesOtherThanConnectivityCheck(int page, int size, String accountIdentifier,
      String orgIdentifier, String projectIdentifier, String referredEntityIdentifier, long start, long end,
      NGActivityStatus status) {
    Criteria criteria = createCriteriaForEntityUsageActivity(
        accountIdentifier, orgIdentifier, projectIdentifier, referredEntityIdentifier, status, start, end);
    Pageable pageable =
        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, ActivityHistoryEntityKeys.activityTime));
    List<NGActivity> activities = activityRepository.findAll(criteria, pageable).getContent();
    return activities.stream().map(activityEntityToDTOMapper::writeDTO).collect(Collectors.toList());
  }

  private void populateActivityStatusCriteria(Criteria criteria, NGActivityStatus status) {
    if (status != null) {
      criteria.and(ActivityHistoryEntityKeys.activityStatus).is(status);
    }
  }

  private Criteria createCriteriaForEntityUsageActivity(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String referredEntityIdentifier, NGActivityStatus status, long startTime,
      long endTime) {
    Criteria criteria = new Criteria();
    criteria.and(ActivityHistoryEntityKeys.type).ne(String.valueOf(CONNECTIVITY_CHECK));
    entityActivityQueryCriteriaHelper.populateEntityFQNFilterInCriteria(
        criteria, accountIdentifier, orgIdentifier, projectIdentifier, referredEntityIdentifier);
    populateActivityStatusCriteria(criteria, status);
    entityActivityQueryCriteriaHelper.addTimeFilterInTheCriteria(criteria, startTime, endTime);
    return criteria;
  }

  @Override
  public NGActivityDTO save(NGActivityDTO activityInput) {
    NGActivity activityEntity = activityDTOToEntityMapper.toActivityHistoryEntity(activityInput);
    NGActivity savedActivityEntity = null;
    try {
      savedActivityEntity = activityRepository.save(activityEntity);
    } catch (DuplicateKeyException ex) {
      logger.info(String.format("Error while saving the activity history [%s] for [%s]", ex.getMessage(),
          activityEntity.getReferredEntityFQN()));
      throw new UnexpectedException(
          String.format("Error while creating the activity history for [%s]", activityEntity.getReferredEntityFQN()));
    }
    return activityEntityToDTOMapper.writeDTO(savedActivityEntity);
  }

  @Override
  public boolean deleteAllActivitiesOfAnEntity(String accountIdentifier, String entityFQN) {
    long numberOfRecordsDeleted = activityRepository.deleteByReferredEntityFQN(entityFQN);
    return numberOfRecordsDeleted > 0;
  }
}
