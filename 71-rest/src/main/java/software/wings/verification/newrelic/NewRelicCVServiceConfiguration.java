package software.wings.verification.newrelic;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.verification.CVConfiguration;

import java.util.List;
import javax.validation.constraints.NotNull;

/**
 * @author Vaibhav Tulsyan
 * 05/Oct/2018
 */

@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class NewRelicCVServiceConfiguration extends CVConfiguration {
  @Transient
  @SchemaIgnore
  private static final Logger logger = LoggerFactory.getLogger(NewRelicCVServiceConfiguration.class);

  @Attributes(required = true, title = "New Relic Server") private String newRelicServerSettingId;
  @Attributes(required = true, title = "Application Name") private String applicationId;
  @Attributes(required = true, title = "Metrics") private List<String> metrics;

  @Builder
  public NewRelicCVServiceConfiguration(@NotNull String appId, @NotNull String envId, @NotNull String serviceId,
      boolean enabled24x7, String newRelicServerSettingId, String applicationId, List<String> metrics) {
    super(appId, envId, serviceId, enabled24x7);
    this.newRelicServerSettingId = newRelicServerSettingId;
    this.applicationId = applicationId;
    this.metrics = metrics;
  }
}
