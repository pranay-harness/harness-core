package io.harness.pms.downloadlogs.beans.resource;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Date;
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
  @DefaultValue("36h") String timeToLive;
  @NonNull String logKey;
  @JsonIgnore Date createdAt;
}
