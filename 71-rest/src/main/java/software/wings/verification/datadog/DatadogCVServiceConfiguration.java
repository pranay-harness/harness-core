package software.wings.verification.datadog;

import com.github.reinert.jjschema.Attributes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.verification.CVConfiguration;

/**
 * @author Vaibhav Tulsyan
 * 16/Oct/2018
 */

@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DatadogCVServiceConfiguration extends CVConfiguration {
  @Attributes(required = false, title = "Datadog Service Name") private String datadogServiceName;
  @Attributes(required = true, title = "Metrics") private String metrics;
}
