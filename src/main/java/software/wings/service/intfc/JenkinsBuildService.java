package software.wings.service.intfc;

import software.wings.beans.JenkinsConfig;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.io.IOException;
import java.util.List;
import javax.ws.rs.core.MultivaluedMap;

// TODO: Auto-generated Javadoc

/**
 * Created by peeyushaggarwal on 5/13/16.
 */
public interface JenkinsBuildService {
  /**
   * Gets the builds.
   *
   * @param queryParameters the query parameters
   * @param jenkinsConfig   the jenkins config
   * @return the builds
   * @throws IOException Signals that an I/O exception has occurred.
   */
  List<BuildDetails> getBuilds(MultivaluedMap<String, String> queryParameters, JenkinsConfig jenkinsConfig)
      throws IOException;
}
