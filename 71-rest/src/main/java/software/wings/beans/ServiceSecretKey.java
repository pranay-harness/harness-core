package software.wings.beans;

import io.harness.annotation.HarnessExportableEntity;
import io.harness.annotation.NaturalKey;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;

/**
 * Created by rsingh on 1/10/18.
 */
@Indexes(
    { @Index(fields = { @Field("serviceType") }, options = @IndexOptions(unique = true, name = "serviceSecretIndex")) })
@Entity(value = "serviceSecrets", noClassnameStored = true)
@HarnessExportableEntity
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class ServiceSecretKey extends Base {
  @NaturalKey private String serviceSecret;
  @NaturalKey private ServiceType serviceType;

  public enum ServiceType { LEARNING_ENGINE }

  // add version in the end
  public enum ServiceApiVersion { V1 }
}
