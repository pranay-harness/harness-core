package io.harness.pms.downloadlogs.beans.resource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import java.util.Date;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel("DownloadLogsResource")
public class DownloadLogsResponseDTO {
  String downloadLink;
  Date validUntil;
  String waitTime;
  // wait time is time needed for link to start working, given to user in a human readable format
}
