package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotation.HarnessExportableEntity;
import io.harness.annotation.NaturalKey;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;

/**
 * Created by rishi on 7/31/18
 */
@Entity(value = "delegateProfiles")
@HarnessExportableEntity
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class DelegateProfile extends Base {
  @NotEmpty @NaturalKey private String accountId;
  @NotEmpty @NaturalKey private String name;
  private String description;
  private String startupScript;
}
