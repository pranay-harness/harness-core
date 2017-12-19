package software.wings.beans.container;

import lombok.Builder;
import lombok.Data;
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
@Data
@Builder
public class UserDataSpecification extends DeploymentSpecification {
  @NotEmpty @Indexed(options = @IndexOptions(unique = true)) private String serviceId;

  @NotNull private String data;
}
