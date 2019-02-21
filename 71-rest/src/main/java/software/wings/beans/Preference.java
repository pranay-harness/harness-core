package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.annotation.HarnessExportableEntity;
import io.harness.annotation.NaturalKey;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;

@Data
@AllArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "preferenceType")
@JsonSubTypes({ @Type(value = DeploymentPreference.class, name = "DEPLOYMENT_PREFERENCE") })
@Entity(value = "preferences")
@HarnessExportableEntity
@EqualsAndHashCode(callSuper = false)
public abstract class Preference extends Base {
  @NotEmpty @NaturalKey private String name;
  @NotEmpty @NaturalKey private String accountId;
  @NotEmpty @NaturalKey private String userId;
  private String preferenceType;

  public Preference(String preferenceType) {
    this.preferenceType = preferenceType;
  }
}
