package software.wings.helpers.ext.nexus.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sgurubelli on 11/17/17.
 */
@lombok.Data
public class DockerImageResponse {
  private List<String> repositories = new ArrayList<>();
}