package io.harness.k8s.model;

import com.esotericsoftware.yamlbeans.YamlException;
import io.harness.exception.WingsException;
import io.harness.k8s.manifest.ObjectYamlUtils;
import io.harness.k8s.model.Release.Status;
import io.harness.serializer.YamlUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReleaseHistory {
  public static final String defaultVersion = "v1";

  private String version;
  private List<Release> releases;

  public static ReleaseHistory createNew() {
    ReleaseHistory releaseHistory = new ReleaseHistory();
    releaseHistory.setVersion(defaultVersion);
    releaseHistory.setReleases(new ArrayList<>());
    return releaseHistory;
  }

  public static ReleaseHistory createFromData(String releaseHistory) throws IOException {
    return new YamlUtils().read(releaseHistory, ReleaseHistory.class);
  }

  public Release createNewRelease(List<KubernetesResourceId> resources) {
    int releaseNumber = 1;
    if (!this.getReleases().isEmpty()) {
      releaseNumber = getLatestRelease().getNumber() + 1;
    }
    this.getReleases().add(
        0, Release.builder().number(releaseNumber).status(Status.Started).resources(resources).build());

    return getLatestRelease();
  }

  public Release getLatestRelease() {
    if (this.getReleases().isEmpty()) {
      throw new WingsException("No existing release found.");
    }

    return this.getReleases().get(0);
  }

  public void setReleaseStatus(Status status) {
    this.getLatestRelease().setStatus(status);
  }

  public Release getLastSuccessfulRelease() {
    for (Release release : this.getReleases()) {
      if (release.getStatus() == Status.Succeeded) {
        return release;
      }
    }
    return null;
  }

  public String getAsYaml() throws YamlException {
    return ObjectYamlUtils.toYaml(this);
  }
}
