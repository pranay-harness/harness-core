package io.harness.cvng.core.services.impl;

import static org.apache.commons.collections4.iterators.PeekingIterator.peekingIterator;

import io.harness.beans.PageRequest.PageRequestBuilder;
import io.harness.beans.SearchFilter.Operator;
import io.harness.beans.SortOrder;
import io.harness.beans.SortOrder.OrderType;
import io.harness.cvng.activity.entities.Activity;
import io.harness.cvng.activity.entities.Activity.ActivityKeys;
import io.harness.cvng.activity.services.api.ActivityService;
import io.harness.cvng.beans.activity.ActivityType;
import io.harness.cvng.beans.change.ChangeCategory;
import io.harness.cvng.beans.change.ChangeEventDTO;
import io.harness.cvng.beans.change.ChangeSourceType;
import io.harness.cvng.core.beans.change.ChangeSummaryDTO;
import io.harness.cvng.core.beans.change.ChangeSummaryDTO.CategoryCountDetails;
import io.harness.cvng.core.beans.change.ChangeTimeline;
import io.harness.cvng.core.beans.change.ChangeTimeline.ChangeTimelineBuilder;
import io.harness.cvng.core.beans.change.ChangeTimeline.TimeRangeDetail;
import io.harness.cvng.core.beans.monitoredService.ChangeSourceDTO;
import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.core.beans.params.ServiceEnvironmentParams;
import io.harness.cvng.core.services.api.ChangeEventService;
import io.harness.cvng.core.services.api.monitoredService.ChangeSourceService;
import io.harness.cvng.core.transformer.changeEvent.ChangeEventEntityAndDTOTransformer;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;

import com.google.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.iterators.PeekingIterator;
import org.apache.commons.lang3.StringUtils;

public class ChangeEventServiceImpl implements ChangeEventService {
  @Inject ChangeSourceService changeSourceService;
  @Inject ChangeEventEntityAndDTOTransformer transformer;
  @Inject ActivityService activityService;

