package software.wings.security.authentication.oauth;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.TrialSignupOptions;
import software.wings.beans.utm.UtmInfo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
  List<TrialSignupOptions.Products> freemiumProducts;
  Boolean freemiumAssistedOption;
}
