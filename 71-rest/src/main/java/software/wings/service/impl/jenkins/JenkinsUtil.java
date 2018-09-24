package software.wings.service.impl.jenkins;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.JenkinsConfig;
import software.wings.common.Constants;
import software.wings.helpers.ext.jenkins.Jenkins;
import software.wings.helpers.ext.jenkins.JenkinsFactory;

@Singleton
public class JenkinsUtil {
  @Inject private JenkinsFactory jenkinsFactory;

  public Jenkins getJenkins(JenkinsConfig jenkinsConfig) {
    if (Constants.TOKEN_FIELD.equals(jenkinsConfig.getAuthMechanism())) {
      return jenkinsFactory.create(jenkinsConfig.getJenkinsUrl(), jenkinsConfig.getToken());
    } else {
      return jenkinsFactory.create(
          jenkinsConfig.getJenkinsUrl(), jenkinsConfig.getUsername(), jenkinsConfig.getPassword());
    }
  }
}
