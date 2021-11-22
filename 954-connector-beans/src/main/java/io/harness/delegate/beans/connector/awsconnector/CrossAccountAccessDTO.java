package io.harness.delegate.beans.connector.awsconnector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApiModel("CrossAccountAccess")
@Schema(name = "CrossAccountAccess", description = "This contains AWS connector cross account access details")
public class CrossAccountAccessDTO {
  @NotNull private String crossAccountRoleArn;
  private String externalId;

  @Builder
  public CrossAccountAccessDTO(String crossAccountRoleArn, String externalId) {
    this.crossAccountRoleArn = crossAccountRoleArn;
    this.externalId = externalId;
  }
}
