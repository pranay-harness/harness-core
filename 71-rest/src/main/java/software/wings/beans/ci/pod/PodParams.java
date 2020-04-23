package software.wings.beans.ci.pod;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public abstract class PodParams<T extends ContainerParams> {
  @NonNull private String name;
  @NonNull private String namespace;
  private Map<String, String> labels;
  private List<T> containerParamsList;

  public abstract PodParams.Type getType();

  public enum Type { K8 }
}