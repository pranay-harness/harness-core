package software.wings.graphql.datafetcher.userGroup;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.sso.SSOType;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@TargetModule(Module._380_CG_GRAPHQL)
public class UserGroupSSOSettings {
  boolean isSSOLinked;
  String linkedSSOId;
  SSOType ssoType;
  String ssoGroupName;
  String linkedSsoDisplayName;
  String ssoGroupId;
}
