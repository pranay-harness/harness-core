/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.nexus.model;

import java.io.Serializable;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

/**
 * Created by srinivas on 4/5/17.
 */
@XmlType(name = "indexBrowserTreeNode")
@XmlAccessorType(XmlAccessType.FIELD)
@lombok.Data
public class IndexBrowserTreeNode implements Serializable {
  private String type;
  private boolean leaf;
  private String nodeName;
  private String repositoryId;
  private String groupId;
  private String artifactId;
  private String version;
  private String path;
  private String packaging;
  private String extension;
  private String classifier;

  @XmlElementWrapper(name = "children")
  @XmlElement(name = "indexBrowserTreeNode")
  private List<IndexBrowserTreeNode> children;
}
