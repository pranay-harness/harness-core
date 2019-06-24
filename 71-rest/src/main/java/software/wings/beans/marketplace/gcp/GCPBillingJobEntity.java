package software.wings.beans.marketplace.gcp;

import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessExportableEntity;
import io.harness.persistence.CreatedAtAccess;
import io.harness.persistence.PersistentIterable;
import io.harness.persistence.UpdatedAtAccess;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;
import software.wings.jersey.JsonViews;

import java.time.Instant;
import javax.validation.constraints.NotNull;

@Entity(value = "gcpBillingJobEntity")
@FieldNameConstants(innerTypeName = "GCPBillingJobEntityKeys")
@HarnessExportableEntity
@Getter
@ToString
@EqualsAndHashCode
public class GCPBillingJobEntity implements PersistentIterable, CreatedAtAccess, UpdatedAtAccess {
  @Id private String accountId;
  @Setter @Indexed private Long nextIteration;

  @JsonView(JsonViews.Internal.class) @SchemaIgnore @NotNull private long createdAt;
  @JsonView(JsonViews.Internal.class) @SchemaIgnore @NotNull private long lastUpdatedAt;

  public GCPBillingJobEntity(String accountId, Long nextIteration) {
    long currentMillis = Instant.now().toEpochMilli();
    this.accountId = accountId;
    this.nextIteration = nextIteration;
    this.createdAt = currentMillis;
    this.lastUpdatedAt = currentMillis;
  }

  @Override
  public Long obtainNextIteration(String fieldName) {
    return nextIteration;
  }

  @Override
  public void updateNextIteration(String fieldName, Long nextIteration) {
    this.nextIteration = nextIteration;
  }

  @Override
  public String getUuid() {
    return accountId;
  }
}
