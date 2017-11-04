package software.wings.delegatetasks.validation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
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
}
