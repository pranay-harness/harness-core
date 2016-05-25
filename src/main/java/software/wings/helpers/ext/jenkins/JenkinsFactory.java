package software.wings.helpers.ext.jenkins;

import com.google.inject.assistedinject.Assisted;

/**
 * Created by peeyushaggarwal on 5/12/16.
 */
public interface JenkinsFactory {
  Jenkins createWithoutCredentials(@Assisted("url") String url);

  Jenkins create(
      @Assisted("url") String url, @Assisted("username") String username, @Assisted("password") String password);
}
