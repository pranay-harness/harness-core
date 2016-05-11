package software.wings.helpers.ext;

import static org.assertj.core.api.Assertions.assertThat;

import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.model.Build;
import com.offbytwo.jenkins.model.BuildWithDetails;
import com.offbytwo.jenkins.model.JobWithDetails;
import org.junit.Ignore;
import org.junit.Test;
import software.wings.utils.JsonUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class JenkinsTest {
  @Ignore
  @Test
  public void testJobExists() throws URISyntaxException, IOException {
    Jenkins jenkins = new Jenkins("http://localhost:8081", "admin", "admin"); //, "user1", "user1");
    assertThat(jenkins.getJob("scheduler")).isTrue();
  }

  @Ignore
  @Test
  public void testGetBuild() throws URISyntaxException, IOException {
    JenkinsServer jenkins = new JenkinsServer(new URI("http://localhost:8081"), "admin", "admin");

    JobWithDetails jobDetails = jenkins.getJob("scheduler");
    Build build = jobDetails.getBuildByNumber(8);

    //		QueueItem q = new QueueItem();
    //		q.setId(5l);
    //		Build build = jenkins.getBuild(q);
    BuildWithDetails details = build.details();
    // FileOutputStream fout = new FileOutputStream("/Users/rishi/abc.war");
    // long l = IOUtils.copy(details.downloadArtifact(details.getArtifacts().get(0)), fout);
    // fout.close();
    System.out.println(details.getId());
    System.out.println(new JsonUtils().asJson(details.getActions()));
  }
}
