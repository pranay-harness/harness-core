package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import ro.fortsoft.pf4j.Extension;
import software.wings.annotation.EncryptableSetting;
import software.wings.settings.SettingValue;
import software.wings.settings.UsageRestrictions;
import software.wings.sm.StateType;
import software.wings.yaml.setting.VerificationProviderYaml;

/**
 * Created by rsingh on 3/15/18.
 */
@Extension
@Data
@JsonTypeName("PROMETHEUS")
@EqualsAndHashCode(callSuper = false)
@Builder
public class PrometheusConfig extends SettingValue implements EncryptableSetting {
  @SchemaIgnore @NotEmpty private String accountId;

  @Attributes(title = "URL", required = true) private String url;

  public PrometheusConfig() {
    super(StateType.PROMETHEUS.name());
  }

  public PrometheusConfig(String url, String accountId) {
    this();
    this.url = url;
    this.accountId = accountId;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static final class PrometheusYaml extends VerificationProviderYaml {
    private String prometheusUrl;

    @Builder
    public PrometheusYaml(
        String type, String harnessApiVersion, String prometheusUrl, UsageRestrictions usageRestrictions) {
      super(type, harnessApiVersion, usageRestrictions);
      this.prometheusUrl = prometheusUrl;
    }
  }
}
