package software.wings.beans.container;

import io.harness.annotation.HarnessExportableEntity;
import io.harness.annotation.NaturalKey;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import software.wings.beans.DeploymentSpecification;

import javax.validation.constraints.NotNull;

/**
 * Created by anubhaw on 12/18/17.
 */
@Entity("userDataSpecifications")
@HarnessExportableEntity
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class UserDataSpecification extends DeploymentSpecification {
  @NotEmpty @Indexed(options = @IndexOptions(unique = true)) @NaturalKey private String serviceId;
  @NotNull private String data;

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends DeploymentSpecification.Yaml {
    private String data;

    @Builder
    public Yaml(String type, String harnessApiVersion, String data) {
      super(type, harnessApiVersion);
      this.data = data;
    }
  }
}
