package software.wings.beans;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import software.wings.beans.Environment.EnvironmentType;

import java.util.List;

/**
 * Created by brett on 7/20/17
 */
@Entity(value = "delegateScopes")
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class DelegateScope extends Base {
  @NotEmpty private String accountId;
  private String name;
  private List<TaskGroup> taskTypes;
  private List<EnvironmentType> environmentTypes;
  private List<String> applications;
  private List<String> environments;
  private List<String> serviceInfrastructures;
  private List<String> tags;

  public boolean isValid() {
    return (isNotEmpty(taskTypes)) || (isNotEmpty(environmentTypes)) || (isNotEmpty(applications))
        || (isNotEmpty(environments)) || (isNotEmpty(serviceInfrastructures) || (isNotEmpty(tags)));
  }
}
