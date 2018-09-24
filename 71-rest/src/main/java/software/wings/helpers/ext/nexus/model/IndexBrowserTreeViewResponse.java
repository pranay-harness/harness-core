package software.wings.helpers.ext.nexus.model;

import lombok.EqualsAndHashCode;
import org.sonatype.nexus.rest.model.NexusResponse;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by srinivas on 4/4/17.
 */
//@XStreamAlias("content")
@XmlRootElement(name = "content")
@XmlAccessorType(XmlAccessType.FIELD)
@lombok.Data
@EqualsAndHashCode(callSuper = false)
public class IndexBrowserTreeViewResponse extends NexusResponse implements Serializable {
  @XmlElement(name = "data") private Data data;
}
