/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

/*
 * Harness feature flag service
 * No description provided (generated by Openapi Generator https://github.com/openapitools/openapi-generator)
 *
 * The version of the OpenAPI document: 1.0.0
 * Contact: ff@harness.io
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */

package io.harness.cf.openapi.model;

import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.swagger.annotations.ApiModelProperty;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Feature
 */
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen",
    date = "2021-05-11T09:07:44.775-07:00[America/Los_Angeles]")
public class Feature {
  public static final String SERIALIZED_NAME_PROJECT = "project";
  @SerializedName(SERIALIZED_NAME_PROJECT) private String project;

  public static final String SERIALIZED_NAME_IDENTIFIER = "identifier";
  @SerializedName(SERIALIZED_NAME_IDENTIFIER) private String identifier;

  public static final String SERIALIZED_NAME_PREREQUISITES = "prerequisites";
  @SerializedName(SERIALIZED_NAME_PREREQUISITES) private List<Prerequisite> prerequisites = null;

  public static final String SERIALIZED_NAME_NAME = "name";
  @SerializedName(SERIALIZED_NAME_NAME) private String name;

  public static final String SERIALIZED_NAME_DESCRIPTION = "description";
  @SerializedName(SERIALIZED_NAME_DESCRIPTION) private String description;

  public static final String SERIALIZED_NAME_OWNER = "owner";
  @SerializedName(SERIALIZED_NAME_OWNER) private List<String> owner = null;

  /**
   * Gets or Sets kind
   */
  @JsonAdapter(KindEnum.Adapter.class)
  public enum KindEnum {
    BOOLEAN("boolean"),

    INT("int"),

    STRING("string"),

    JSON("json");

    private String value;

    KindEnum(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    public static KindEnum fromValue(String value) {
      for (KindEnum b : KindEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }

    public static class Adapter extends TypeAdapter<KindEnum> {
      @Override
      public void write(final JsonWriter jsonWriter, final KindEnum enumeration) throws IOException {
        jsonWriter.value(enumeration.getValue());
      }

      @Override
      public KindEnum read(final JsonReader jsonReader) throws IOException {
        String value = jsonReader.nextString();
        return KindEnum.fromValue(value);
      }
    }
  }

  public static final String SERIALIZED_NAME_KIND = "kind";
  @SerializedName(SERIALIZED_NAME_KIND) private KindEnum kind;

  public static final String SERIALIZED_NAME_ARCHIVED = "archived";
  @SerializedName(SERIALIZED_NAME_ARCHIVED) private Boolean archived;

  public static final String SERIALIZED_NAME_VARIATIONS = "variations";
  @SerializedName(SERIALIZED_NAME_VARIATIONS) private List<Variation> variations = new ArrayList<>();

  public static final String SERIALIZED_NAME_DEFAULT_ON_VARIATION = "defaultOnVariation";
  @SerializedName(SERIALIZED_NAME_DEFAULT_ON_VARIATION) private String defaultOnVariation;

  public static final String SERIALIZED_NAME_DEFAULT_OFF_VARIATION = "defaultOffVariation";
  @SerializedName(SERIALIZED_NAME_DEFAULT_OFF_VARIATION) private String defaultOffVariation;

  public static final String SERIALIZED_NAME_PERMANENT = "permanent";
  @SerializedName(SERIALIZED_NAME_PERMANENT) private Boolean permanent;

  public static final String SERIALIZED_NAME_ENV_PROPERTIES = "envProperties";
  @SerializedName(SERIALIZED_NAME_ENV_PROPERTIES) private FeatureEnvProperties envProperties;

  public static final String SERIALIZED_NAME_CREATED_AT = "createdAt";
  @SerializedName(SERIALIZED_NAME_CREATED_AT) private Long createdAt;

  public static final String SERIALIZED_NAME_MODIFIED_AT = "modifiedAt";
  @SerializedName(SERIALIZED_NAME_MODIFIED_AT) private Long modifiedAt;

  public static final String SERIALIZED_NAME_TAGS = "tags";
  @SerializedName(SERIALIZED_NAME_TAGS) private List<Tag> tags = null;

  public static final String SERIALIZED_NAME_EVALUATION = "evaluation";
  @SerializedName(SERIALIZED_NAME_EVALUATION) private String evaluation;

  public Feature project(String project) {
    this.project = project;
    return this;
  }

  /**
   * Get project
   * @return project
   **/
  @ApiModelProperty(required = true, value = "")

  public String getProject() {
    return project;
  }

  public void setProject(String project) {
    this.project = project;
  }

  public Feature identifier(String identifier) {
    this.identifier = identifier;
    return this;
  }

  /**
   * Get identifier
   * @return identifier
   **/
  @ApiModelProperty(required = true, value = "")

  public String getIdentifier() {
    return identifier;
  }

  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }

