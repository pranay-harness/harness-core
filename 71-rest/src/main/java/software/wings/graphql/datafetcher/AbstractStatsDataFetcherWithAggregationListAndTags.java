package software.wings.graphql.datafetcher;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static java.util.function.Function.identity;

import com.google.api.client.util.ArrayMap;
import com.google.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import software.wings.beans.EntityType;
import software.wings.beans.HarnessTagLink;
import software.wings.graphql.schema.type.aggregation.QLBillingDataPoint;
import software.wings.graphql.schema.type.aggregation.QLBillingStackedTimeSeriesData;
import software.wings.graphql.schema.type.aggregation.QLBillingStackedTimeSeriesDataPoint;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.QLReference;
import software.wings.graphql.schema.type.aggregation.TagAggregation;
import software.wings.graphql.schema.type.aggregation.billing.QLEntityTableData;
import software.wings.graphql.schema.type.aggregation.billing.QLEntityTableListData;
import software.wings.service.intfc.HarnessTagService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public abstract class AbstractStatsDataFetcherWithAggregationListAndTags<A, F, G, S, E, TA extends TagAggregation, EA>
    extends AbstractStatsDataFetcherWithAggregationList<A, F, G, S> {
  @Inject protected HarnessTagService tagService;
  protected abstract TA getTagAggregation(G groupBy);
  protected abstract EA getEntityAggregation(G groupBy);
  protected abstract EntityType getEntityType(E entityType);

  @Override
  public QLData postFetch(String accountId, List<G> groupByList, QLData qlData) {
    List<TA> groupByTagList = getGroupByTag(groupByList);
    if (groupByTagList.isEmpty()) {
      return qlData;
    }
    TA groupByTagLevel1 = groupByTagList.get(0);

    if (qlData instanceof QLBillingStackedTimeSeriesData) {
      QLBillingStackedTimeSeriesData billingStackedTimeSeriesData = (QLBillingStackedTimeSeriesData) qlData;
      List<QLBillingStackedTimeSeriesDataPoint> billingStackedTimeSeriesDataPoints =
          billingStackedTimeSeriesData.getData();
      if (isEmpty(billingStackedTimeSeriesDataPoints)) {
        return qlData;
      }
      billingStackedTimeSeriesDataPoints.forEach(billingStackedTimeSeriesDataPoint -> {
        List<QLBillingDataPoint> dataPoints =
            getTagDataPoints(accountId, billingStackedTimeSeriesDataPoint.getValues(), groupByTagLevel1);
        billingStackedTimeSeriesDataPoint.setValues(dataPoints);
      });
    } else if (qlData instanceof QLEntityTableListData) {
      QLEntityTableListData entityTableListData = (QLEntityTableListData) qlData;
      List<QLEntityTableData> entityTableDataPoints = entityTableListData.getData();
      if (isEmpty(entityTableDataPoints)) {
        return qlData;
      }
      getTagEntityTableDataPoints(accountId, entityTableDataPoints, groupByTagLevel1);
    }
    return qlData;
  }

  protected int findFirstGroupByTagPosition(List<G> groupByList) {
    int index = -1;
    for (G groupByEntry : groupByList) {
      index++;
      TA tagAggregation = getTagAggregation(groupByEntry);
      if (tagAggregation != null) {
        return index;
      }
    }
    return -1;
  }

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

  protected int findFirstGroupByEntityPosition(List<G> groupByList) {
    int index = -1;
    for (G groupByEntry : groupByList) {
      index++;
      EA entityAggregation = getEntityAggregation(groupByEntry);
      if (entityAggregation != null) {
        return index;
      }
    }
    return -1;
  }

  protected List<EA> getGroupByEntityListFromTags(
      List<G> groupByList, List<EA> groupByEntityList, List<TA> groupByTagList) {
    if (!groupByTagList.isEmpty()) {
      if (groupByEntityList == null) {
        groupByEntityList = new ArrayList<>();
      }
      // We need to determine the order so that the correct grouping happens at each level.
      if (groupByEntityList.size() == 1 && groupByTagList.size() == 1) {
        int entityIndex = findFirstGroupByEntityPosition(groupByList);
        int tagIndex = findFirstGroupByTagPosition(groupByList);
        if (entityIndex > tagIndex) {
          groupByEntityList.add(0, getGroupByEntityFromTag(groupByTagList.get(0)));
        } else {
          groupByEntityList.add(getGroupByEntityFromTag(groupByTagList.get(0)));
        }
      } else {
        for (TA groupByTagEntry : groupByTagList) {
          groupByEntityList.add(getGroupByEntityFromTag(groupByTagEntry));
        }
      }
    }
    return groupByEntityList;
  }

  private List<QLBillingDataPoint> getTagDataPoints(
      String accountId, List<QLBillingDataPoint> dataPoints, TA groupByTag) {
    Set<String> entityIdSet =
        dataPoints.stream().map(dataPoint -> dataPoint.getKey().getId()).collect(Collectors.toSet());
    Set<HarnessTagLink> tagLinks = tagService.getTagLinks(
        accountId, getEntityType((E) groupByTag.getEntityType()), entityIdSet, groupByTag.getTagName());
    Map<String, HarnessTagLink> entityIdTagLinkMap =
        tagLinks.stream().collect(Collectors.toMap(HarnessTagLink::getEntityId, identity()));

    ArrayMap<String, QLBillingDataPoint> tagNameDataPointMap = new ArrayMap<>();

    dataPoints.removeIf(dataPoint -> {
      String entityId = dataPoint.getKey().getId();
      HarnessTagLink tagLink = entityIdTagLinkMap.get(entityId);
      if (tagLink == null) {
        return true;
      }

      String tagName = tagLink.getKey() + ":" + tagLink.getValue();
      QLBillingDataPoint existingDataPoint = tagNameDataPointMap.get(tagName);
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

  private void getTagEntityTableDataPoints(String accountId, List<QLEntityTableData> dataPoints, TA groupByTag) {
    Set<String> entityIdSet = dataPoints.stream().map(dataPoint -> dataPoint.getId()).collect(Collectors.toSet());
    Set<HarnessTagLink> tagLinks = tagService.getTagLinks(
        accountId, getEntityType((E) groupByTag.getEntityType()), entityIdSet, groupByTag.getTagName());
    Map<String, HarnessTagLink> entityIdTagLinkMap =
        tagLinks.stream().collect(Collectors.toMap(HarnessTagLink::getEntityId, identity()));

    ArrayMap<String, QLEntityTableData> tagNameDataPointMap = new ArrayMap<>();

    dataPoints.removeIf(dataPoint -> {
      String entityId = dataPoint.getId();
      HarnessTagLink tagLink = entityIdTagLinkMap.get(entityId);
      if (tagLink == null) {
        return true;
      }

      String tagName = tagLink.getKey() + ":" + tagLink.getValue();
      QLEntityTableData existingDataPoint = tagNameDataPointMap.get(tagName);
      if (existingDataPoint != null) {
        existingDataPoint.setTotalCost(
            existingDataPoint.getTotalCost().doubleValue() + dataPoint.getTotalCost().doubleValue());
        existingDataPoint.setIdleCost(
            existingDataPoint.getIdleCost().doubleValue() + dataPoint.getIdleCost().doubleValue());
        existingDataPoint.setCpuIdleCost(
            existingDataPoint.getCpuIdleCost().doubleValue() + dataPoint.getCpuIdleCost().doubleValue());
        existingDataPoint.setMemoryIdleCost(
            existingDataPoint.getMemoryIdleCost().doubleValue() + dataPoint.getMemoryIdleCost().doubleValue());
        return true;
      }

      dataPoint.setId(tagName);
      dataPoint.setName(tagName);
      dataPoint.setType(EntityType.TAG.name());
      tagNameDataPointMap.put(tagName, dataPoint);
      return false;
    });
  }

  protected abstract EA getGroupByEntityFromTag(TA groupByTag);
}
