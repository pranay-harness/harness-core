package software.wings.verification;

import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.Base;
import software.wings.service.impl.analysis.AnalysisTolerance;
import software.wings.sm.StateType;

import javax.validation.constraints.NotNull;

/**
 * @author Vaibhav Tulsyan
 * 05/Oct/2018
 */

@Entity(value = "verificationServiceConfigurations")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class CVConfiguration extends Base {
  @NotNull private String name;
  @NotNull private String accountId;
  @NotNull private String connectorId;
  @NotNull private String envId;
  @NotNull private String serviceId;
  @NotNull private StateType stateType;
  @NotNull private AnalysisTolerance analysisTolerance;
  private boolean enabled24x7;

  @Transient @SchemaIgnore private String connectorName;
  @Transient @SchemaIgnore private String serviceName;
  @Transient @SchemaIgnore private String appName;
}
