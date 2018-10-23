package software.wings.beans.infrastructure.instance.stats;

import com.google.common.collect.ImmutableList;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Value;
import lombok.experimental.NonFinal;
import org.apache.commons.collections4.CollectionUtils;
import org.mongodb.morphia.annotations.Entity;
import software.wings.beans.Base;
import software.wings.beans.EntityType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
@Entity(value = "instanceStats", noClassnameStored = true)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class InstanceStatsSnapshot extends Base {
  private static final List<EntityType> ENTITY_TYPES_TO_AGGREGATE_ON = Arrays.asList(EntityType.APPLICATION);

  @NonFinal private Instant timestamp;
  @NonFinal private String accountId;
  @NonFinal private List<AggregateCount> aggregateCounts = new ArrayList<>();
  @NonFinal private int total;

  public InstanceStatsSnapshot(Instant timestamp, String accountId, List<AggregateCount> aggregateCounts) {
    validate(aggregateCounts);

    this.timestamp = timestamp;
    this.accountId = accountId;
    this.aggregateCounts = aggregateCounts;

    if (CollectionUtils.isEmpty(aggregateCounts)) {
      this.total = 0;
    } else {
      EntityType firstEntity = aggregateCounts.get(0).entityType;
      this.total =
          aggregateCounts.stream().filter(ac -> ac.entityType == firstEntity).mapToInt(AggregateCount::getCount).sum();
    }
  }

  private void validate(List<AggregateCount> aggregateCounts) {
    for (AggregateCount aggregateCount : aggregateCounts) {
      if (!ENTITY_TYPES_TO_AGGREGATE_ON.contains(aggregateCount.entityType)) {
        throw new IllegalArgumentException(
            "Unexpected entity type. See ENTITY_TYPES_TO_AGGREGATE_ON for allowed types. Type: "
            + aggregateCount.entityType);
      }
    }
  }

  public List<AggregateCount> getAggregateCounts() {
    return ImmutableList.copyOf(aggregateCounts);
  }

  @Value
  @AllArgsConstructor
  public static class AggregateCount {
    private EntityType entityType;
    private String name;
    private String id;
    @NonFinal private int count;

    public void incrementCount(int diff) {
      this.count = count + diff;
    }
  }
}
