package software.wings.graphql.datafetcher.userGroup;

import lombok.Builder;
import lombok.Data;
import software.wings.beans.sso.SSOType;

@Data
@Builder
public class UserGroupSSOSettings {
  boolean isSSOLinked;
  String linkedSSOId;
  SSOType ssoType;
  String ssoGroupName;
  String linkedSsoDisplayName;
  String ssoGroupId;
}
