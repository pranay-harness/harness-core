package io.harness.delegate.beans.ci.pod;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Data
@Builder
@AllArgsConstructor
public class HostAliasParams {
  @NonNull private String ipAddress;
  @NonNull private List<String> hostnameList;
}
