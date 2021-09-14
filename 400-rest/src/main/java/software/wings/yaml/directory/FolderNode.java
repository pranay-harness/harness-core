/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.yaml.directory;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import software.wings.yaml.gitSync.YamlGitConfig;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@OwnedBy(HarnessTeam.DX)
public class FolderNode extends DirectoryNode {
  private boolean defaultToClosed;
  private List<DirectoryNode> children = new ArrayList<>();
  private String appId;
  @Getter @Setter private transient YamlGitConfig yamlGitConfig;

  public FolderNode() {
    this.setType(NodeType.FOLDER);
    this.setRestName("folders");
  }

  public FolderNode(String accountId, String name, Class theClass) {
    super(accountId, name, theClass);
    this.setType(NodeType.FOLDER);
    this.setRestName("folders");
  }

  public FolderNode(String accountId, String name, Class theClass, DirectoryPath directoryPath) {
    super(accountId, name, theClass, directoryPath, NodeType.FOLDER);
    this.setRestName("folders");
  }

  public FolderNode(String accountId, String name, Class theClass, DirectoryPath directoryPath, String appId) {
    super(accountId, name, theClass, directoryPath, NodeType.FOLDER);
    this.appId = appId;
    this.setRestName("folders");
  }

  public boolean isDefaultToClosed() {
    return defaultToClosed;
  }

  public void setDefaultToClosed(boolean defaultToClosed) {
    this.defaultToClosed = defaultToClosed;
  }

  public List<DirectoryNode> getChildren() {
    return children;
  }

  public void setChildren(List<DirectoryNode> children) {
    this.children = children;
  }

  public void addChild(DirectoryNode child) {
    this.children.add(child);
  }

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }
}