  public Feature prerequisites(List<Prerequisite> prerequisites) {
    this.prerequisites = prerequisites;
    return this;
  }

  public Feature addPrerequisitesItem(Prerequisite prerequisitesItem) {
    if (this.prerequisites == null) {
      this.prerequisites = new ArrayList<>();
    }
    this.prerequisites.add(prerequisitesItem);
    return this;
  }

  /**
   * Get prerequisites
   * @return prerequisites
   **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public List<Prerequisite> getPrerequisites() {
    return prerequisites;
  }

  public void setPrerequisites(List<Prerequisite> prerequisites) {
    this.prerequisites = prerequisites;
  }

  public Feature name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Get name
   * @return name
   **/
  @ApiModelProperty(required = true, value = "")

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Feature description(String description) {
    this.description = description;
    return this;
  }

  /**
   * Get description
   * @return description
   **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Feature owner(List<String> owner) {
    this.owner = owner;
    return this;
  }

  public Feature addOwnerItem(String ownerItem) {
    if (this.owner == null) {
      this.owner = new ArrayList<>();
    }
    this.owner.add(ownerItem);
    return this;
  }

  /**
   * Get owner
   * @return owner
   **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public List<String> getOwner() {
    return owner;
  }

  public void setOwner(List<String> owner) {
    this.owner = owner;
  }

  public Feature kind(KindEnum kind) {
    this.kind = kind;
    return this;
  }

  /**
   * Get kind
   * @return kind
   **/
  @ApiModelProperty(required = true, value = "")

  public KindEnum getKind() {
    return kind;
  }

  public void setKind(KindEnum kind) {
    this.kind = kind;
  }

  public Feature archived(Boolean archived) {
    this.archived = archived;
    return this;
  }

  /**
   * Get archived
   * @return archived
   **/
  @ApiModelProperty(required = true, value = "")

  public Boolean getArchived() {
    return archived;
  }

  public void setArchived(Boolean archived) {
    this.archived = archived;
  }

  public Feature variations(List<Variation> variations) {
    this.variations = variations;
    return this;
  }

  public Feature addVariationsItem(Variation variationsItem) {
    this.variations.add(variationsItem);
    return this;
  }

  /**
   * Get variations
   * @return variations
   **/
  @ApiModelProperty(required = true, value = "")

  public List<Variation> getVariations() {
    return variations;
  }

  public void setVariations(List<Variation> variations) {
    this.variations = variations;
  }

  public Feature defaultOnVariation(String defaultOnVariation) {
    this.defaultOnVariation = defaultOnVariation;
    return this;
  }

  /**
   * Get defaultOnVariation
   * @return defaultOnVariation
   **/
  @ApiModelProperty(required = true, value = "")

  public String getDefaultOnVariation() {
    return defaultOnVariation;
  }

  public void setDefaultOnVariation(String defaultOnVariation) {
    this.defaultOnVariation = defaultOnVariation;
  }

  public Feature defaultOffVariation(String defaultOffVariation) {
    this.defaultOffVariation = defaultOffVariation;
    return this;
  }

  /**
   * Get defaultOffVariation
   * @return defaultOffVariation
   **/
  @ApiModelProperty(required = true, value = "")

  public String getDefaultOffVariation() {
    return defaultOffVariation;
  }

  public void setDefaultOffVariation(String defaultOffVariation) {
    this.defaultOffVariation = defaultOffVariation;
  }

  public Feature permanent(Boolean permanent) {
    this.permanent = permanent;
    return this;
  }

  /**
   * Get permanent
   * @return permanent
   **/
  @ApiModelProperty(required = true, value = "")

  public Boolean getPermanent() {
    return permanent;
  }

  public void setPermanent(Boolean permanent) {
    this.permanent = permanent;
  }

  public Feature envProperties(FeatureEnvProperties envProperties) {
    this.envProperties = envProperties;
    return this;
  }

