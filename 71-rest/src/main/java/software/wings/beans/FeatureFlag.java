package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import lombok.Builder;
import lombok.Data;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.util.Set;

@Data
@Builder
@Entity(value = "featureFlag", noClassnameStored = true)
@JsonIgnoreProperties({"obsolete", "accountIds"})
public class FeatureFlag implements PersistentEntity, UpdatedAtAware {
  @Id private String name;
  private boolean enabled;
  private boolean obsolete;
  private Set<String> accountIds;

  private long lastUpdatedAt;
}
