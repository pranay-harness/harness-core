package software.wings.service.intfc.yaml;

/**
 * Api interface for all artifact stream/build source types
 * @author rktummala on 10/09/17
 */
import software.wings.beans.Base;
import software.wings.beans.RestResponse;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.yaml.YamlPayload;

public interface YamlArtifactStreamService {
  RestResponse<YamlPayload> getArtifactStreamYaml(String appId, String artifactStreamId);

  ArtifactStream.Yaml getArtifactStreamYamlObject(String appId, String artifactStreamId);

  String getArtifactStreamYamlString(ArtifactStream artifactStream);

  String getArtifactStreamYamlString(String appId, String artifactStreamId);

  RestResponse<Base> updateArtifactStream(
      String appId, String artifactStreamId, YamlPayload yamlPayload, boolean deleteEnabled);
}
