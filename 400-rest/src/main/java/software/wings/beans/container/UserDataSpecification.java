package software.wings.beans.container;

import io.harness.annotation.HarnessEntity;
import io.harness.mongo.index.FdUniqueIndex;

import software.wings.beans.DeploymentSpecification;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;

/**
 * Created by anubhaw on 12/18/17.
 */
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@FieldNameConstants(innerTypeName = "UserDataSpecificationKeys")
@Entity("userDataSpecifications")
@HarnessEntity(exportable = true)
public class UserDataSpecification extends DeploymentSpecification {
  @NotEmpty @FdUniqueIndex private String serviceId;
  @NotNull private String data;
}
