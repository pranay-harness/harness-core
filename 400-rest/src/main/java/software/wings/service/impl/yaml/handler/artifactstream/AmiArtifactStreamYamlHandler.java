package software.wings.service.impl.yaml.handler.artifactstream;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static java.util.stream.Collectors.toList;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;

import software.wings.beans.NameValuePair;
import software.wings.beans.artifact.AmiArtifactStream;
import software.wings.beans.artifact.AmiArtifactStream.FilterClass;
import software.wings.beans.artifact.AmiArtifactStream.Tag;
import software.wings.beans.artifact.AmiArtifactStreamYaml;
import software.wings.beans.yaml.ChangeContext;

import com.google.common.collect.Lists;
import com.google.inject.Singleton;
import java.util.List;
/**
 * @author rktummala on 10/09/17
 */
@OwnedBy(CDC)
@Singleton
public class AmiArtifactStreamYamlHandler extends ArtifactStreamYamlHandler<AmiArtifactStreamYaml, AmiArtifactStream> {
  @Override
  public AmiArtifactStreamYaml toYaml(AmiArtifactStream bean, String appId) {
    AmiArtifactStreamYaml yaml = AmiArtifactStreamYaml.builder().build();
    super.toYaml(yaml, bean);
    yaml.setPlatform(bean.getPlatform());
    yaml.setRegion(bean.getRegion());
    yaml.setAmiTags(getTagsYaml(bean.getTags()));
    yaml.setAmiFilters(getFiltersYaml(bean.getFilters()));
    return yaml;
  }

  @Override
  public Class getYamlClass() {
    return AmiArtifactStreamYaml.class;
  }

  @Override
  protected void toBean(AmiArtifactStream bean, ChangeContext<AmiArtifactStreamYaml> changeContext, String appId) {
    super.toBean(bean, changeContext, appId);
    AmiArtifactStreamYaml yaml = changeContext.getYaml();
    if (isEmpty(yaml.getRegion())) {
      throw new InvalidRequestException("Region cannot be null or empty");
    }
    bean.setRegion(yaml.getRegion());
    bean.setPlatform(yaml.getPlatform());
    bean.setTags(getTags(yaml.getAmiTags()));
    bean.setFilters(getFilters(yaml.getAmiFilters()));
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

  private List<NameValuePair.Yaml> getFiltersYaml(List<FilterClass> filterClassList) {
    if (isEmpty(filterClassList)) {
      return Lists.newArrayList();
    }
    return filterClassList.stream()
        .map(filterClass
            -> NameValuePair.Yaml.builder().name(filterClass.getKey()).value(filterClass.getValue()).build())
        .collect(toList());
  }

  private List<FilterClass> getFilters(List<NameValuePair.Yaml> filterYamlList) {
    return filterYamlList.stream()
        .map(filterYaml -> {
          FilterClass filterClass = new FilterClass();
          filterClass.setKey(filterYaml.getName());
          filterClass.setValue(filterYaml.getValue());
          return filterClass;
        })
        .collect(toList());
  }
}
