package io.harness.cvng.core.beans.params;

import javax.ws.rs.QueryParam;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceEnvironmentParams extends ProjectParams {
  @QueryParam("serviceIdentifier") @NonNull String serviceIdentifier;
  @QueryParam("environmentIdentifier") @NonNull String environmentIdentifier;

  public static ServiceEnvironmentParamsBuilder builderWithProjectParams(ProjectParams projectParams) {
    return ServiceEnvironmentParams.builder()
        .orgIdentifier(projectParams.getOrgIdentifier())
        .accountIdentifier(projectParams.getAccountIdentifier())
        .projectIdentifier(projectParams.getProjectIdentifier());
  }
}
