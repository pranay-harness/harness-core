package io.harness.notification;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.*;

@JsonTypeName("stmp")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"password"})
@EqualsAndHashCode
public class SmtpConfig {
  @JsonProperty("type") private String type;
  @JsonProperty("host") private String host;
  @JsonProperty("port") private int port;
  @JsonProperty("fromAddress") private String fromAddress;
  @JsonProperty("useSSL") private boolean useSSL;
  @JsonProperty("username") private String username;
  @JsonProperty("password") private char[] password;
}
