import java.util.HashMap;
import java.util.Map;

import software.wings.beans.ArtifactSource;
import software.wings.beans.JenkinsArtifactSource;
import software.wings.beans.Release;
import software.wings.common.JsonUtils;

public class TestJson {
  public static void main(String[] args) {
    Release rel = new Release();
    rel.setReleaseName("Rel1.0");
    rel.setDescription("Rel1.0");
    Map<String, ArtifactSource> artifactSources = new HashMap<String, ArtifactSource>();
    JenkinsArtifactSource sampleJob = new JenkinsArtifactSource();
    sampleJob.setJenkinsURL("http://localhost:8080/jenkins/");
    sampleJob.setJobname("test-freestyle");
    sampleJob.setArtifactPathRegex("sample.tar.gz");
    sampleJob.setUsername("user1");
    sampleJob.setPassword("user1");
    artifactSources.put("sample-job", sampleJob);
    rel.setArtifactSources(artifactSources);

    Map<String, String> svcArtifactSourceMap = new HashMap<>();
    svcArtifactSourceMap.put("ui", "sample-job");
    rel.setSvcArtifactSourceMap(svcArtifactSourceMap);

    System.out.println(JsonUtils.asJson(rel));
    System.out.println("done");
  }
}
