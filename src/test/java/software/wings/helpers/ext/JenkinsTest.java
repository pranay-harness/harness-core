package software.wings.helpers.ext;

import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.model.Build;
import com.offbytwo.jenkins.model.BuildWithDetails;
import com.offbytwo.jenkins.model.JobWithDetails;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class JenkinsTest {
  @Test
  public void testJobExists() {
    Jenkins jenkins = new Jenkins("http://localhost:8080/jenkins"); //, "user1", "user1");
    System.out.println(jenkins.jobExists("test-freestyle"));
  }

  @Test
  public void testGetBuild() throws URISyntaxException, IOException {
    JenkinsServer jenkins = new JenkinsServer(new URI("http://localhost:8080/jenkins"));

    JobWithDetails jobDetails = jenkins.getJob("test-freestyle");
    Build build = jobDetails.getBuildByNumber(4);

    //		QueueItem q = new QueueItem();
    //		q.setId(5l);
    //		Build build = jenkins.getBuild(q);
    BuildWithDetails details = build.details();
    FileOutputStream fout = new FileOutputStream("/Users/rishi/abc.war");
    long l = IOUtils.copy(details.downloadArtifact(details.getArtifacts().get(0)), fout);
    fout.close();
    System.out.println(details);
  }
}
