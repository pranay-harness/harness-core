package software.wings.delegatetasks.validation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;

import java.time.OffsetDateTime;
import java.util.Date;
import javax.validation.constraints.NotNull;

@Data
@Builder
@Entity(value = "delegateConnectionResults", noClassnameStored = true)
@Indexes({
  @Index(fields = {
    @Field("accountId"), @Field("delegateId"), @Field("criteria")
  }, options = @IndexOptions(unique = true, name = "delegateConnectionResultsIdx"))
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class DelegateConnectionResult implements UuidAware, PersistentEntity, UpdatedAtAware {
  @Id private String uuid;

  @NotNull private long lastUpdatedAt;

  @Indexed @NotEmpty private String accountId;
  @Indexed @NotEmpty private String delegateId;
  @Indexed @NotEmpty private String criteria;
  private boolean validated;
  private long duration;

  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
  @Default
  private Date validUntil = Date.from(OffsetDateTime.now().plusDays(30).toInstant());
}
