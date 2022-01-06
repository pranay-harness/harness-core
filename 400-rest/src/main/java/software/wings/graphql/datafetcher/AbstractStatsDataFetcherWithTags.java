/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static com.amazonaws.util.CollectionUtils.mergeLists;
import static java.util.function.Function.identity;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.exception.InvalidRequestException;

import software.wings.beans.EntityType;
import software.wings.beans.HarnessTagLink;
import software.wings.graphql.schema.type.aggregation.QLAggregatedData;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.QLDataPoint;
import software.wings.graphql.schema.type.aggregation.QLReference;
import software.wings.graphql.schema.type.aggregation.QLSinglePointData;
import software.wings.graphql.schema.type.aggregation.QLStackedData;
import software.wings.graphql.schema.type.aggregation.QLStackedDataPoint;
import software.wings.graphql.schema.type.aggregation.QLStackedTimeSeriesData;
import software.wings.graphql.schema.type.aggregation.QLStackedTimeSeriesDataPoint;
import software.wings.graphql.schema.type.aggregation.QLTimeSeriesAggregation;
import software.wings.graphql.schema.type.aggregation.QLTimeSeriesData;
import software.wings.graphql.schema.type.aggregation.TagAggregation;
import software.wings.graphql.schema.type.aggregation.deployment.QLDeploymentTagAggregation;
import software.wings.graphql.schema.type.aggregation.deployment.QLDeploymentTagType;

