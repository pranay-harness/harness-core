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

import java.util.List;

/**
 * Created by bsollish on 10/04/17
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@Entity(value = "featureFlag", noClassnameStored = true)
@Indexes(@Index(fields = { @Field("type") }, options = @IndexOptions(name = "featureFlagIdx", unique = true)))
public class FeatureFlag extends Base {
  private Type type;
  private boolean flag;
  private List<String> whiteListedAccountIds;

  public enum Type { GIT_SYNC }
}