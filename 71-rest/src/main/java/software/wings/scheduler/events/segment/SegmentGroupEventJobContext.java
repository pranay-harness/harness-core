package software.wings.scheduler.events.segment;

import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessEntity;
import io.harness.data.structure.CollectionUtils;
import io.harness.iterator.PersistentRegularIterable;
import io.harness.mongo.index.Indexed;
import io.harness.persistence.CreatedAtAccess;
import io.harness.persistence.UpdatedAtAccess;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.NonFinal;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import software.wings.jersey.JsonViews;

import java.util.List;
import javax.validation.constraints.NotNull;

@Value
@Entity(value = "segmentGroupEventJobContexts")
@HarnessEntity(exportable = false)
@FieldNameConstants(innerTypeName = "SegmentGroupEventJobContextKeys")
public class SegmentGroupEventJobContext implements PersistentRegularIterable, CreatedAtAccess, UpdatedAtAccess {
  @Id private String uuid;

  private List<String> accountIds;
  @JsonView(JsonViews.Internal.class) @SchemaIgnore @NotNull private long createdAt;
  @JsonView(JsonViews.Internal.class) @SchemaIgnore @NotNull private long lastUpdatedAt;

  public SegmentGroupEventJobContext(Long nextIteration, List<String> accountIds) {
    this.uuid = null;
    this.accountIds = CollectionUtils.emptyIfNull(accountIds);
    this.createdAt = System.currentTimeMillis();
    this.lastUpdatedAt = System.currentTimeMillis();
    this.nextIteration = nextIteration;
  }

  @Indexed @NonFinal private Long nextIteration;

  @Override
  public void updateNextIteration(String fieldName, Long nextIteration) {
    this.nextIteration = nextIteration;
  }

  @Override
  public Long obtainNextIteration(String fieldName) {
    return nextIteration;
  }

  @Override
  public String getUuid() {
    return uuid;
  }

  @Override
  public long getCreatedAt() {
    return 0;
  }

  @Override
  public long getLastUpdatedAt() {
    return 0;
  }
}