import com.google.api.client.util.ArrayMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(DX)
@Slf4j
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public abstract class AbstractStatsDataFetcherWithTags<A, F, G, S, E, TA extends TagAggregation, EA>
    extends AbstractStatsDataFetcher<A, F, G, S> {
  protected abstract TA getTagAggregation(G groupBy);
  protected abstract EA getEntityAggregation(G groupBy);
  protected abstract EntityType getEntityType(E entityType);

  protected List<TA> getGroupByTag(List<G> groupByList) {
    List<TA> groupByTagList = new ArrayList<>();
    if (isEmpty(groupByList)) {
      return groupByTagList;
    }

    for (G groupBy : groupByList) {
      TA tagAggregation = getTagAggregation(groupBy);
      if (tagAggregation != null) {
        groupByTagList.add(tagAggregation);
      }
    }
    return groupByTagList;
  }

  @Override
  public QLData postFetch(String accountId, List<G> groupByList, QLData qlData) {
    List<TA> groupByTagList = getGroupByTag(groupByList);
    int size = groupByTagList.size();
    if (size == 0) {
      return qlData;
    }

    groupByTagList.removeIf(ta
        -> ta != null
            && (ta instanceof QLDeploymentTagAggregation && (QLDeploymentTagType.DEPLOYMENT == ta.getEntityType())));
    if (isEmpty(groupByTagList)) {
      return qlData;
    }
    TA groupByTagLevel1 = groupByTagList.get(0);
    TA groupByTagLevel2 = null;
    int groupByTagLevel1Position = findFirstGroupByTagPositionExcludingDeploymentTag(groupByList);

    if (groupByTagList.size() > 1) {
      groupByTagLevel2 = groupByTagList.get(1);
    }

    if (qlData instanceof QLSinglePointData || qlData instanceof QLTimeSeriesData) {
      return qlData;
    } else if (qlData instanceof QLAggregatedData) {
      QLAggregatedData qlAggregatedData = (QLAggregatedData) qlData;
      List<QLDataPoint> dataPoints = qlAggregatedData.getDataPoints();
      if (isEmpty(dataPoints)) {
        return qlData;
      }
      List<QLDataPoint> tagDataPoints = getTagDataPoints(accountId, dataPoints, groupByTagLevel1);
      qlData = QLAggregatedData.builder().dataPoints(tagDataPoints).build();
    } else if (qlData instanceof QLStackedData) {
      QLStackedData qlStackedData = (QLStackedData) qlData;
      List<QLStackedDataPoint> stackedDataPoints = qlStackedData.getDataPoints();
      if (isEmpty(stackedDataPoints)) {
        return qlData;
      }

      if (groupByTagLevel1Position == 0) {
        List<QLStackedDataPoint> tagStackedDataPoints =
            getTagStackedDataPoints(accountId, stackedDataPoints, groupByTagLevel1);
        qlStackedData.setDataPoints(tagStackedDataPoints);

        if (groupByTagLevel2 != null) {
          final TA groupByTagLevel2Final = groupByTagLevel2;
          tagStackedDataPoints.forEach(tagStackedDataPoint -> {
            List<QLDataPoint> dataPointList = tagStackedDataPoint.getValues();
            List<QLDataPoint> tagDataPoints = getTagDataPoints(accountId, dataPointList, groupByTagLevel2Final);
            tagStackedDataPoint.setValues(tagDataPoints);
          });
        }
      } else {
        stackedDataPoints.forEach(stackedDataPoint -> {
          List<QLDataPoint> dataPointList = stackedDataPoint.getValues();
          List<QLDataPoint> tagDataPoints = getTagDataPoints(accountId, dataPointList, groupByTagLevel1);
          stackedDataPoint.setValues(tagDataPoints);
        });
      }
    } else if (qlData instanceof QLStackedTimeSeriesData) {
      QLStackedTimeSeriesData qlStackedTimeSeriesData = (QLStackedTimeSeriesData) qlData;
      List<QLStackedTimeSeriesDataPoint> stackedTimeSeriesDataPoints = qlStackedTimeSeriesData.getData();
      if (isEmpty(stackedTimeSeriesDataPoints)) {
        return qlData;
      }
      stackedTimeSeriesDataPoints.forEach(stackedTimeSeriesDataPoint -> {
        List<QLDataPoint> dataPoints =
            getTagDataPoints(accountId, stackedTimeSeriesDataPoint.getValues(), groupByTagLevel1);
        stackedTimeSeriesDataPoint.setValues(dataPoints);
      });
    }
    return qlData;
  }

  protected int findFirstGroupByTagPositionExcludingDeploymentTag(List<G> groupByList) {
    int index = -1;
    for (G groupBy : groupByList) {
      index++;
      TA tagAggregation = getTagAggregation(groupBy);
      if (tagAggregation != null) {
        if (tagAggregation instanceof QLDeploymentTagAggregation
            && QLDeploymentTagType.DEPLOYMENT == tagAggregation.getEntityType()) {
          continue;
        }
        return index;
      }
    }
    return -1;
  }

  protected int findFirstGroupByTagPosition(List<G> groupByList) {
    int index = -1;
    for (G groupBy : groupByList) {
      index++;
      TA tagAggregation = getTagAggregation(groupBy);
      if (tagAggregation != null) {
        return index;
      }
    }
    return -1;
  }

  protected int findFirstGroupByEntityPosition(List<G> groupByList) {
    int index = -1;
    for (G groupBy : groupByList) {
      index++;
      EA entityAggregation = getEntityAggregation(groupBy);
      if (entityAggregation != null) {
        return index;
      }
    }
    return -1;
  }

  protected List<EA> getGroupByEntityListFromTags(
      List<G> groupByList, List<EA> groupByEntityList, List<TA> groupByTagList, QLTimeSeriesAggregation groupByTime) {
    int entityListSize = isEmpty(groupByEntityList) ? 0 : groupByEntityList.size();
    int tagListSize = isEmpty(groupByTagList) ? 0 : groupByTagList.size();
    int timeSize = groupByTime == null ? 0 : 1;

    int totalGroupBy = entityListSize + tagListSize + timeSize;
    if (totalGroupBy > 2) {
      log.warn("The total number of aggregations cannot exceed 2, the observed count is {}", totalGroupBy);
      throw new InvalidRequestException(GENERIC_EXCEPTION_MSG);
    }

    if (tagListSize > 0) {
      if (groupByEntityList == null) {
        groupByEntityList = new ArrayList<>();
      }

      // We need to determine the order so that the correct grouping happens at each level.
      if (entityListSize == 1 && tagListSize == 1) {
        int entityIndex = findFirstGroupByEntityPosition(groupByList);
        int tagIndex = findFirstGroupByTagPosition(groupByList);

        if (entityIndex > tagIndex) {
          groupByEntityList.add(0, getGroupByEntityFromTag(groupByTagList.get(0)));
        } else {
          groupByEntityList.add(getGroupByEntityFromTag(groupByTagList.get(0)));
        }
      } else {
        for (TA groupByTag : groupByTagList) {
          groupByEntityList.add(getGroupByEntityFromTag(groupByTag));
        }
      }
    }
    return groupByEntityList;
  }

  private List<QLDataPoint> getTagDataPoints(String accountId, List<QLDataPoint> dataPoints, TA groupByTag) {
    Set<String> entityIdSet =
        dataPoints.stream().map(dataPoint -> dataPoint.getKey().getId()).collect(Collectors.toSet());
    Set<HarnessTagLink> tagLinks = tagService.getTagLinks(
        accountId, getEntityType((E) groupByTag.getEntityType()), entityIdSet, groupByTag.getTagName());
    Map<String, HarnessTagLink> entityIdTagLinkMap =
        tagLinks.stream().collect(Collectors.toMap(HarnessTagLink::getEntityId, identity()));

    ArrayMap<String, QLDataPoint> tagNameDataPointMap = new ArrayMap<>();

    dataPoints.removeIf(dataPoint -> {
      String entityId = dataPoint.getKey().getId();
      HarnessTagLink tagLink = entityIdTagLinkMap.get(entityId);
      if (tagLink == null) {
        return true;
      }

      String tagName = tagLink.getKey() + ":" + tagLink.getValue();
      QLDataPoint existingDataPoint = tagNameDataPointMap.get(tagName);
      if (existingDataPoint != null) {
        existingDataPoint.setValue(existingDataPoint.getValue().doubleValue() + dataPoint.getValue().doubleValue());
        return true;
      }

      QLReference tagRef = QLReference.builder().id(tagName).name(tagName).type(EntityType.TAG.name()).build();
      dataPoint.setKey(tagRef);
      tagNameDataPointMap.put(tagName, dataPoint);
      return false;
    });
    return new ArrayList<>(tagNameDataPointMap.values());
  }

  private List<QLStackedDataPoint> getTagStackedDataPoints(
      String accountId, List<QLStackedDataPoint> stackedDataPoints, TA groupByTag) {
    Set<String> entityIdSet = stackedDataPoints.stream()
                                  .map(stackedDataPoint -> stackedDataPoint.getKey().getId())
                                  .collect(Collectors.toSet());
    Set<HarnessTagLink> tagLinks = tagService.getTagLinks(
        accountId, getEntityType((E) groupByTag.getEntityType()), entityIdSet, groupByTag.getTagName());
    Map<String, HarnessTagLink> entityIdTagLinkMap =
        tagLinks.stream().collect(Collectors.toMap(HarnessTagLink::getEntityId, identity()));

    ArrayMap<String, QLStackedDataPoint> tagNameStackedDataPointMap = new ArrayMap<>();

    stackedDataPoints.removeIf(stackedDataPoint -> {
      String entityId = stackedDataPoint.getKey().getId();
      HarnessTagLink tagLink = entityIdTagLinkMap.get(entityId);
      if (tagLink == null) {
        return true;
      }

      String tagName = tagLink.getKey() + ":" + tagLink.getValue();
      QLStackedDataPoint existingDataPoint = tagNameStackedDataPointMap.get(tagName);
      if (existingDataPoint != null) {
        List<QLDataPoint> dataPoints = mergeLists(existingDataPoint.getValues(), stackedDataPoint.getValues());
        existingDataPoint.setValues(dataPoints);
        return true;
      }

      QLReference tagRef = QLReference.builder().id(tagName).name(tagName).type(EntityType.TAG.name()).build();
      stackedDataPoint.setKey(tagRef);
      tagNameStackedDataPointMap.put(tagName, stackedDataPoint);
      return false;
    });
    return new ArrayList<>(tagNameStackedDataPointMap.values());
  }

  protected abstract EA getGroupByEntityFromTag(TA groupByTag);
}