  @Override
  public Boolean register(ChangeEventDTO changeEventDTO) {
    ServiceEnvironmentParams serviceEnvironmentParams = ServiceEnvironmentParams.builder()
                                                            .accountIdentifier(changeEventDTO.getAccountId())
                                                            .orgIdentifier(changeEventDTO.getOrgIdentifier())
                                                            .projectIdentifier(changeEventDTO.getProjectIdentifier())
                                                            .serviceIdentifier(changeEventDTO.getServiceIdentifier())
                                                            .environmentIdentifier(changeEventDTO.getEnvIdentifier())
                                                            .build();
    Optional<ChangeSourceDTO> changeSourceDTOOptional =
        changeSourceService.getByType(serviceEnvironmentParams, changeEventDTO.getType())
            .stream()
            .filter(source -> source.isEnabled())
            .findAny();
    if (!changeSourceDTOOptional.isPresent()) {
      return false;
    }
    if (StringUtils.isEmpty(changeEventDTO.getChangeSourceIdentifier())) {
      changeEventDTO.setChangeSourceIdentifier(changeSourceDTOOptional.get().getIdentifier());
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

  @Override
  public PageResponse<ChangeEventDTO> getPaginated(ProjectParams projectParams, List<String> serviceIdentifiers,
      List<String> environmentIdentifier, Instant startTime, Instant endTime, List<ChangeCategory> changeCategories,
      PageRequest pageRequest) {
    PageRequestBuilder pageRequestBuilder =
        PageRequestBuilder.aPageRequest()
            .withOffset(String.valueOf(pageRequest.getPageIndex() * pageRequest.getPageSize()))
            .withLimit(String.valueOf(pageRequest.getPageSize()))
            .addFilter(ActivityKeys.accountId, Operator.EQ, projectParams.getAccountIdentifier())
            .addFilter(ActivityKeys.orgIdentifier, Operator.EQ, projectParams.getOrgIdentifier())
            .addFilter(ActivityKeys.projectIdentifier, Operator.EQ, projectParams.getProjectIdentifier())
            .addFilter(ActivityKeys.eventTime, Operator.GE, startTime)
            .addFilter(ActivityKeys.eventTime, Operator.LT, endTime)
            .addOrder(SortOrder.Builder.aSortOrder().withField(ActivityKeys.eventTime, OrderType.DESC).build());

    if (CollectionUtils.isNotEmpty(serviceIdentifiers)) {
      pageRequestBuilder.addFilter(ActivityKeys.serviceIdentifier, Operator.IN, serviceIdentifiers.toArray());
    }
    if (CollectionUtils.isNotEmpty(environmentIdentifier)) {
      pageRequestBuilder.addFilter(ActivityKeys.environmentIdentifier, Operator.IN, environmentIdentifier.toArray());
    }
    if (CollectionUtils.isNotEmpty(changeCategories)) {
      List<ActivityType> activityTypes = changeCategories.stream()
                                             .map(ChangeSourceType::getForCategory)
                                             .flatMap(Collection::stream)
                                             .map(ChangeSourceType::getActivityType)
                                             .collect(Collectors.toList());
      pageRequestBuilder.addFilter(ActivityKeys.type, Operator.IN, activityTypes.toArray());
    } else {
      List<ActivityType> activityTypes = Arrays.stream(ChangeSourceType.values())
                                             .map(ChangeSourceType::getActivityType)
                                             .distinct()
                                             .collect(Collectors.toList());
      pageRequestBuilder.addFilter(ActivityKeys.type, Operator.IN, activityTypes.toArray());
    }
    io.harness.beans.PageResponse<Activity> pageResponse = activityService.getPaginated(pageRequestBuilder.build());
    Long totalPages = (pageResponse.getTotal() / pageResponse.getPageSize())
        + ((pageResponse.getTotal() % pageRequest.getPageSize()) == 0 ? 0 : 1);
    return PageResponse.<ChangeEventDTO>builder()
        .pageIndex(pageRequest.getPageIndex())
        .totalPages(totalPages)
        .pageSize(pageRequest.getPageSize())
        .totalItems(pageResponse.getTotal())
        .pageItemCount(pageResponse.size())
        .content(pageResponse.stream().map(transformer::getDto).collect(Collectors.toList()))
        .build();
  }

  @Override
  public ChangeTimeline getTimeline(ProjectParams projectParams, List<String> serviceIdentifiers,
      List<String> environmentIdentifier, Instant startTime, Instant endTime, Integer pointCount) {
    List<ActivityType> activityTypes = Arrays.stream(ChangeSourceType.values())
                                           .map(ChangeSourceType::getActivityType)
                                           .distinct()
                                           .collect(Collectors.toList());
    PageRequestBuilder pageRequestBuilder =
        PageRequestBuilder.aPageRequest()
            .addFieldsIncluded(ActivityKeys.type, ActivityKeys.eventTime)
            .addFilter(ActivityKeys.accountId, Operator.EQ, projectParams.getAccountIdentifier())
            .addFilter(ActivityKeys.orgIdentifier, Operator.EQ, projectParams.getOrgIdentifier())
            .addFilter(ActivityKeys.projectIdentifier, Operator.EQ, projectParams.getProjectIdentifier())
            .addFilter(ActivityKeys.eventTime, Operator.GE, startTime)
            .addFilter(ActivityKeys.eventTime, Operator.LT, endTime)
            .addFilter(ActivityKeys.type, Operator.IN, activityTypes.toArray())
            .addOrder(SortOrder.Builder.aSortOrder().withField(ActivityKeys.eventTime, OrderType.ASC).build());
    pageRequestBuilder.addFilter(ActivityKeys.type, Operator.IN, activityTypes.toArray());
    if (CollectionUtils.isNotEmpty(serviceIdentifiers)) {
      pageRequestBuilder.addFilter(ActivityKeys.serviceIdentifier, Operator.IN, serviceIdentifiers.toArray());
    }
    if (CollectionUtils.isNotEmpty(environmentIdentifier)) {
      pageRequestBuilder.addFilter(ActivityKeys.environmentIdentifier, Operator.IN, environmentIdentifier.toArray());
    }
    io.harness.beans.PageResponse<Activity> pageResponse = activityService.getPaginated(pageRequestBuilder.build());
    Map<ChangeCategory, List<Activity>> changeCategoryActivitiesMap = pageResponse.stream().collect(
        Collectors.groupingBy(activity -> ChangeSourceType.ofActivityType(activity.getType()).getChangeCategory()));
    ChangeTimelineBuilder changeTimelineBuilder = ChangeTimeline.builder();
    Arrays.asList(ChangeCategory.values())
        .forEach(changeCategory
            -> changeTimelineBuilder.categoryTimeline(changeCategory,
                getAggregateDetails(changeCategoryActivitiesMap.getOrDefault(changeCategory, Collections.EMPTY_LIST),
                    startTime, endTime, pointCount)));
    return changeTimelineBuilder.build();
  }

  @Override
  public ChangeSummaryDTO getChangeSummary(ProjectParams projectParams, List<String> serviceIdentifiers,
      List<String> environmentIdentifier, Instant startTime, Instant endTime) {
    return ChangeSummaryDTO.builder()
        .categoryCountMap(Arrays.stream(ChangeCategory.values())
                              .collect(Collectors.toMap(Function.identity(),
                                  changeCategory
                                  -> getCountDetails(projectParams, serviceIdentifiers, environmentIdentifier,
                                      startTime, endTime, changeCategory))))
        .build();
  }

  private List<TimeRangeDetail> getAggregateDetails(
      List<Activity> activitiesSortedByTime, Instant startTime, Instant endTime, Integer pointCount) {
    Duration aggregationDuration = Duration.between(startTime, endTime).dividedBy(pointCount);
    Instant timeRangeStart = startTime, timeRangeEnd = startTime.plus(aggregationDuration);
    Long count = 0L;
    PeekingIterator<Activity> activityIterator = peekingIterator(activitiesSortedByTime.iterator());
    List<TimeRangeDetail> result = new ArrayList<>();
    while (activityIterator.hasNext()) {
      Activity activity = activityIterator.peek();
      if (activity.getEventTime().isBefore(timeRangeEnd)) {
        count++;
        activityIterator.next();
      } else {
        if (count > 0) {
          TimeRangeDetail timeRangeDetail = TimeRangeDetail.builder()
                                                .count(count)
                                                .startTime(timeRangeStart.toEpochMilli())
                                                .endTime(timeRangeEnd.toEpochMilli())
                                                .build();
          result.add(timeRangeDetail);
        }
        count = 0L;
        timeRangeStart = timeRangeEnd;
        timeRangeEnd = timeRangeStart.plus(aggregationDuration);
      }
    }
    if (count > 0) {
      TimeRangeDetail timeRangeDetail = TimeRangeDetail.builder()
                                            .count(count)
                                            .startTime(timeRangeStart.toEpochMilli())
                                            .endTime(timeRangeEnd.toEpochMilli())
                                            .build();
      result.add(timeRangeDetail);
    }
    return result;
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

  private CategoryCountDetails getCountDetails(ProjectParams projectParams, List<String> serviceIdentifiers,
      List<String> environmentIdentifiers, Instant startTime, Instant endTime, ChangeCategory changeCategory) {
    Instant startTimeOfPreviousWindow = startTime.minus(Duration.between(startTime, endTime));
    List<ActivityType> activityTypes = ChangeSourceType.getForCategory(changeCategory)
                                           .stream()
                                           .map(changeSourceType -> changeSourceType.getActivityType())
                                           .collect(Collectors.toList());
    return CategoryCountDetails.builder()
        .count(activityService.getCount(
            projectParams, serviceIdentifiers, environmentIdentifiers, startTime, endTime, activityTypes))
        .countInPrecedingWindow(activityService.getCount(projectParams, serviceIdentifiers, environmentIdentifiers,
            startTimeOfPreviousWindow, startTime, activityTypes))
        .build();
  }
}
