package software.wings.beans;

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
  private List<TaskType> taskTypes;
  private List<EnvironmentType> environmentTypes;
  private List<String> applications;
  private List<String> environments;
  private List<String> serviceInfrastructures;

  public boolean isEmpty() {
    return (taskTypes == null || taskTypes.isEmpty()) && (environmentTypes == null || environmentTypes.isEmpty())
        && (applications == null || applications.isEmpty()) && (environments == null || environments.isEmpty())
        && (serviceInfrastructures == null || serviceInfrastructures.isEmpty());
  }
}
