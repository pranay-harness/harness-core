package software.wings.service.impl.template;

import static io.harness.persistence.HQuery.excludeAuthority;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.Base.TEMPATE_UUID_KEY;
import static software.wings.beans.Variable.VariableBuilder.aVariable;
import static software.wings.beans.VariableType.TEXT;
import static software.wings.common.TemplateConstants.HARNESS_GALLERY;
import static software.wings.common.TemplateConstants.LATEST_TAG;
import static software.wings.utils.TemplateTestConstants.TEMPLATE_DESC_CHANGED;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;

import com.google.inject.Inject;

import com.mongodb.DBCursor;
import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.MorphiaIterator;
import org.mongodb.morphia.query.Query;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.CustomArtifactStream;
import software.wings.beans.template.Template;
import software.wings.beans.template.TemplateFolder;
import software.wings.beans.template.artifactsource.ArtifactSourceTemplate;
import software.wings.beans.template.artifactsource.CustomArtifactSourceTemplate;
import software.wings.beans.template.artifactsource.CustomRepositoryMapping;
import software.wings.beans.template.artifactsource.CustomRepositoryMapping.AttributeMapping;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ArtifactStreamService;

import java.util.ArrayList;
import java.util.List;

public class ArtifactSourceTemplateProcessorTest extends TemplateBaseTest {
  @Mock private WingsPersistence wingsPersistence;
  @Mock private ArtifactStreamService artifactStreamService;
  @Mock private MorphiaIterator<ArtifactStream, ArtifactStream> artifactStreamIterator;
  @Mock private Query<ArtifactStream> query;
  @Mock private FieldEnd end;
  @Mock private DBCursor dbCursor;

  @Inject private ArtifactSourceTemplateProcessor artifactSourceTemplateProcessor;

  @Test
  @Category(UnitTests.class)
  public void shouldUpdateArtifactStreamLinked() {
    Template template = constructCustomArtifactTemplateEntity();
    Template savedTemplate = templateService.save(template);
    assertThat(savedTemplate).isNotNull();
    assertThat(savedTemplate.getAppId()).isNotNull().isEqualTo(GLOBAL_APP_ID);
    assertThat(savedTemplate.getVersion()).isEqualTo(1);

    savedTemplate.setDescription(TEMPLATE_DESC_CHANGED);

    savedTemplate.setVariables(asList(aVariable().withName("MyVar").withValue("MyValue").build(),
        aVariable().withName("MySecondVar").withValue("MySecondValue").build()));

    on(artifactSourceTemplateProcessor).set("wingsPersistence", wingsPersistence);
    on(artifactSourceTemplateProcessor).set("artifactStreamService", artifactStreamService);

    CustomArtifactStream artifactStream = CustomArtifactStream.builder()
                                              .appId(APP_ID)
                                              .serviceId(SERVICE_ID)
                                              .name("Custom Artifact Stream" + System.currentTimeMillis())
                                              .build();
    artifactStream.setTemplateUuid(savedTemplate.getUuid());
    artifactStream.setTemplateVersion(LATEST_TAG);
    when(wingsPersistence.createQuery(ArtifactStream.class, excludeAuthority)).thenReturn(query);
    when(query.filter(TEMPATE_UUID_KEY, savedTemplate.getUuid())).thenReturn(query);
    when(query.fetch()).thenReturn(artifactStreamIterator);
    when(artifactStreamIterator.getCursor()).thenReturn(dbCursor);
    when(artifactStreamIterator.hasNext()).thenReturn(true).thenReturn(false);
    when(artifactStreamIterator.next()).thenReturn(artifactStream);
    templateService.updateLinkedEntities(savedTemplate);

    verify(artifactStreamService).update(artifactStream);
  }

  private Template constructCustomArtifactTemplateEntity() {
    TemplateFolder parentFolder = templateFolderService.getByFolderPath(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    List<AttributeMapping> attributeMapping = new ArrayList<>();
    attributeMapping.add(
        AttributeMapping.builder().relativePath("assets.downloadUrl").mappedAttribute("metadata.downloadUrl").build());
    CustomRepositoryMapping mapping = CustomRepositoryMapping.builder()
                                          .artifactRoot("$.items")
                                          .buildNoPath("name")
                                          .artifactAttributes(attributeMapping)
                                          .build();
    CustomArtifactSourceTemplate customArtifactSourceTemplate =
        CustomArtifactSourceTemplate.builder().script("echo \"hello world\"").customRepositoryMapping(mapping).build();
    return Template.builder()
        .type("ARTIFACT_SOURCE")
        .templateObject(ArtifactSourceTemplate.builder().artifactSource(customArtifactSourceTemplate).build())
        .folderId(parentFolder.getUuid())
        .appId(GLOBAL_APP_ID)
        .accountId(GLOBAL_ACCOUNT_ID)
        .name("Custom Artifact Template 1")
        .variables(asList(aVariable().withType(TEXT).withName("var1").withMandatory(true).build()))
        .build();
  }
}
