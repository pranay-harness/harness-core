package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotation.HarnessEntity;
import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;

@OwnedBy(CDC)
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@FieldNameConstants(innerTypeName = "ServiceSecretKeyKeys")
@Entity(value = "serviceSecrets", noClassnameStored = true)
@HarnessEntity(exportable = true)
public class ServiceSecretKey extends Base {
  private String serviceSecret;
  @Indexed(options = @IndexOptions(unique = true)) private ServiceType serviceType;

  public enum ServiceType { LEARNING_ENGINE, MANAGER_TO_COMMAND_LIBRARY_SERVICE }

  // add version in the end
  public enum ServiceApiVersion { V1 }
}
