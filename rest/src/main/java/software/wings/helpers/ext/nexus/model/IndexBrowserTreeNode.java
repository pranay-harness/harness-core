package software.wings.helpers.ext.nexus.model;

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
  private String path;
  private String packaging;
  private String extension;

  @XmlElementWrapper(name = "children")
  @XmlElement(name = "indexBrowserTreeNode")
  private List<IndexBrowserTreeNode> children;
}
