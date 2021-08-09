package software.wings.service.impl.bugsnag;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * Created by Praveen
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public class BugsnagApplication implements Comparable<BugsnagApplication> {
  private String name;
  private String id;

  @Override
  public int compareTo(BugsnagApplication o) {
    return name.compareTo(o.name);
  }

  @Data
  @Builder
  public static class BugsnagApplications {
    private List<BugsnagApplication> applications;
  }
}
