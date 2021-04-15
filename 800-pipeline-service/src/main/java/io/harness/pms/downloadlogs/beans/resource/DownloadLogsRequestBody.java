package io.harness.pms.downloadlogs.beans.resource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.ws.rs.DefaultValue;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@AllArgsConstructor
public class DownloadLogsRequestBody {
  @DefaultValue("6h") String timeToLive;
  @NonNull String logKey;
}
