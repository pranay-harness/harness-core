package software.wings.delegatetasks.validation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.Base;

import java.time.OffsetDateTime;
import java.util.Date;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@Entity(value = "delegateConnectionResults", noClassnameStored = true)
@Indexes({
  @Index(fields = {
    @Field("accountId"), @Field("delegateId"), @Field("criteria")
  }, options = @IndexOptions(unique = true, name = "delegateConnectionResultsIdx"))
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class DelegateConnectionResult extends Base {
  @Indexed @NotEmpty private String accountId;
  @Indexed @NotEmpty private String delegateId;
  @Indexed @NotEmpty private String criteria;
  private boolean validated;
  private long duration;

  @SchemaIgnore
  @JsonIgnore
  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
  @Default
  private Date validUntil = Date.from(OffsetDateTime.now().plusHours(6).toInstant());
}
