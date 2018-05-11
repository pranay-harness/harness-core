package software.wings.helpers.ext.jenkins;

import com.google.inject.assistedinject.Assisted;

/**
 * Created by peeyushaggarwal on 5/12/16.
 */
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
