package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@Entity(value = "featureFlag", noClassnameStored = true)
@Indexes(@Index(fields = { @Field("name") }, options = @IndexOptions(name = "featureFlagIdx", unique = true)))
@JsonIgnoreProperties({"obsolete", "accountIds"})
public class FeatureFlag extends Base {
  private String name;
  private boolean enabled;
  private boolean obsolete;
  private Set<String> accountIds;
}
