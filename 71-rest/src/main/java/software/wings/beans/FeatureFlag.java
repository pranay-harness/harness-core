package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;
import lombok.Builder;
import lombok.Data;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.util.Set;

@Data
@Builder
@Entity(value = "featureFlag", noClassnameStored = true)
@JsonIgnoreProperties({"obsolete", "accountIds"})
public class FeatureFlag implements PersistentEntity, UuidAware, UpdatedAtAware {
  @Id private String uuid;

  private String name;
  public enum Scope {
    GLOBAL,
    PER_ACCOUNT,
  }

  private boolean enabled;
  private boolean obsolete;
  private Set<String> accountIds;

  private long lastUpdatedAt;
}
