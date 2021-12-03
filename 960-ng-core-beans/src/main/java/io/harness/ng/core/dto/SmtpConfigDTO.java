package io.harness.ng.core.dto;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@OwnedBy(PL)
@JsonTypeName("smtp")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"password"})
@EqualsAndHashCode
@Schema(name = "SmtpConfig", description = "This is the view of SmtpConfig entity defined in Harness")
public class SmtpConfigDTO {
  @JsonProperty("host") private String host;
  @JsonProperty("port") private int port;
  @JsonProperty("fromAddress") private String fromAddress;
  @JsonProperty("useSSL") private boolean useSSL;
  @JsonProperty("startTLS") private boolean startTLS;
  @JsonProperty("username") private String username;
  @JsonProperty("password") private char[] password;
}
