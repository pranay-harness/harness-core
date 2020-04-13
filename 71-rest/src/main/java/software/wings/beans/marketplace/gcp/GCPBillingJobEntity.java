package software.wings.beans.marketplace.gcp;

import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessEntity;
import io.harness.iterator.PersistentRegularIterable;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAccess;
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

@FieldNameConstants(innerTypeName = "GCPBillingJobEntityKeys")
@Getter
@ToString
@EqualsAndHashCode
@Entity(value = "gcpBillingJobEntity")
@HarnessEntity(exportable = true)
public class GCPBillingJobEntity implements PersistentRegularIterable, CreatedAtAccess, UpdatedAtAccess, AccountAccess {
  @Id private String uuid;
  @Indexed private String accountId;
  private String gcpAccountId;
  @Setter @Indexed private Long nextIteration;

  @JsonView(JsonViews.Internal.class) @SchemaIgnore @NotNull private long createdAt;
  @JsonView(JsonViews.Internal.class) @SchemaIgnore @NotNull private long lastUpdatedAt;

  public GCPBillingJobEntity(String accountId, String gcpAccountId, Long nextIteration) {
    long currentMillis = Instant.now().toEpochMilli();
    this.uuid = accountId;
    this.accountId = accountId;
    this.gcpAccountId = gcpAccountId;
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
