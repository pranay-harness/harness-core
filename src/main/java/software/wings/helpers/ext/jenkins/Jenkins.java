package software.wings.helpers.ext.jenkins;

import com.offbytwo.jenkins.model.JobWithDetails;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.List;

/**
 * Created by peeyushaggarwal on 5/12/16.
 */
public interface Jenkins {
  JobWithDetails getJob(String jobname) throws IOException;

  List<BuildDetails> getBuildsForJob(String jobname, int lastN) throws IOException;

  String trigger(String jobname);

  String checkStatus(String jobname);

  String checkStatus(String jobname, String buildNo);

  String checkArtifactStatus(String jobname, String artifactpathRegex);

  String checkArtifactStatus(String jobname, String buildNo, String artifactpathRegex);

  Pair<String, InputStream> downloadArtifact(String jobname, String artifactpathRegex)
      throws IOException, URISyntaxException;

  Pair<String, InputStream> downloadArtifact(String jobname, String buildNo, String artifactpathRegex)
      throws IOException, URISyntaxException;
}
