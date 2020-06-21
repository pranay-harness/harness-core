package software.wings.service.impl.analysis;

import io.harness.beans.EmbeddedUser;
import io.harness.mongo.index.Indexed;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.CreatedByAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UpdatedByAware;
import io.harness.persistence.UuidAware;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Id;

import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@org.mongodb.morphia.annotations.Entity(value = "timeSeriesKeyTransactions", noClassnameStored = true)
@FieldNameConstants(innerTypeName = "TimeSeriesKeyTransactionsKeys")
public class TimeSeriesKeyTransactions implements PersistentEntity, UuidAware, CreatedAtAware, CreatedByAware,
                                                  UpdatedAtAware, UpdatedByAware, AccountAccess {
  @Id private String uuid;
  private long createdAt;
  private long lastUpdatedAt;
  private EmbeddedUser createdBy;
  private EmbeddedUser lastUpdatedBy;

  @Indexed private String cvConfigId;
  @Indexed private String serviceId;
  @Indexed private String accountId;
  private Set<String> keyTransactions;
}
