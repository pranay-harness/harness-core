package software.wings.graphql.datafetcher.userGroup;

import software.wings.beans.sso.SSOType;

import lombok.Builder;
import lombok.Data;

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
