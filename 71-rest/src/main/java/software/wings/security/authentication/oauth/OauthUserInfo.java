package software.wings.security.authentication.oauth;

import static io.harness.annotations.dev.HarnessTeam.PL;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotations.dev.OwnedBy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.wings.beans.utm.UtmInfo;

@OwnedBy(PL)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OauthUserInfo {
  String email;
  String name;
  String login;
  UtmInfo utmInfo;
}
