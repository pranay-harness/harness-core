package io.harness.ng.core.activityhistory.impl;

import static io.harness.ng.core.activityhistory.NGActivityType.ENTITY_USAGE;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.UnexpectedException;
import io.harness.ng.core.activityhistory.dto.NGActivityDTO;
import io.harness.ng.core.activityhistory.entity.NGActivity;
import io.harness.ng.core.activityhistory.entity.NGActivity.ActivityHistoryEntityKeys;
import io.harness.ng.core.activityhistory.mapper.NGActivityDTOToEntityMapper;
import io.harness.ng.core.activityhistory.mapper.NGActivityEntityToDTOMapper;
import io.harness.ng.core.activityhistory.repository.NGActivityRepository;
import io.harness.ng.core.activityhistory.service.NGActivityService;
import io.harness.utils.FullyQualifiedIdentifierHelper;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;

@Singleton
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@Slf4j
public class NGActivityServiceImpl implements NGActivityService {
  NGActivityRepository activityRepository;
  NGActivityEntityToDTOMapper activityEntityToDTOMapper;
  NGActivityDTOToEntityMapper activityDTOToEntityMapper;

  @Override
  public Page<NGActivityDTO> list(int page, int size, String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String referredEntityIdentifier) {
    Criteria criteria = createCriteriaForEntityUsageActivity(
        accountIdentifier, orgIdentifier, projectIdentifier, referredEntityIdentifier);
    Pageable pageable =
        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, ActivityHistoryEntityKeys.activityTime));
    Page<NGActivity> activities = activityRepository.findAll(criteria, pageable);
    return activities.map(activityEntityToDTOMapper::writeDTO);
  }

  private Criteria createCriteriaForEntityUsageActivity(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String referredEntityIdentifier) {
    Criteria criteria = new Criteria();
    criteria.and(ActivityHistoryEntityKeys.type).is(String.valueOf(ENTITY_USAGE));
    criteria.and(ActivityHistoryEntityKeys.referredEntityFQN)
        .is(FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(
            accountIdentifier, orgIdentifier, projectIdentifier, referredEntityIdentifier));
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
}