  /**
   * Get envProperties
   * @return envProperties
   **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public FeatureEnvProperties getEnvProperties() {
    return envProperties;
  }

  public void setEnvProperties(FeatureEnvProperties envProperties) {
    this.envProperties = envProperties;
  }

  public Feature createdAt(Long createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  /**
   * Get createdAt
   * @return createdAt
   **/
  @ApiModelProperty(required = true, value = "")

  public Long getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Long createdAt) {
    this.createdAt = createdAt;
  }

  public Feature modifiedAt(Long modifiedAt) {
    this.modifiedAt = modifiedAt;
    return this;
  }

  /**
   * Get modifiedAt
   * @return modifiedAt
   **/
  @ApiModelProperty(required = true, value = "")

  public Long getModifiedAt() {
    return modifiedAt;
  }

  public void setModifiedAt(Long modifiedAt) {
    this.modifiedAt = modifiedAt;
  }

  public Feature tags(List<Tag> tags) {
    this.tags = tags;
    return this;
  }

  public Feature addTagsItem(Tag tagsItem) {
    if (this.tags == null) {
      this.tags = new ArrayList<>();
    }
    this.tags.add(tagsItem);
    return this;
  }

  /**
   * Get tags
   * @return tags
   **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public List<Tag> getTags() {
    return tags;
  }

  public void setTags(List<Tag> tags) {
    this.tags = tags;
  }

  public Feature evaluation(String evaluation) {
    this.evaluation = evaluation;
    return this;
  }

  /**
   * Get evaluation
   * @return evaluation
   **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public String getEvaluation() {
    return evaluation;
  }

  public void setEvaluation(String evaluation) {
    this.evaluation = evaluation;
  }

  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Feature feature = (Feature) o;
    return Objects.equals(this.project, feature.project) && Objects.equals(this.identifier, feature.identifier)
        && Objects.equals(this.prerequisites, feature.prerequisites) && Objects.equals(this.name, feature.name)
        && Objects.equals(this.description, feature.description) && Objects.equals(this.owner, feature.owner)
        && Objects.equals(this.kind, feature.kind) && Objects.equals(this.archived, feature.archived)
        && Objects.equals(this.variations, feature.variations)
        && Objects.equals(this.defaultOnVariation, feature.defaultOnVariation)
        && Objects.equals(this.defaultOffVariation, feature.defaultOffVariation)
        && Objects.equals(this.permanent, feature.permanent)
        && Objects.equals(this.envProperties, feature.envProperties)
        && Objects.equals(this.createdAt, feature.createdAt) && Objects.equals(this.modifiedAt, feature.modifiedAt)
        && Objects.equals(this.tags, feature.tags) && Objects.equals(this.evaluation, feature.evaluation);
  }

  @Override
  public int hashCode() {
    return Objects.hash(project, identifier, prerequisites, name, description, owner, kind, archived, variations,
        defaultOnVariation, defaultOffVariation, permanent, envProperties, createdAt, modifiedAt, tags, evaluation);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Feature {\n");
    sb.append("    project: ").append(toIndentedString(project)).append("\n");
    sb.append("    identifier: ").append(toIndentedString(identifier)).append("\n");
    sb.append("    prerequisites: ").append(toIndentedString(prerequisites)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    owner: ").append(toIndentedString(owner)).append("\n");
    sb.append("    kind: ").append(toIndentedString(kind)).append("\n");
    sb.append("    archived: ").append(toIndentedString(archived)).append("\n");
    sb.append("    variations: ").append(toIndentedString(variations)).append("\n");
    sb.append("    defaultOnVariation: ").append(toIndentedString(defaultOnVariation)).append("\n");
    sb.append("    defaultOffVariation: ").append(toIndentedString(defaultOffVariation)).append("\n");
    sb.append("    permanent: ").append(toIndentedString(permanent)).append("\n");
    sb.append("    envProperties: ").append(toIndentedString(envProperties)).append("\n");
    sb.append("    createdAt: ").append(toIndentedString(createdAt)).append("\n");
    sb.append("    modifiedAt: ").append(toIndentedString(modifiedAt)).append("\n");
    sb.append("    tags: ").append(toIndentedString(tags)).append("\n");
    sb.append("    evaluation: ").append(toIndentedString(evaluation)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}
