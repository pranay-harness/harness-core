/**
 *
 */
package software.wings.sm;

import com.google.inject.Inject;

import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.Application;
import software.wings.beans.Artifact;
import software.wings.beans.Environment;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.EnvironmentService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Rishi
 */
public class WorkflowStandardParams implements ContextElement {
  @Inject private AppService appService;

  @Inject private ArtifactService artifactService;

  @Inject private EnvironmentService environmentService;

  @Inject private WingsPersistence wingsPersistence;

  private String appId;
  private String envId;
  private List<String> artifactIds;

  @Transient private transient Application app;
  @Transient private transient Environment env;
  @Transient private transient List<Artifact> artifacts;

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

  private Application getApp() {
    if (app == null && appId != null) {
      app = appService.findByUuid(appId);
    }
    return app;
  }

  private Environment getEnv() {
    if (env == null && envId != null) {
      env = environmentService.get(appId, envId);
    }
    return env;
  }

  private List<Artifact> getArtifacts() {
    if (artifacts == null && artifactIds != null && artifactIds.size() > 0) {
      List<Artifact> list = new ArrayList<>();
      for (String artifactId : artifactIds) {
        list.add(artifactService.get(appId, artifactId));
      }
      artifacts = list;
    }
    return artifacts;
  }
}
