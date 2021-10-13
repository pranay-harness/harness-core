package io.harness.connector;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.sdk.EntityGitDetails;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("ConnectorResponse")
@OwnedBy(DX)
@Schema(name = "ConnectorResponse", description = "Connetor Resource along with metadata")
public class ConnectorResponseDTO {
  ConnectorInfoDTO connector;
  Long createdAt;
  Long lastModifiedAt;
  ConnectorConnectivityDetails status;
  ConnectorActivityDetails activityDetails;
  boolean harnessManaged;
  EntityGitDetails gitDetails;
}
