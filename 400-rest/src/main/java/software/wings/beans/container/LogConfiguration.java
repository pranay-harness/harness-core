package software.wings.beans.container;

import com.github.reinert.jjschema.Attributes;
import java.util.List;
import lombok.Builder;
import lombok.Data;

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
}
