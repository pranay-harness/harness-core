package software.wings.service.intfc;

import software.wings.beans.JenkinsConfig;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.io.IOException;
import java.util.List;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Created by peeyushaggarwal on 5/13/16.
 */
public interface JenkinsBuildService {
  List<BuildDetails> getBuilds(MultivaluedMap<String, String> queryParameters, JenkinsConfig jenkinsConfig)
      throws IOException;
}
