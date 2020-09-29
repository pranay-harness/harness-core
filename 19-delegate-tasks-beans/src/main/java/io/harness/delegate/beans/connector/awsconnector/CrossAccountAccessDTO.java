package io.harness.delegate.beans.connector.awsconnector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApiModel("CrossAccountAccess")
public class CrossAccountAccessDTO {
  @NotNull private String crossAccountRoleArn;
  private String externalId;

  @Builder
  public CrossAccountAccessDTO(String crossAccountRoleArn, String externalId) {
    this.crossAccountRoleArn = crossAccountRoleArn;
    this.externalId = externalId;
  }
}
