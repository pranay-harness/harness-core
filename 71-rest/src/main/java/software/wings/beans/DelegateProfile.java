package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;

/**
 * Created by rishi on 7/31/18
 */
@Entity(value = "delegateProfiles")
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class DelegateProfile extends Base {
  @NotEmpty private String accountId;
  @NotEmpty private String name;
  private String description;
  private String startupScript;
}
