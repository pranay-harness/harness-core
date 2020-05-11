package software.wings.beans.template.artifactsource;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDC)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "artifactStreamType", include = EXTERNAL_PROPERTY)
@JsonSubTypes({ @JsonSubTypes.Type(value = CustomArtifactSourceTemplate.class, name = "CUSTOM") })
public interface ArtifactSource {}
