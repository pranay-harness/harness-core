package software.wings.beans.artifact;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ro.fortsoft.pf4j.ExtensionPoint;
import software.wings.stencils.Stencil;

/**
 * Created by anubhaw on 1/25/17.
 */
public interface ArtifactStreamTypeDescriptor extends ExtensionPoint, Stencil<ArtifactStream> {
  @JsonIgnore Class<? extends ArtifactStream> getTypeClass();
}
