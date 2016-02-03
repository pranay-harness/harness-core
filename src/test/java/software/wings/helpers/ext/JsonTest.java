package software.wings.helpers.ext;

import org.junit.Test;

import software.wings.beans.Deployment;
import software.wings.beans.JenkinsArtifactSource;
import software.wings.beans.Release;
import software.wings.common.JsonUtils;

public class JsonTest {
  @Test
  public void testJson() {
    BaseA a = new BaseA();
    String jsona = JsonUtils.asJson(a);
    System.out.println(jsona);
    BaseB b = new BaseB();
    String jsonb = JsonUtils.asJson(b);
    System.out.println(jsonb);

    Base a2 = JsonUtils.asObject(jsona, Base.class);
    Base b2 = JsonUtils.asObject(jsonb, Base.class);

    System.out.println(b2.getBaseType());
  }

  @Test
  public void testJson2() {
    Release rel = new Release();
    rel.setReleaseName("TestRel");
    JenkinsArtifactSource jenkins = new JenkinsArtifactSource();
    jenkins.setJenkinsURL("http://localhost:8080/jenkins");
    jenkins.setUsername("user1");
    jenkins.setPassword("user1");
    jenkins.setJobname("test-freestyle");
    jenkins.setArtifactPathRegex("abc.war");
    rel.addArtifactSources("account", jenkins);

    String json = JsonUtils.asJson(rel);
    System.out.println(json);
  }

  @Test
  public void testJson3() {
    Deployment deployment = new Deployment();
  }
}
