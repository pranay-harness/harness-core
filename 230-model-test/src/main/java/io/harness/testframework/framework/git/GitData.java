/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.testframework.framework.git;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"name", "path", "sha", "size", "url", "html_url", "git_url", "download_url", "type", "_links"})
public class GitData {
  @JsonProperty("name") private String name;
  @JsonProperty("path") private String path;
  @JsonProperty("sha") private String sha;
  @JsonProperty("size") private long size;
  @JsonProperty("url") private String url;
  @JsonProperty("html_url") private String html_url;
  @JsonProperty("git_url") private String git_url;
  @JsonProperty("download_url") private String download_url;
  @JsonProperty("type") private String type;
  @JsonProperty("_links") private Links links;
  @JsonIgnore private Map<String, Object> additionalProperties = new HashMap<>();

  @JsonProperty("name")
  public String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(String name) {
    this.name = name;
  }

  @JsonProperty("path")
  public String getPath() {
    return path;
  }

  @JsonProperty("path")
  public void setPath(String path) {
    this.path = path;
  }

  @JsonProperty("sha")
  public String getSha() {
    return sha;
  }

  @JsonProperty("sha")
  public void setSha(String sha) {
    this.sha = sha;
  }

  @JsonProperty("size")
  public long getSize() {
    return size;
  }

  @JsonProperty("size")
  public void setSize(long size) {
    this.size = size;
  }

  @JsonProperty("url")
  public String getUrl() {
    return url;
  }

  @JsonProperty("url")
  public void setUrl(String url) {
    this.url = url;
  }

  @JsonProperty("html_url")
  public String getHtml_url() {
    return html_url;
  }

  @JsonProperty("html_url")
  public void setHtml_url(String html_url) {
    this.html_url = html_url;
  }

  @JsonProperty("git_url")
  public String getGit_url() {
    return git_url;
  }

  @JsonProperty("git_url")
  public void setGit_url(String git_url) {
    this.git_url = git_url;
  }

  @JsonProperty("download_url")
  public String getDownload_url() {
    return download_url;
  }

  @JsonProperty("download_url")
  public void setDownload_url(String download_url) {
    this.download_url = download_url;
  }

  @JsonProperty("type")
  public String getType() {
    return type;
  }

  @JsonProperty("type")
  public void setType(String type) {
    this.type = type;
  }

  @JsonProperty("_links")
  public Links getLinks() {
    return links;
  }

  @JsonProperty("_links")
  public void setLinks(Links links) {
    this.links = links;
  }

  @JsonAnyGetter
  public Map<String, Object> getAdditionalProperties() {
    return this.additionalProperties;
  }

  @JsonAnySetter
  public void setAdditionalProperty(String name, Object value) {
    this.additionalProperties.put(name, value);
  }
}
