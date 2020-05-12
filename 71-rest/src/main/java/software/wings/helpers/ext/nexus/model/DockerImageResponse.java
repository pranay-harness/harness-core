package software.wings.helpers.ext.nexus.model;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sgurubelli on 11/17/17.
 */
@OwnedBy(CDC)
@lombok.Data
public class DockerImageResponse {
  private List<String> repositories = new ArrayList<>();
}