package software.wings.service.impl.instance;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.mongodb.morphia.annotations.Id;
import software.wings.beans.instance.dashboard.EntitySummary;

import java.util.List;

@Data
@NoArgsConstructor
public final class ServiceInstanceCount {
  @Id private String serviceId;
  private long count;
  private List<EnvType> envTypeList;
  private EntitySummary appInfo;
  private EntitySummary serviceInfo;

  @Data
  @NoArgsConstructor
  public static final class EnvType {
    private String type;
  }
}
