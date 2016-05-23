/**
 *
 */
package software.wings.sm;

import software.wings.app.WingsBootstrap;
import software.wings.beans.Application;
import software.wings.beans.Artifact;
import software.wings.beans.Environment;
import software.wings.dl.WingsPersistence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Rishi
 */
public class WorkflowStandardParams implements ContextElement {
  private static final long serialVersionUID = 247894502473046682L;

  private String appId;
  private String envId;
  private List<String> artifactIds;

  private transient Application app;
  private transient Environment env;
  private transient List<Artifact> artifacts;

  private Long startTs;
  private Long endTs;

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public String getEnvId() {
    return envId;
  }

  public void setEnvId(String envId) {
    this.envId = envId;
  }

  public List<String> getArtifactIds() {
    return artifactIds;
  }

  public void setArtifactIds(List<String> artifactIds) {
    this.artifactIds = artifactIds;
  }

  public Application getApp() {
    if (app == null && appId != null) {
      app = WingsBootstrap.lookup(WingsPersistence.class).get(Application.class, appId);
    }
    return app;
  }

  public void setApp(Application app) {
    this.app = app;
  }

  public Environment getEnv() {
    if (env == null && envId != null) {
      env = WingsBootstrap.lookup(WingsPersistence.class).get(Environment.class, appId, envId);
    }
    return env;
  }

  public void setEnv(Environment env) {
    this.env = env;
  }

  public List<Artifact> getArtifacts() {
    if (artifacts == null && artifactIds != null && artifactIds.size() > 0) {
      List<Artifact> list = new ArrayList<>();
      for (String artifactId : artifactIds) {
        list.add(WingsBootstrap.lookup(WingsPersistence.class).get(Artifact.class, appId, artifactId));
      }
      artifacts = list;
    }
    return artifacts;
  }

  public void setArtifacts(List<Artifact> artifacts) {
    this.artifacts = artifacts;
  }

  public Long getStartTs() {
    return startTs;
  }

  public void setStartTs(Long startTs) {
    this.startTs = startTs;
  }

  public Long getEndTs() {
    return endTs;
  }

  public void setEndTs(Long endTs) {
    this.endTs = endTs;
  }

  @Override
  public Map<String, Object> paramMap() {
    Map<String, Object> map = new HashMap<>();
    map.put("app", getApp());
    map.put("env", getEnv());

    return map;
  }

  @Override
  public ContextElementType getElementType() {
    return null;
  }

  @Override
  public String getName() {
    return null;
  }
}
