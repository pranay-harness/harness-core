package software.wings.beans.container;

import com.github.reinert.jjschema.Attributes;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.beans.NameValuePair;
import software.wings.yaml.BaseYaml;

import java.util.List;

@Data
@Builder
public class LogConfiguration {
  @Attributes(title = "Log Driver") private String logDriver;
  @Attributes(title = "Options") private List<LogOption> options;

  public static class LogOption {
    private String key;
    private String value;

    public String getKey() {
      return key;
    }

    public void setKey(String key) {
      this.key = key;
    }

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static final class Yaml extends BaseYaml {
    private String logDriver;
    private List<NameValuePair.Yaml> options;

    @Builder
    public Yaml(String logDriver, List<NameValuePair.Yaml> options) {
      this.logDriver = logDriver;
      this.options = options;
    }
  }
}