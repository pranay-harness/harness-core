package software.wings.beans;

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
public class FeatureFlag extends Base {
  private String name;
  private boolean enabled;
  private boolean obsolete;
  private Set<String> accountIds;

  public enum FeatureName { GIT_SYNC, ECS_CREATE_CLUSTER, KUBERNETES_CREATE_CLUSTER, TIME_SERIES_ML, KMS }
}
