/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ng.core.dto;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.validator.constraints.NotBlank;

@OwnedBy(PL)
@JsonTypeName("smtp")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"password"})
@EqualsAndHashCode
@Schema(name = "SmtpConfig", description = "This has the SMTP configuration details defined in Harness.")
public class SmtpConfigDTO {
  @JsonProperty(value = "host", required = true) @NotNull @NotBlank private String host;
  @JsonProperty(value = "port", required = true) @NotNull private int port;
  @JsonProperty("fromAddress") private String fromAddress;
  @JsonProperty("useSSL") private boolean useSSL;
  @JsonProperty("startTLS") private boolean startTLS;
  @JsonProperty("username") private String username;
  @JsonProperty("password") private char[] password;
}
