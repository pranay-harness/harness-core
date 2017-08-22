package software.wings.beans.artifact;

import static org.joor.Reflect.on;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import software.wings.exception.WingsException;
import software.wings.stencils.OverridingStencil;
import software.wings.stencils.StencilCategory;
import software.wings.utils.JsonUtils;

import java.net.URL;
import java.util.HashMap;

/**
 * The Enum ArtifactStreamType.
 */
public enum ArtifactStreamType implements ArtifactStreamTypeDescriptor {
  /**
   * Jenkins source type.
   */
  JENKINS(JenkinsArtifactStream.class, "JENKINS"), /**
                                                    * BambooService source type.
                                                    */
  BAMBOO(BambooArtifactStream.class, "BAMBOO"), /**
                                                 * Docker source type.
                                                 */
  DOCKER(DockerArtifactStream.class, "DOCKER"), /**
                                                 * ECR source type.
                                                 */
  ECR(EcrArtifactStream.class, "ECR"), /**
                                        * Google Container Registry source type.
                                        */
  GCR(GcrArtifactStream.class, "GCR"), /**
                                        * Nexus Artifact source type.
                                        */
  NEXUS(NexusArtifactStream.class, "NEXUS"), /**
                                              * Artifactory Artifact source type.
                                              */
  ARTIFACTORY(ArtifactoryArtifactStream.class, "ARTIFACTORY"), /**

   */
  ARTIFACTORYDOCKER(ArtifactoryDockerArtifactStream.class, "ARTIFACTORYDOCKER"),
  /**
   * Amazon S3 source type.
   */
  AMAZON_S3(AmazonS3ArtifactStream.class, "AMAZON_S3");

  private static final String stencilsPath = "/templates/artifactstreams/";
  private static final String uiSchemaSuffix = "-ArtifactStreamUISchema.json";

  private Object uiSchema;
  private JsonNode jsonSchema;

  @JsonIgnore private Class<? extends ArtifactStream> artifactStreamClass;
  @JsonIgnore private String name;

  ArtifactStreamType(Class<? extends ArtifactStream> artifactStreamClass, String name) {
    this.artifactStreamClass = artifactStreamClass;
    this.name = name;
    /* try {
       uiSchema = UISchemaProcessor.generate(artifactStreamClass);

     } catch (Exception e) {*/
    try {
      uiSchema = readResource(stencilsPath + name() + uiSchemaSuffix);
    } catch (Exception ex) {
      uiSchema = new HashMap<String, String>();
    }
    /*}*/
    jsonSchema = JsonUtils.jsonSchema(artifactStreamClass);
  }

  @Override
  public StencilCategory getStencilCategory() {
    return StencilCategory.BUILD;
  }

  @Override
  public Integer getDisplayOrder() {
    return DEFAULT_DISPLAY_ORDER;
  }

  @Override
  public Object getUiSchema() {
    return uiSchema;
  }

  @Override
  public JsonNode getJsonSchema() {
    return jsonSchema;
  }

  @Override
  public String getName() {
    return name;
  }

  /**
   * Gets command unit class.
   *
   * @return the command unit class
   */
  @Override
  public Class<? extends ArtifactStream> getTypeClass() {
    return artifactStreamClass;
  }

  @Override
  public OverridingStencil getOverridingStencil() {
    return new OverridingArtifactStreamDescriptor(this);
  }

  @Override
  public ArtifactStream newInstance(String id) {
    return on(artifactStreamClass).create().get();
  }

  @Override
  public String getType() {
    return name();
  }

  private Object readResource(String file) {
    try {
      URL url = this.getClass().getResource(file);
      String json = Resources.toString(url, Charsets.UTF_8);
      return JsonUtils.asObject(json, HashMap.class);
    } catch (Exception exception) {
      throw new WingsException("Error in initializing CommandUnitType-" + file, exception);
    }
  }

  @Override
  public boolean matches(Object context) {
    return true;
  }
}
