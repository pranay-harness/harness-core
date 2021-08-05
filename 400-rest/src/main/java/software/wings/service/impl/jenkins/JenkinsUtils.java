package software.wings.service.impl.jenkins;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.JenkinsConfig;
import software.wings.helpers.ext.jenkins.Jenkins;
import software.wings.helpers.ext.jenkins.JenkinsFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@OwnedBy(CDC)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
@Singleton
public class JenkinsUtils {
  public static final String TOKEN_FIELD = "Bearer Token(HTTP Header)";

  @Inject private JenkinsFactory jenkinsFactory;

  public Jenkins getJenkins(JenkinsConfig jenkinsConfig) {
    if (TOKEN_FIELD.equals(jenkinsConfig.getAuthMechanism())) {
      return jenkinsFactory.create(jenkinsConfig.getJenkinsUrl(), jenkinsConfig.getToken());
    } else {
      return jenkinsFactory.create(
          jenkinsConfig.getJenkinsUrl(), jenkinsConfig.getUsername(), jenkinsConfig.getPassword());
    }
  }
}
