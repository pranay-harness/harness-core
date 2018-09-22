package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;

@Data
@AllArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "preferenceType")
@JsonSubTypes({ @Type(value = DeploymentPreference.class, name = "DEPLOYMENT_PREFERENCE") })
@Entity(value = "preferences")
public abstract class Preference extends Base {
  @NotEmpty private String name;
  @NotEmpty private String accountId;
  @NotEmpty private String userId;
  private String preferenceType;

  public Preference(String preferenceType) {
    this.preferenceType = preferenceType;
  }
}
