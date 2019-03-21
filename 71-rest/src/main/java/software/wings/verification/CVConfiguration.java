package software.wings.verification;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessExportableEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.Base;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.AnalysisTolerance;
import software.wings.sm.StateType;
import software.wings.yaml.BaseEntityYaml;

import java.util.Date;
import javax.validation.constraints.NotNull;

/**
 * @author Vaibhav Tulsyan
 * 05/Oct/2018
 */

@Entity(value = "verificationServiceConfigurations")
@Indexes({
  @Index(fields = {
    @Field("appId"), @Field("envId"), @Field("name")
  }, options = @IndexOptions(unique = true, name = "nameUniqueIndex"))
})
@HarnessExportableEntity
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class CVConfiguration extends Base {
  public static final String NAME_KEY = "name";
  public static final String SERVICE_ID_KEY = "serviceId";

  @NotNull private String name;
  @NotNull @Indexed private String accountId;
  @NotNull private String connectorId;
  @NotNull @Indexed private String envId;
  @NotNull @Indexed private String serviceId;
  @NotNull private StateType stateType;
  @NotNull private AnalysisTolerance analysisTolerance;
  private boolean enabled24x7;
  private AnalysisComparisonStrategy comparisonStrategy;
  private String contextId;
  private boolean isWorkflowConfig;

  @Transient @SchemaIgnore private String connectorName;
  @Transient @SchemaIgnore private String serviceName;
  @Transient @SchemaIgnore private String envName;
  @Transient @SchemaIgnore private String appName;

  @JsonIgnore @SchemaIgnore @Indexed(options = @IndexOptions(expireAfterSeconds = 0)) private Date validUntil;

  public AnalysisComparisonStrategy getComparisonStrategy() {
    return comparisonStrategy == null ? AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS : comparisonStrategy;
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public abstract static class CVConfigurationYaml extends BaseEntityYaml {
    private String name;
    private String accountId;
    private String connectorName;
    private String envName;
    private String serviceName;
    private String harnessApplicationName;
    private AnalysisTolerance analysisTolerance;
    private boolean enabled24x7;

    public CVConfigurationYaml(String type, String harnessApiVersion, String name, String accountId, String connectorId,
        String envId, String serviceId, String analysisTolerance) {}
  }
}
