package software.wings.service.impl.yaml.handler.artifactstream;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static java.util.stream.Collectors.toList;

import com.google.common.collect.Lists;
import com.google.inject.Singleton;

import software.wings.beans.NameValuePair;
import software.wings.beans.artifact.AmiArtifactStream;
import software.wings.beans.artifact.AmiArtifactStream.Tag;
import software.wings.beans.artifact.AmiArtifactStream.Yaml;
import software.wings.beans.yaml.ChangeContext;

import java.util.List;
/**
 * @author rktummala on 10/09/17
 */
@Singleton
public class AmiArtifactStreamYamlHandler extends ArtifactStreamYamlHandler<Yaml, AmiArtifactStream> {
  @Override
  public Yaml toYaml(AmiArtifactStream bean, String appId) {
    Yaml yaml = Yaml.builder().build();
    super.toYaml(yaml, bean);
    yaml.setPlatform(bean.getPlatform());
    yaml.setRegion(bean.getRegion());
    yaml.setTags(getTagsYaml(bean.getTags()));
    return yaml;
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  protected void toBean(AmiArtifactStream bean, ChangeContext<Yaml> changeContext, String appId) {
    super.toBean(bean, changeContext, appId);
    Yaml yaml = changeContext.getYaml();
    bean.setRegion(yaml.getRegion());
    bean.setPlatform(yaml.getPlatform());
    bean.setTags(getTags(yaml.getTags()));
  }

  @Override
  protected AmiArtifactStream getNewArtifactStreamObject() {
    return new AmiArtifactStream();
  }

  private List<NameValuePair.Yaml> getTagsYaml(List<Tag> tagList) {
    if (isEmpty(tagList)) {
      return Lists.newArrayList();
    }
    return tagList.stream()
        .map(tag -> NameValuePair.Yaml.builder().name(tag.getKey()).value(tag.getValue()).build())
        .collect(toList());
  }

  private List<Tag> getTags(List<NameValuePair.Yaml> tagYamlList) {
    return tagYamlList.stream()
        .map(tagYaml -> {
          Tag tag = new Tag();
          tag.setKey(tagYaml.getName());
          tag.setValue(tagYaml.getValue());
          return tag;
        })
        .collect(toList());
  }
}
