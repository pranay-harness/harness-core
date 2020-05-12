package software.wings.helpers.ext.jenkins;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.inject.assistedinject.Assisted;

import io.harness.annotations.dev.OwnedBy;

/**
 * Created by peeyushaggarwal on 5/12/16.
 */
@OwnedBy(CDC)
public interface JenkinsFactory {
  /**
   * Creates a new Jenkins object.
   *
   * @param url the url
   * @return the jenkins
   */
  Jenkins createWithoutCredentials(@Assisted("url") String url);

  /**
   * Creates the.
   *
   * @param url       the url
   * @param username  the username
   * @param password  the password
   * @return the jenkins
   */
  Jenkins create(
      @Assisted("url") String url, @Assisted("username") String username, @Assisted("password") char[] password);

  /**
   *
   * @param url
   * @param token
   * @return
   */
  Jenkins create(@Assisted("url") String url, @Assisted("token") char[] token);
}
