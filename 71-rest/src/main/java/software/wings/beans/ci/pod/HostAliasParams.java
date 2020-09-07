package software.wings.beans.ci.pod;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class HostAliasParams {
  @NonNull private String ipAddress;
  @NonNull private List<String> hostnameList;
}
