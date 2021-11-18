package io.harness.delegate.beans.connector.scm.bitbucket;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModel;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
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
@ApiModel("BitbucketApiAccess")
@Schema(name = "BitbucketApiAccess",
    description = "This contains details of the information needed for Bitbucket API access")
public class BitbucketApiAccessDTO {
  @NotNull BitbucketApiAccessType type;
  @JsonProperty("spec")
  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
  @Valid
  @NotNull
  BitbucketApiAccessSpecDTO spec;

  @Builder
  public BitbucketApiAccessDTO(BitbucketApiAccessType type, BitbucketApiAccessSpecDTO spec) {
    this.type = type;
    this.spec = spec;
  }
}
