package io.harness.cvng.core.services.impl;

import io.harness.cvng.activity.services.api.ActivityService;
import io.harness.cvng.beans.activity.ActivityType;
import io.harness.cvng.core.beans.ChangeSummaryDTO;
import io.harness.cvng.core.beans.ChangeSummaryDTO.CategoryCountDetails;
import io.harness.cvng.core.beans.change.event.ChangeEventDTO;
import io.harness.cvng.core.beans.monitoredService.ChangeSourceDTO;
import io.harness.cvng.core.beans.params.ServiceEnvironmentParams;
import io.harness.cvng.core.services.api.ChangeEventService;
import io.harness.cvng.core.services.api.monitoredService.ChangeSourceService;
import io.harness.cvng.core.transformer.changeEvent.ChangeEventEntityAndDTOTransformer;
import io.harness.cvng.core.types.ChangeCategory;
import io.harness.cvng.core.types.ChangeSourceType;

import com.google.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;

public class ChangeEventServiceImpl implements ChangeEventService {
  @Inject ChangeSourceService changeSourceService;
  @Inject ChangeEventEntityAndDTOTransformer transformer;
  @Inject ActivityService activityService;

  @Override
  public Boolean register(String accountId, ChangeEventDTO changeEventDTO) {
    ServiceEnvironmentParams serviceEnvironmentParams = ServiceEnvironmentParams.builder()
                                                            .accountIdentifier(accountId)
                                                            .orgIdentifier(changeEventDTO.getOrgIdentifier())
                                                            .projectIdentifier(changeEventDTO.getProjectIdentifier())
                                                            .serviceIdentifier(changeEventDTO.getServiceIdentifier())
                                                            .environmentIdentifier(changeEventDTO.getEnvIdentifier())
                                                            .build();
    Set<ChangeSourceDTO> changeSourceDTOS =
        changeSourceService.getByType(serviceEnvironmentParams, changeEventDTO.getType())
            .stream()
            .filter(source -> source.isEnabled())
            .collect(Collectors.toSet());
    if (changeSourceDTOS.isEmpty()) {
      return false;
    }
    activityService.upsert(transformer.getEntity(changeEventDTO));
    return true;
  }

  @Override
  public List<ChangeEventDTO> get(ServiceEnvironmentParams serviceEnvironmentParams,
      List<String> changeSourceIdentifiers, Instant startTime, Instant endTime, List<ChangeCategory> changeCategories) {
    List<ActivityType> activityTypes =
        CollectionUtils.emptyIfNull(changeCategories)
            .stream()
            .flatMap(changeCategory -> ChangeSourceType.getForCategory(changeCategory).stream())
            .map(changeSourceType -> changeSourceType.getActivityType())
            .collect(Collectors.toList());
    return activityService.get(serviceEnvironmentParams, changeSourceIdentifiers, startTime, endTime, activityTypes)
        .stream()
        .map(changeEvent -> transformer.getDto(changeEvent))
        .collect(Collectors.toList());
  }

  @Override
  public ChangeSummaryDTO getChangeSummary(ServiceEnvironmentParams serviceEnvironmentParams,
      List<String> changeSourceIdentifiers, Instant startTime, Instant endTime) {
    return ChangeSummaryDTO.builder()
        .categoryCountMap(Arrays.stream(ChangeCategory.values())
                              .collect(Collectors.toMap(Function.identity(),
                                  changeCategory
                                  -> getCountDetails(serviceEnvironmentParams, changeSourceIdentifiers, startTime,
                                      endTime, changeCategory))))
        .build();
  }

  private CategoryCountDetails getCountDetails(ServiceEnvironmentParams serviceEnvironmentParams,
      List<String> changeSourceIdentifiers, Instant startTime, Instant endTime, ChangeCategory changeCategory) {
    Instant startTimeOfPreviousWindow = startTime.minus(Duration.between(startTime, endTime));
    List<ActivityType> activityTypes = ChangeSourceType.getForCategory(changeCategory)
                                           .stream()
                                           .map(changeSourceType -> changeSourceType.getActivityType())
                                           .collect(Collectors.toList());
    return CategoryCountDetails.builder()
        .count(activityService.getCount(
            serviceEnvironmentParams, changeSourceIdentifiers, startTime, endTime, activityTypes))
        .countInPrecedingWindow(activityService.getCount(
            serviceEnvironmentParams, changeSourceIdentifiers, startTimeOfPreviousWindow, startTime, activityTypes))
        .build();
  }
}
