package software.wings.helpers.ext.nexus.model;

import java.io.Serializable;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.sonatype.nexus.rest.model.NexusResponse;

/**
 * Created by srinivas on 4/4/17.
 */
//@XStreamAlias("content")
@XmlRootElement(name = "content")
@XmlAccessorType(XmlAccessType.FIELD)
public class IndexBrowserTreeViewResponse extends NexusResponse implements Serializable {
  @XmlElement(name = "data") private Data data;
  public Data getData() {
    return data;
  }

  public void setData(Data data) {
    this.data = data;
  }
}
