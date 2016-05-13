package software.wings.service.intfc;

import software.wings.beans.ArtifactFile;
import software.wings.beans.ArtifactSource;

import java.util.List;
import java.util.Map;

/**
 * Created by peeyushaggarwal on 5/11/16.
 */

public interface ArtifactCollectorService {
  List<ArtifactFile> collect(ArtifactSource artifactSource, Map<String, String> arguments);
}
