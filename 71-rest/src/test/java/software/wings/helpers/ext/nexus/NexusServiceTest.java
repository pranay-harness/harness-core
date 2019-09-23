package software.wings.helpers.ext.nexus;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static io.harness.rule.OwnerRule.SRINIVAS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.google.inject.Inject;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.harness.category.element.UnitTests;
import io.harness.delegate.exception.ArtifactServerException;
import io.harness.exception.WingsException;
import io.harness.rule.OwnerRule.Owner;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import software.wings.WingsBaseTest;
import software.wings.beans.artifact.Artifact.ArtifactMetadataKeys;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.config.NexusConfig;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.utils.RepositoryFormat;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by srinivas on 3/30/17.
 */
public class NexusServiceTest extends WingsBaseTest {
  private static final String XML_RESPONSE = "<indexBrowserTreeViewResponse>\n"
      + "  <data>\n"
      + "    <type>G</type>\n"
      + "    <leaf>false</leaf>\n"
      + "    <nodeName>rest-client</nodeName>\n"
      + "    <path>/software/wings/nexus/rest-client/</path>\n"
      + "    <children>\n"
      + "      <indexBrowserTreeNode>\n"
      + "        <type>A</type>\n"
      + "        <leaf>false</leaf>\n"
      + "        <nodeName>rest-client</nodeName>\n"
      + "        <path>/software/wings/nexus/rest-client/</path>\n"
      + "        <children>\n"
      + "          <indexBrowserTreeNode>\n"
      + "            <type>V</type>\n"
      + "            <leaf>false</leaf>\n"
      + "            <nodeName>3.1.2</nodeName>\n"
      + "            <path>/software/wings/nexus/rest-client/3.1.2/</path>\n"
      + "            <children>\n"
      + "              <indexBrowserTreeNode>\n"
      + "                <type>artifact</type>\n"
      + "                <leaf>true</leaf>\n"
      + "                <nodeName>rest-client-3.1.2.pom</nodeName>\n"
      + "                <path>/software/wings/nexus/rest-client/3.1.2/rest-client-3.1.2.pom</path>\n"
      + "                <groupId>software.wings.nexus</groupId>\n"
      + "                <artifactId>rest-client</artifactId>\n"
      + "                <version>3.1.2</version>\n"
      + "                <repositoryId>releases</repositoryId>\n"
      + "                <locallyAvailable>false</locallyAvailable>\n"
      + "                <artifactTimestamp>0</artifactTimestamp>\n"
      + "                <extension>pom</extension>\n"
      + "                <artifactUri></artifactUri>\n"
      + "                <pomUri>http://localhost:8081/nexus/service/local/artifact/maven/redirect?r=releases&amp;g=software.wings.nexus&amp;a=rest-client&amp;v=3.1.2&amp;p=pom</pomUri>\n"
      + "              </indexBrowserTreeNode>\n"
      + "              <indexBrowserTreeNode>\n"
      + "                <type>artifact</type>\n"
      + "                <leaf>true</leaf>\n"
      + "                <nodeName>rest-client-3.1.2-capsule.jar</nodeName>\n"
      + "                <path>/software/wings/nexus/rest-client/3.1.2/rest-client-3.1.2-capsule.jar</path>\n"
      + "                <groupId>software.wings.nexus</groupId>\n"
      + "                <artifactId>rest-client</artifactId>\n"
      + "                <version>3.1.2</version>\n"
      + "                <repositoryId>releases</repositoryId>\n"
      + "                <locallyAvailable>false</locallyAvailable>\n"
      + "                <artifactTimestamp>0</artifactTimestamp>\n"
      + "                <classifier>capsule</classifier>\n"
      + "                <extension>jar</extension>\n"
      + "                <packaging>jar</packaging>\n"
      + "                <artifactUri>http://localhost:8081/nexus/service/local/artifact/maven/redirect?r=releases&amp;g=software.wings.nexus&amp;a=rest-client&amp;v=3.1.2&amp;p=jar</artifactUri>\n"
      + "                <pomUri></pomUri>\n"
      + "              </indexBrowserTreeNode>\n"
      + "            </children>\n"
      + "            <groupId>software.wings.nexus</groupId>\n"
      + "            <artifactId>rest-client</artifactId>\n"
      + "            <version>3.1.2</version>\n"
      + "            <repositoryId>releases</repositoryId>\n"
      + "            <locallyAvailable>false</locallyAvailable>\n"
      + "            <artifactTimestamp>0</artifactTimestamp>\n"
      + "          </indexBrowserTreeNode>\n"
      + "          <indexBrowserTreeNode>\n"
      + "            <type>V</type>\n"
      + "            <leaf>false</leaf>\n"
      + "            <nodeName>3.0</nodeName>\n"
      + "            <path>/software/wings/nexus/rest-client/3.0/</path>\n"
      + "            <children>\n"
      + "              <indexBrowserTreeNode>\n"
      + "                <type>artifact</type>\n"
      + "                <leaf>true</leaf>\n"
      + "                <nodeName>rest-client-3.0.jar</nodeName>\n"
      + "                <path>/software/wings/nexus/rest-client/3.0/rest-client-3.0.jar</path>\n"
      + "                <groupId>software.wings.nexus</groupId>\n"
      + "                <artifactId>rest-client</artifactId>\n"
      + "                <version>3.0</version>\n"
      + "                <repositoryId>releases</repositoryId>\n"
      + "                <locallyAvailable>false</locallyAvailable>\n"
      + "                <artifactTimestamp>0</artifactTimestamp>\n"
      + "                <extension>jar</extension>\n"
      + "                <packaging>jar</packaging>\n"
      + "                <artifactUri>http://localhost:8081/nexus/service/local/artifact/maven/redirect?r=releases&amp;g=software.wings.nexus&amp;a=rest-client&amp;v=3.0&amp;p=jar</artifactUri>\n"
      + "                <pomUri>http://localhost:8081/nexus/service/local/artifact/maven/redirect?r=releases&amp;g=software.wings.nexus&amp;a=rest-client&amp;v=3.0&amp;p=pom</pomUri>\n"
      + "              </indexBrowserTreeNode>\n"
      + "            </children>\n"
      + "            <groupId>software.wings.nexus</groupId>\n"
      + "            <artifactId>rest-client</artifactId>\n"
      + "            <version>3.0</version>\n"
      + "            <repositoryId>releases</repositoryId>\n"
      + "            <locallyAvailable>false</locallyAvailable>\n"
      + "            <artifactTimestamp>0</artifactTimestamp>\n"
      + "          </indexBrowserTreeNode>\n"
      + "        </children>\n"
      + "        <groupId>software.wings.nexus</groupId>\n"
      + "        <artifactId>rest-client</artifactId>\n"
      + "        <repositoryId>releases</repositoryId>\n"
      + "        <locallyAvailable>false</locallyAvailable>\n"
      + "        <artifactTimestamp>0</artifactTimestamp>\n"
      + "      </indexBrowserTreeNode>\n"
      + "    </children>\n"
      + "    <repositoryId>releases</repositoryId>\n"
      + "    <locallyAvailable>false</locallyAvailable>\n"
      + "    <artifactTimestamp>0</artifactTimestamp>\n"
      + "  </data>\n"
      + "</indexBrowserTreeViewResponse>";

  private static final String XML_RESPONSE_INDEX_TREE_BROWSER = "<indexBrowserTreeViewResponse>\n"
      + "    <data>\n"
      + "        <type>G</type>\n"
      + "        <leaf>false</leaf>\n"
      + "        <nodeName>demo</nodeName>\n"
      + "        <path>/io/harness/test/demo/</path>\n"
      + "        <children>\n"
      + "            <indexBrowserTreeNode>\n"
      + "                <type>A</type>\n"
      + "                <leaf>false</leaf>\n"
      + "                <nodeName>demo</nodeName>\n"
      + "                <path>/io/harness/test/demo/</path>\n"
      + "                <children>\n"
      + "                    <indexBrowserTreeNode>\n"
      + "                        <type>V</type>\n"
      + "                        <leaf>false</leaf>\n"
      + "                        <nodeName>2.0</nodeName>\n"
      + "                        <path>/io/harness/test/demo/2.0/</path>\n"
      + "                        <children>\n"
      + "                            <indexBrowserTreeNode>\n"
      + "                                <type>artifact</type>\n"
      + "                                <leaf>true</leaf>\n"
      + "                                <nodeName>demo-2.0.tar</nodeName>\n"
      + "                                <path>/io/harness/test/demo/2.0/demo-2.0.tar</path>\n"
      + "                                <groupId>io.harness.test</groupId>\n"
      + "                                <artifactId>demo</artifactId>\n"
      + "                                <version>2.0</version>\n"
      + "                                <repositoryId>releases</repositoryId>\n"
      + "                                <locallyAvailable>false</locallyAvailable>\n"
      + "                                <artifactTimestamp>0</artifactTimestamp>\n"
      + "                                <extension>tar</extension>\n"
      + "                                <packaging>jar</packaging>\n"
      + "                                <artifactUri>\n"
      + "http://localhost:8081/nexus/service/local/artifact/maven/redirect?r=releases&amp;g=io.harness.test&amp;a=demo&amp;v=2.0&amp;p=jar\n"
      + "</artifactUri>\n"
      + "                                <pomUri>\n"
      + "http://localhost:8081/nexus/service/local/artifact/maven/redirect?r=releases&amp;g=io.harness.test&amp;a=demo&amp;v=2.0&amp;p=pom\n"
      + "</pomUri>\n"
      + "                            </indexBrowserTreeNode>\n"
      + "                        </children>\n"
      + "                        <groupId>io.harness.test</groupId>\n"
      + "                        <artifactId>demo</artifactId>\n"
      + "                        <version>2.0</version>\n"
      + "                        <repositoryId>releases</repositoryId>\n"
      + "                        <locallyAvailable>false</locallyAvailable>\n"
      + "                        <artifactTimestamp>0</artifactTimestamp>\n"
      + "                    </indexBrowserTreeNode>\n"
      + "                    <indexBrowserTreeNode>\n"
      + "                        <type>V</type>\n"
      + "                        <leaf>false</leaf>\n"
      + "                        <nodeName>1.0</nodeName>\n"
      + "                        <path>/io/harness/test/demo/1.0/</path>\n"
      + "                        <children>\n"
      + "                            <indexBrowserTreeNode>\n"
      + "                                <type>artifact</type>\n"
      + "                                <leaf>true</leaf>\n"
      + "                                <nodeName>demo-1.0.tar</nodeName>\n"
      + "                                <path>/io/harness/test/demo/1.0/demo-1.0.tar</path>\n"
      + "                                <groupId>io.harness.test</groupId>\n"
      + "                                <artifactId>demo</artifactId>\n"
      + "                                <version>1.0</version>\n"
      + "                                <repositoryId>releases</repositoryId>\n"
      + "                                <locallyAvailable>false</locallyAvailable>\n"
      + "                                <artifactTimestamp>0</artifactTimestamp>\n"
      + "                                <extension>tar</extension>\n"
      + "                                <packaging>jar</packaging>\n"
      + "                                <artifactUri>\n"
      + "http://localhost:8081/nexus/service/local/artifact/maven/redirect?r=releases&amp;g=io.harness.test&amp;a=demo&amp;v=1.0&amp;p=jar\n"
      + "</artifactUri>\n"
      + "                                <pomUri>\n"
      + "http://localhost:8081/nexus/service/local/artifact/maven/redirect?r=releases&amp;g=io.harness.test&amp;a=demo&amp;v=1.0&amp;p=pom\n"
      + "</pomUri>\n"
      + "                            </indexBrowserTreeNode>\n"
      + "                        </children>\n"
      + "                        <groupId>io.harness.test</groupId>\n"
      + "                        <artifactId>demo</artifactId>\n"
      + "                        <version>1.0</version>\n"
      + "                        <repositoryId>releases</repositoryId>\n"
      + "                        <locallyAvailable>false</locallyAvailable>\n"
      + "                        <artifactTimestamp>0</artifactTimestamp>\n"
      + "                    </indexBrowserTreeNode>\n"
      + "                </children>\n"
      + "                <groupId>io.harness.test</groupId>\n"
      + "                <artifactId>demo</artifactId>\n"
      + "                <repositoryId>releases</repositoryId>\n"
      + "                <locallyAvailable>false</locallyAvailable>\n"
      + "                <artifactTimestamp>0</artifactTimestamp>\n"
      + "            </indexBrowserTreeNode>\n"
      + "        </children>\n"
      + "        <repositoryId>releases</repositoryId>\n"
      + "        <locallyAvailable>false</locallyAvailable>\n"
      + "        <artifactTimestamp>0</artifactTimestamp>\n"
      + "    </data>\n"
      + "</indexBrowserTreeViewResponse>";

  private static final String XML_CONTENT_RESPONSE_2 = "<content>\n"
      + "    <data>\n"
      + "        <content-item>\n"
      + "            <resourceURI>\n"
      + "http://localhost:8081/nexus/service/local/repositories/releases/content/io/harness/test/demo/1.0/demo-1.0.pom.sha1\n"
      + "</resourceURI>\n"
      + "            <relativePath>/io/harness/test/demo/1.0/demo-1.0.pom.sha1</relativePath>\n"
      + "            <text>demo-1.0.pom.sha1</text>\n"
      + "            <leaf>true</leaf>\n"
      + "            <lastModified>2019-09-09 06:18:02.0 UTC</lastModified>\n"
      + "            <sizeOnDisk>40</sizeOnDisk>\n"
      + "        </content-item>\n"
      + "        <content-item>\n"
      + "            <resourceURI>\n"
      + "http://localhost:8081/nexus/service/local/repositories/releases/content/io/harness/test/demo/1.0/demo-1.0.jar\n"
      + "</resourceURI>\n"
      + "            <relativePath>/io/harness/test/demo/1.0/demo-1.0.jar</relativePath>\n"
      + "            <text>demo-1.0.jar</text>\n"
      + "            <leaf>true</leaf>\n"
      + "            <lastModified>2019-09-09 06:18:03.0 UTC</lastModified>\n"
      + "            <sizeOnDisk>1667</sizeOnDisk>\n"
      + "        </content-item>\n"
      + "        <content-item>\n"
      + "            <resourceURI>\n"
      + "http://localhost:8081/nexus/service/local/repositories/releases/content/io/harness/test/demo/1.0/demo-1.0-binary.jar\n"
      + "</resourceURI>\n"
      + "            <relativePath>/io/harness/test/demo/1.0/demo-1.0-binary.jar</relativePath>\n"
      + "            <text>demo-1.0-binary.jar</text>\n"
      + "            <leaf>true</leaf>\n"
      + "            <lastModified>2019-09-09 06:18:03.0 UTC</lastModified>\n"
      + "            <sizeOnDisk>1667</sizeOnDisk>\n"
      + "        </content-item>\n"
      + "        <content-item>\n"
      + "            <resourceURI>\n"
      + "http://localhost:8081/nexus/service/local/repositories/releases/content/io/harness/test/demo/1.0/demo-1.0.pom\n"
      + "</resourceURI>\n"
      + "            <relativePath>/io/harness/test/demo/1.0/demo-1.0.pom</relativePath>\n"
      + "            <text>demo-1.0.pom</text>\n"
      + "            <leaf>true</leaf>\n"
      + "            <lastModified>2019-09-09 06:18:02.0 UTC</lastModified>\n"
      + "            <sizeOnDisk>1402</sizeOnDisk>\n"
      + "        </content-item>\n"
      + "        <content-item>\n"
      + "            <resourceURI>\n"
      + "http://localhost:8081/nexus/service/local/repositories/releases/content/io/harness/test/demo/1.0/demo-1.0.tar.sha1\n"
      + "</resourceURI>\n"
      + "            <relativePath>/io/harness/test/demo/1.0/demo-1.0.tar.sha1</relativePath>\n"
      + "            <text>demo-1.0.tar.sha1</text>\n"
      + "            <leaf>true</leaf>\n"
      + "            <lastModified>2019-09-09 06:18:51.0 UTC</lastModified>\n"
      + "            <sizeOnDisk>40</sizeOnDisk>\n"
      + "        </content-item>\n"
      + "        <content-item>\n"
      + "            <resourceURI>\n"
      + "http://localhost:8081/nexus/service/local/repositories/releases/content/io/harness/test/demo/1.0/demo-1.0.pom.md5\n"
      + "</resourceURI>\n"
      + "            <relativePath>/io/harness/test/demo/1.0/demo-1.0.pom.md5</relativePath>\n"
      + "            <text>demo-1.0.pom.md5</text>\n"
      + "            <leaf>true</leaf>\n"
      + "            <lastModified>2019-09-09 06:18:02.0 UTC</lastModified>\n"
      + "            <sizeOnDisk>32</sizeOnDisk>\n"
      + "        </content-item>\n"
      + "        <content-item>\n"
      + "            <resourceURI>\n"
      + "http://localhost:8081/nexus/service/local/repositories/releases/content/io/harness/test/demo/1.0/demo-1.0.jar.md5\n"
      + "</resourceURI>\n"
      + "            <relativePath>/io/harness/test/demo/1.0/demo-1.0.jar.md5</relativePath>\n"
      + "            <text>demo-1.0.jar.md5</text>\n"
      + "            <leaf>true</leaf>\n"
      + "            <lastModified>2019-09-09 06:18:03.0 UTC</lastModified>\n"
      + "            <sizeOnDisk>32</sizeOnDisk>\n"
      + "        </content-item>\n"
      + "        <content-item>\n"
      + "            <resourceURI>\n"
      + "http://localhost:8081/nexus/service/local/repositories/releases/content/io/harness/test/demo/1.0/demo-1.0.tar.md5\n"
      + "</resourceURI>\n"
      + "            <relativePath>/io/harness/test/demo/1.0/demo-1.0.tar.md5</relativePath>\n"
      + "            <text>demo-1.0.tar.md5</text>\n"
      + "            <leaf>true</leaf>\n"
      + "            <lastModified>2019-09-09 06:18:51.0 UTC</lastModified>\n"
      + "            <sizeOnDisk>32</sizeOnDisk>\n"
      + "        </content-item>\n"
      + "        <content-item>\n"
      + "            <resourceURI>\n"
      + "http://localhost:8081/nexus/service/local/repositories/releases/content/io/harness/test/demo/1.0/demo-1.0.jar.sha1\n"
      + "</resourceURI>\n"
      + "            <relativePath>/io/harness/test/demo/1.0/demo-1.0.jar.sha1</relativePath>\n"
      + "            <text>demo-1.0.jar.sha1</text>\n"
      + "            <leaf>true</leaf>\n"
      + "            <lastModified>2019-09-09 06:18:03.0 UTC</lastModified>\n"
      + "            <sizeOnDisk>40</sizeOnDisk>\n"
      + "        </content-item>\n"
      + "        <content-item>\n"
      + "            <resourceURI>\n"
      + "http://localhost:8081/nexus/service/local/repositories/releases/content/io/harness/test/demo/1.0/demo-1.0.tar\n"
      + "</resourceURI>\n"
      + "            <relativePath>/io/harness/test/demo/1.0/demo-1.0.tar</relativePath>\n"
      + "            <text>demo-1.0.tar</text>\n"
      + "            <leaf>true</leaf>\n"
      + "            <lastModified>2019-09-09 06:18:51.0 UTC</lastModified>\n"
      + "            <sizeOnDisk>0</sizeOnDisk>\n"
      + "        </content-item>\n"
      + "    </data>\n"
      + "</content>";

  private static final String XML_CONTENT_RESPONSE_1 = "<content>\n"
      + "    <data>\n"
      + "        <content-item>\n"
      + "            <resourceURI>\n"
      + "http://localhost:8081/nexus/service/local/repositories/releases/content/io/harness/test/demo/2.0/demo-2.0.tar.md5\n"
      + "</resourceURI>\n"
      + "            <relativePath>/io/harness/test/demo/2.0/demo-2.0.tar.md5</relativePath>\n"
      + "            <text>demo-2.0.tar.md5</text>\n"
      + "            <leaf>true</leaf>\n"
      + "            <lastModified>2019-09-06 21:16:35.0 UTC</lastModified>\n"
      + "            <sizeOnDisk>32</sizeOnDisk>\n"
      + "        </content-item>\n"
      + "        <content-item>\n"
      + "            <resourceURI>\n"
      + "http://localhost:8081/nexus/service/local/repositories/releases/content/io/harness/test/demo/2.0/demo-2.0.jar\n"
      + "</resourceURI>\n"
      + "            <relativePath>/io/harness/test/demo/2.0/demo-2.0.jar</relativePath>\n"
      + "            <text>demo-2.0.jar</text>\n"
      + "            <leaf>true</leaf>\n"
      + "            <lastModified>2019-09-06 21:15:54.0 UTC</lastModified>\n"
      + "            <sizeOnDisk>1652</sizeOnDisk>\n"
      + "        </content-item>\n"
      + "        <content-item>\n"
      + "            <resourceURI>\n"
      + "http://localhost:8081/nexus/service/local/repositories/releases/content/io/harness/test/demo/2.0/demo-2.0.pom\n"
      + "</resourceURI>\n"
      + "            <relativePath>/io/harness/test/demo/2.0/demo-2.0.pom</relativePath>\n"
      + "            <text>demo-2.0.pom</text>\n"
      + "            <leaf>true</leaf>\n"
      + "            <lastModified>2019-09-06 21:15:54.0 UTC</lastModified>\n"
      + "            <sizeOnDisk>1402</sizeOnDisk>\n"
      + "        </content-item>\n"
      + "        <content-item>\n"
      + "            <resourceURI>\n"
      + "http://localhost:8081/nexus/service/local/repositories/releases/content/io/harness/test/demo/2.0/demo-2.0.tar.sha1\n"
      + "</resourceURI>\n"
      + "            <relativePath>/io/harness/test/demo/2.0/demo-2.0.tar.sha1</relativePath>\n"
      + "            <text>demo-2.0.tar.sha1</text>\n"
      + "            <leaf>true</leaf>\n"
      + "            <lastModified>2019-09-06 21:16:34.0 UTC</lastModified>\n"
      + "            <sizeOnDisk>40</sizeOnDisk>\n"
      + "        </content-item>\n"
      + "        <content-item>\n"
      + "            <resourceURI>\n"
      + "http://localhost:8081/nexus/service/local/repositories/releases/content/io/harness/test/demo/2.0/demo-2.0.jar.sha1\n"
      + "</resourceURI>\n"
      + "            <relativePath>/io/harness/test/demo/2.0/demo-2.0.jar.sha1</relativePath>\n"
      + "            <text>demo-2.0.jar.sha1</text>\n"
      + "            <leaf>true</leaf>\n"
      + "            <lastModified>2019-09-06 21:15:54.0 UTC</lastModified>\n"
      + "            <sizeOnDisk>40</sizeOnDisk>\n"
      + "        </content-item>\n"
      + "        <content-item>\n"
      + "            <resourceURI>\n"
      + "http://localhost:8081/nexus/service/local/repositories/releases/content/io/harness/test/demo/2.0/demo-2.0.pom.sha1\n"
      + "</resourceURI>\n"
      + "            <relativePath>/io/harness/test/demo/2.0/demo-2.0.pom.sha1</relativePath>\n"
      + "            <text>demo-2.0.pom.sha1</text>\n"
      + "            <leaf>true</leaf>\n"
      + "            <lastModified>2019-09-06 21:15:54.0 UTC</lastModified>\n"
      + "            <sizeOnDisk>40</sizeOnDisk>\n"
      + "        </content-item>\n"
      + "        <content-item>\n"
      + "            <resourceURI>\n"
      + "http://localhost:8081/nexus/service/local/repositories/releases/content/io/harness/test/demo/2.0/demo-2.0.pom.md5\n"
      + "</resourceURI>\n"
      + "            <relativePath>/io/harness/test/demo/2.0/demo-2.0.pom.md5</relativePath>\n"
      + "            <text>demo-2.0.pom.md5</text>\n"
      + "            <leaf>true</leaf>\n"
      + "            <lastModified>2019-09-06 21:15:54.0 UTC</lastModified>\n"
      + "            <sizeOnDisk>32</sizeOnDisk>\n"
      + "        </content-item>\n"
      + "        <content-item>\n"
      + "            <resourceURI>\n"
      + "http://localhost:8081/nexus/service/local/repositories/releases/content/io/harness/test/demo/2.0/demo-2.0.jar.md5\n"
      + "</resourceURI>\n"
      + "            <relativePath>/io/harness/test/demo/2.0/demo-2.0.jar.md5</relativePath>\n"
      + "            <text>demo-2.0.jar.md5</text>\n"
      + "            <leaf>true</leaf>\n"
      + "            <lastModified>2019-09-06 21:15:54.0 UTC</lastModified>\n"
      + "            <sizeOnDisk>32</sizeOnDisk>\n"
      + "        </content-item>\n"
      + "        <content-item>\n"
      + "            <resourceURI>\n"
      + "http://localhost:8081/nexus/service/local/repositories/releases/content/io/harness/test/demo/2.0/demo-2.0.tar\n"
      + "</resourceURI>\n"
      + "            <relativePath>/io/harness/test/demo/2.0/demo-2.0.tar</relativePath>\n"
      + "            <text>demo-2.0.tar</text>\n"
      + "            <leaf>true</leaf>\n"
      + "            <lastModified>2019-09-06 21:16:34.0 UTC</lastModified>\n"
      + "            <sizeOnDisk>0</sizeOnDisk>\n"
      + "        </content-item>\n"
      + "    </data>\n"
      + "</content>";
  /**
   * The Wire mock rule.
   */
  @Rule public WireMockRule wireMockRule = new WireMockRule(8881);

  private static final String DEFAULT_NEXUS_URL = "http://localhost:8881/nexus/";

  @Inject @InjectMocks private NexusService nexusService;

  private NexusConfig nexusConfig =
      NexusConfig.builder().nexusUrl(DEFAULT_NEXUS_URL).username("admin").password("wings123!".toCharArray()).build();
  private NexusConfig nexusThreeConfig = NexusConfig.builder()
                                             .nexusUrl(DEFAULT_NEXUS_URL)
                                             .version("3.x")
                                             .username("admin")
                                             .password("wings123!".toCharArray())
                                             .build();

  @Test
  @Category(UnitTests.class)
  public void shouldGetRepositories() {
    wireMockRule.stubFor(
        get(urlEqualTo("/nexus/service/local/repositories"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withBody("<repositories>\n"
                        + "    <data>\n"
                        + "        <repositories-item>\n"
                        + "            <resourceURI>http://localhost:8081/nexus/service/local/repositories/snapshots</resourceURI>\n"
                        + "            <contentResourceURI>http://localhost:8081/nexus/content/repositories/snapshots</contentResourceURI>\n"
                        + "            <id>snapshots</id>\n"
                        + "            <name>Snapshots</name>\n"
                        + "            <repoType>hosted</repoType>\n"
                        + "            <repoPolicy>SNAPSHOT</repoPolicy>\n"
                        + "            <provider>maven2</provider>\n"
                        + "            <providerRole>org.sonatype.nexus.proxy.repository.Repository</providerRole>\n"
                        + "            <format>maven2</format>\n"
                        + "            <userManaged>true</userManaged>\n"
                        + "            <exposed>true</exposed>\n"
                        + "            <effectiveLocalStorageUrl>file:/usr/local/var/nexus/storage/snapshots/</effectiveLocalStorageUrl>\n"
                        + "        </repositories-item>"
                        + " </data>"
                        + "</repositories>")
                    .withHeader("Content-Type", "application/xml")));
    assertThat(nexusService.getRepositories(nexusConfig, null)).hasSize(1).containsEntry("snapshots", "Snapshots");
  }

  @Test
  @Category(UnitTests.class)
  public void shouldGetArtifactPaths() {
    wireMockRule.stubFor(
        get(urlEqualTo("/nexus/service/local/repositories/releases/content/"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withBody(""
                        + "<content>\n"
                        + "  <data>\n"
                        + "    <content-item>\n"
                        + "      <resourceURI>http://localhost:8081/nexus/service/local/repositories/releases/content/archetype-catalog.xml</resourceURI>\n"
                        + "      <relativePath>/archetype-catalog.xml</relativePath>\n"
                        + "      <text>archetype-catalog.xml</text>\n"
                        + "      <leaf>true</leaf>\n"
                        + "      <lastModified>2017-04-02 04:20:37.830 UTC</lastModified>\n"
                        + "      <sizeOnDisk>25</sizeOnDisk>\n"
                        + "    </content-item>\n"
                        + "    <content-item>\n"
                        + "      <resourceURI>http://localhost:8081/nexus/service/local/repositories/releases/content/fakepath/</resourceURI>\n"
                        + "      <relativePath>/fakepath/</relativePath>\n"
                        + "      <text>fakepath</text>\n"
                        + "      <leaf>false</leaf>\n"
                        + "      <lastModified>2017-03-30 20:08:56.0 UTC</lastModified>\n"
                        + "      <sizeOnDisk>-1</sizeOnDisk>\n"
                        + "    </content-item>\n"
                        + "  </data>\n"
                        + "</content>")
                    .withHeader("Content-Type", "application/xml")));
    assertThat(nexusService.getArtifactPaths(nexusConfig, null, "releases")).hasSize(2).contains("/fakepath/");
  }

  @Test
  @Category(UnitTests.class)
  public void shouldGetArtifactPathsByRepo() {
    wireMockRule.stubFor(
        get(urlEqualTo("/nexus/service/local/repositories/releases/content/fakepath"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withBody(""
                        + "<content>\n"
                        + "  <data>\n"
                        + "    <content-item>\n"
                        + "      <resourceURI>http://localhost:8081/nexus/service/local/repositories/releases/content/fakepath/nexus-client-core/</resourceURI>\n"
                        + "      <relativePath>/fakepath/nexus-client-core/</relativePath>\n"
                        + "      <text>nexus-client-core</text>\n"
                        + "      <leaf>true</leaf>\n"
                        + "      <lastModified>2017-04-02 04:20:37.830 UTC</lastModified>\n"
                        + "      <sizeOnDisk>25</sizeOnDisk>\n"
                        + "    </content-item>\n"
                        + "  </data>\n"
                        + "</content>")
                    .withHeader("Content-Type", "application/xml")));
    assertThat(nexusService.getArtifactPaths(nexusConfig, null, "releases", "fakepath"))
        .hasSize(1)
        .contains("/fakepath/nexus-client-core/");
  }

  @Test
  @Category(UnitTests.class)
  public void shouldGetArtifactPathsByRepoStartsWithUrl() {
    wireMockRule.stubFor(
        get(urlEqualTo("/nexus/service/local/repositories/releases/content/fakepath"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withBody(""
                        + "<content>\n"
                        + "  <data>\n"
                        + "    <content-item>\n"
                        + "      <resourceURI>http://localhost:8081/nexus/service/local/repositories/releases/content/fakepath/nexus-client-core/</resourceURI>\n"
                        + "      <relativePath>/fakepath/nexus-client-core/</relativePath>\n"
                        + "      <text>nexus-client-core</text>\n"
                        + "      <leaf>true</leaf>\n"
                        + "      <lastModified>2017-04-02 04:20:37.830 UTC</lastModified>\n"
                        + "      <sizeOnDisk>25</sizeOnDisk>\n"
                        + "    </content-item>\n"
                        + "  </data>\n"
                        + "</content>")
                    .withHeader("Content-Type", "application/xml")));
    assertThat(nexusService.getArtifactPaths(nexusConfig, null, "releases", "/fakepath"))
        .hasSize(1)
        .contains("/fakepath/nexus-client-core/");
  }

  @Test
  @Category(UnitTests.class)
  public void shouldGetRepositoriesError() {
    wireMockRule.stubFor(get(urlEqualTo("/nexus/service/local/repositories"))
                             .willReturn(aResponse()
                                             .withStatus(200)
                                             .withFault(Fault.MALFORMED_RESPONSE_CHUNK)
                                             .withHeader("Content-Type", "application/xml")));
    assertThatThrownBy(() -> nexusService.getRepositories(nexusConfig, null))
        .isInstanceOf(WingsException.class)
        .hasMessageContaining("INVALID_ARTIFACT_SERVER");
  }

  @Test
  @Category(UnitTests.class)
  public void shouldGetArtifactPathsByRepoError() {
    wireMockRule.stubFor(get(urlEqualTo("/nexus/service/local/repositories/releases/content/"))
                             .willReturn(aResponse()
                                             .withStatus(200)
                                             .withFault(Fault.EMPTY_RESPONSE)
                                             .withHeader("Content-Type", "application/xml")));

    assertThatThrownBy(() -> nexusService.getArtifactPaths(nexusConfig, null, "releases"))
        .isInstanceOf(WingsException.class)
        .hasMessageContaining("INVALID_ARTIFACT_SERVER");
  }

  @Test
  @Owner(emails = SRINIVAS)
  @Category(UnitTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldGetArtifactPathsByRepoStartsWithUrlError() {
    wireMockRule.stubFor(get(urlEqualTo("/nexus/service/local/repositories/releases/content/fakepath"))
                             .willReturn(aResponse()
                                             .withStatus(200)
                                             .withFault(Fault.RANDOM_DATA_THEN_CLOSE)
                                             .withHeader("Content-Type", "application/xml")));

    assertThatThrownBy(() -> nexusService.getArtifactPaths(nexusConfig, null, "releases", "fakepath"))
        .isInstanceOf(WingsException.class)
        .hasMessageContaining("INVALID_ARTIFACT_SERVER");
  }

  @Test
  @Owner(emails = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldGetGroupIdPaths() {
    wireMockRule.stubFor(get(urlEqualTo("/nexus/service/local/repositories/releases/index_content/"))
                             .willReturn(aResponse()
                                             .withStatus(200)
                                             .withBody("<indexBrowserTreeViewResponse>\n"
                                                 + "  <data>\n"
                                                 + "    <type>G</type>\n"
                                                 + "    <leaf>false</leaf>\n"
                                                 + "    <nodeName>/</nodeName>\n"
                                                 + "    <path>/</path>\n"
                                                 + "    <children>\n"
                                                 + "      <indexBrowserTreeNode>\n"
                                                 + "        <type>G</type>\n"
                                                 + "        <leaf>false</leaf>\n"
                                                 + "        <nodeName>fakepath</nodeName>\n"
                                                 + "        <path>/fakepath/</path>\n"
                                                 + "        <repositoryId>releases</repositoryId>\n"
                                                 + "        <locallyAvailable>false</locallyAvailable>\n"
                                                 + "        <artifactTimestamp>0</artifactTimestamp>\n"
                                                 + "      </indexBrowserTreeNode>\n"
                                                 + "    </children>\n"
                                                 + "    <repositoryId>releases</repositoryId>\n"
                                                 + "    <locallyAvailable>false</locallyAvailable>\n"
                                                 + "    <artifactTimestamp>0</artifactTimestamp>\n"
                                                 + "  </data>\n"
                                                 + "</indexBrowserTreeViewResponse>")
                                             .withHeader("Content-Type", "application/xml")));

    wireMockRule.stubFor(get(urlEqualTo("/nexus/service/local/repositories/releases/index_content/fakepath/"))
                             .willReturn(aResponse()
                                             .withStatus(200)
                                             .withBody("<indexBrowserTreeViewResponse>\n"
                                                 + "  <data>\n"
                                                 + "    <type>G</type>\n"
                                                 + "    <leaf>false</leaf>\n"
                                                 + "    <nodeName>nexus-client-core</nodeName>\n"
                                                 + "    <path>/fakepath/nexus-cleint-core</path>\n"
                                                 + "    <children>\n"
                                                 + "      <indexBrowserTreeNode>\n"
                                                 + "        <type>A</type>\n"
                                                 + "        <leaf>false</leaf>\n"
                                                 + "        <nodeName>nexus-client-core</nodeName>\n"
                                                 + "        <path>/fakepath/nexus-cleint-core</path>\n"
                                                 + "        <repositoryId>releases</repositoryId>\n"
                                                 + "        <locallyAvailable>false</locallyAvailable>\n"
                                                 + "        <artifactTimestamp>0</artifactTimestamp>\n"
                                                 + "      </indexBrowserTreeNode>\n"
                                                 + "    </children>\n"
                                                 + "    <repositoryId>releases</repositoryId>\n"
                                                 + "    <locallyAvailable>false</locallyAvailable>\n"
                                                 + "    <artifactTimestamp>0</artifactTimestamp>\n"
                                                 + "  </data>\n"
                                                 + "</indexBrowserTreeViewResponse>")
                                             .withHeader("Content-Type", "application/xml")));

    assertThat(nexusService.getGroupIdPaths(nexusConfig, null, "releases", null)).hasSize(1).contains("fakepath");
  }

  @Test
  @Category(UnitTests.class)
  public void shouldGetArtifactNames() {
    wireMockRule.stubFor(
        get(urlEqualTo("/nexus/service/local/repositories/releases/index_content/software/wings/nexus/"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withBody("<indexBrowserTreeViewResponse>\n"
                        + "  <data>\n"
                        + "    <type>G</type>\n"
                        + "    <leaf>false</leaf>\n"
                        + "    <nodeName>nexus</nodeName>\n"
                        + "    <path>/software/wings/nexus/</path>\n"
                        + "    <children>\n"
                        + "      <indexBrowserTreeNode>\n"
                        + "        <type>A</type>\n"
                        + "        <leaf>false</leaf>\n"
                        + "        <nodeName>rest-client</nodeName>\n"
                        + "        <path>/software/wings/nexus/rest-client/</path>\n"
                        + "        <children>\n"
                        + "          <indexBrowserTreeNode>\n"
                        + "            <type>V</type>\n"
                        + "            <leaf>false</leaf>\n"
                        + "            <nodeName>2.1.2</nodeName>\n"
                        + "            <path>/software/wings/nexus/rest-client/2.1.2/</path>\n"
                        + "            <children>\n"
                        + "              <indexBrowserTreeNode>\n"
                        + "                <type>artifact</type>\n"
                        + "                <leaf>true</leaf>\n"
                        + "                <nodeName>rest-client-2.1.2.pom</nodeName>\n"
                        + "                <path>/software/wings/nexus/rest-client/2.1.2/rest-client-2.1.2.pom</path>\n"
                        + "                <groupId>software.wings.nexus</groupId>\n"
                        + "                <artifactId>rest-client</artifactId>\n"
                        + "                <version>2.1.2</version>\n"
                        + "                <repositoryId>releases</repositoryId>\n"
                        + "                <locallyAvailable>false</locallyAvailable>\n"
                        + "                <artifactTimestamp>0</artifactTimestamp>\n"
                        + "                <extension>pom</extension>\n"
                        + "                <artifactUri></artifactUri>\n"
                        + "                <pomUri>http://localhost:8081/nexus/service/local/artifact/maven/redirect?r=releases&amp;g=software.wings.nexus&amp;a=rest-client&amp;v=2.1.2&amp;p=pom</pomUri>\n"
                        + "              </indexBrowserTreeNode>\n"
                        + "              <indexBrowserTreeNode>\n"
                        + "                <type>artifact</type>\n"
                        + "                <leaf>true</leaf>\n"
                        + "                <nodeName>rest-client-2.1.2-capsule.jar</nodeName>\n"
                        + "                <path>/software/wings/nexus/rest-client/2.1.2/rest-client-2.1.2-capsule.jar</path>\n"
                        + "                <groupId>software.wings.nexus</groupId>\n"
                        + "                <artifactId>rest-client</artifactId>\n"
                        + "                <version>2.1.2</version>\n"
                        + "                <repositoryId>releases</repositoryId>\n"
                        + "                <locallyAvailable>false</locallyAvailable>\n"
                        + "                <artifactTimestamp>0</artifactTimestamp>\n"
                        + "                <classifier>capsule</classifier>\n"
                        + "                <extension>jar</extension>\n"
                        + "                <packaging>jar</packaging>\n"
                        + "                <artifactUri>http://localhost:8081/nexus/service/local/artifact/maven/redirect?r=releases&amp;g=software.wings.nexus&amp;a=rest-client&amp;v=2.1.2&amp;p=jar</artifactUri>\n"
                        + "                <pomUri></pomUri>\n"
                        + "              </indexBrowserTreeNode>\n"
                        + "            </children>\n"
                        + "            <groupId>software.wings.nexus</groupId>\n"
                        + "            <artifactId>rest-client</artifactId>\n"
                        + "            <version>2.1.2</version>\n"
                        + "            <repositoryId>releases</repositoryId>\n"
                        + "            <locallyAvailable>false</locallyAvailable>\n"
                        + "            <artifactTimestamp>0</artifactTimestamp>\n"
                        + "          </indexBrowserTreeNode>\n"
                        + "        </children>\n"
                        + "        <groupId>software.wings.nexus</groupId>\n"
                        + "        <artifactId>rest-client</artifactId>\n"
                        + "        <repositoryId>releases</repositoryId>\n"
                        + "        <locallyAvailable>false</locallyAvailable>\n"
                        + "        <artifactTimestamp>0</artifactTimestamp>\n"
                        + "      </indexBrowserTreeNode>\n"
                        + "    </children>\n"
                        + "    <repositoryId>releases</repositoryId>\n"
                        + "    <locallyAvailable>false</locallyAvailable>\n"
                        + "    <artifactTimestamp>0</artifactTimestamp>\n"
                        + "  </data>\n"
                        + "</indexBrowserTreeViewResponse>")
                    .withHeader("Content-Type", "application/xml")));

    assertThat(nexusService.getArtifactNames(nexusConfig, null, "releases", "software.wings.nexus"))
        .hasSize(1)
        .contains("rest-client");
  }

  @Test
  @Category(UnitTests.class)
  public void shouldGetVersions() {
    wireMockRule.stubFor(
        get(urlEqualTo("/nexus/service/local/repositories/releases/index_content/software/wings/nexus/rest-client/"))
            .willReturn(
                aResponse().withStatus(200).withBody(XML_RESPONSE).withHeader("Content-Type", "application/xml")));

    List<BuildDetails> buildDetails =
        nexusService.getVersions(nexusConfig, null, "releases", "software.wings.nexus", "rest-client", null, null);
    assertThat(buildDetails)
        .hasSize(2)
        .extracting(BuildDetails::getNumber, BuildDetails::getRevision)
        .containsExactly(tuple("3.0", "3.0"), tuple("3.1.2", "3.1.2"));

    assertThat(buildDetails)
        .hasSize(2)
        .extracting(BuildDetails::getBuildUrl)
        .containsExactly(
            "http://localhost:8881/nexus/service/local/artifact/maven/content?r=releases&g=software.wings.nexus&a=rest-client&v=3.0&p=jar&e=jar",
            "http://localhost:8881/nexus/service/local/artifact/maven/content?r=releases&g=software.wings.nexus&a=rest-client&v=3.1.2&p=jar&e=jar&c=capsule");
  }

  @Test
  @Category(UnitTests.class)
  public void shouldGetVersionsWithExtensionAndClassifier() {
    wireMockRule.stubFor(
        get(urlEqualTo("/nexus/service/local/repositories/releases/index_content/software/wings/nexus/rest-client/"))
            .willReturn(
                aResponse().withStatus(200).withBody(XML_RESPONSE).withHeader("Content-Type", "application/xml")));

    List<BuildDetails> buildDetails = nexusService.getVersions(
        nexusConfig, null, "releases", "software.wings.nexus", "rest-client", "jar", "sources");
    assertThat(buildDetails)
        .hasSize(2)
        .extracting(BuildDetails::getNumber, BuildDetails::getRevision)
        .containsExactly(tuple("3.0", "3.0"), tuple("3.1.2", "3.1.2"));

    assertThat(buildDetails)
        .hasSize(2)
        .extracting(BuildDetails::getBuildUrl)
        .containsExactly(
            "http://localhost:8881/nexus/service/local/artifact/maven/content?r=releases&g=software.wings.nexus&a=rest-client&v=3.0&p=jar&e=jar&c=sources",
            "http://localhost:8881/nexus/service/local/artifact/maven/content?r=releases&g=software.wings.nexus&a=rest-client&v=3.1.2&p=jar&e=jar&c=sources");
  }

  @Test
  @Owner(emails = SRINIVAS)
  @Category(UnitTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldDownloadArtifact() {
    setPomModelWireMock();

    // Return artifacts under version
    wireMockRule.stubFor(
        get(urlEqualTo(
                "/nexus/service/local/repositories/releases/index_content/software/wings/nexus/rest-client/3.0/"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withBody("<indexBrowserTreeViewResponse>\n"
                        + "  <data>\n"
                        + "    <type>G</type>\n"
                        + "    <leaf>false</leaf>\n"
                        + "    <nodeName>3.0</nodeName>\n"
                        + "    <path>/software/wings/nexus/rest-client/3.0/</path>\n"
                        + "    <children>\n"
                        + "      <indexBrowserTreeNode>\n"
                        + "        <type>A</type>\n"
                        + "        <leaf>false</leaf>\n"
                        + "        <nodeName>rest-client</nodeName>\n"
                        + "        <path>/software/wings/nexus/rest-client/</path>\n"
                        + "        <children>\n"
                        + "          <indexBrowserTreeNode>\n"
                        + "            <type>V</type>\n"
                        + "            <leaf>false</leaf>\n"
                        + "            <nodeName>3.0</nodeName>\n"
                        + "            <path>/software/wings/nexus/rest-client/3.0/</path>\n"
                        + "            <children>\n"
                        + "              <indexBrowserTreeNode>\n"
                        + "                <type>artifact</type>\n"
                        + "                <leaf>true</leaf>\n"
                        + "                <nodeName>rest-client-3.0.jar</nodeName>\n"
                        + "                <path>/software/wings/nexus/rest-client/3.0/rest-client-3.0.jar</path>\n"
                        + "                <groupId>software.wings.nexus</groupId>\n"
                        + "                <artifactId>rest-client</artifactId>\n"
                        + "                <version>3.0</version>\n"
                        + "                <repositoryId>releases</repositoryId>\n"
                        + "                <locallyAvailable>false</locallyAvailable>\n"
                        + "                <artifactTimestamp>0</artifactTimestamp>\n"
                        + "                <extension>jar</extension>\n"
                        + "                <packaging>jar</packaging>\n"
                        + "                <artifactUri>http://localhost:8081/nexus/service/local/artifact/maven/redirect?r=releases&amp;g=software.wings.nexus&amp;a=rest-client&amp;v=3.0&amp;p=jar</artifactUri>\n"
                        + "                <pomUri>http://localhost:8081/nexus/service/local/artifact/maven/redirect?r=releases&amp;g=software.wings.nexus&amp;a=rest-client&amp;v=3.0&amp;p=pom</pomUri>\n"
                        + "              </indexBrowserTreeNode>\n"
                        + "            </children>\n"
                        + "            <groupId>software.wings.nexus</groupId>\n"
                        + "            <artifactId>rest-client</artifactId>\n"
                        + "            <version>3.0</version>\n"
                        + "            <repositoryId>releases</repositoryId>\n"
                        + "            <locallyAvailable>false</locallyAvailable>\n"
                        + "            <artifactTimestamp>0</artifactTimestamp>\n"
                        + "          </indexBrowserTreeNode>\n"
                        + "        </children>\n"
                        + "        <groupId>software.wings.nexus</groupId>\n"
                        + "        <artifactId>rest-client</artifactId>\n"
                        + "        <repositoryId>releases</repositoryId>\n"
                        + "        <locallyAvailable>false</locallyAvailable>\n"
                        + "        <artifactTimestamp>0</artifactTimestamp>\n"
                        + "      </indexBrowserTreeNode>\n"
                        + "    </children>\n"
                        + "    <repositoryId>releases</repositoryId>\n"
                        + "    <locallyAvailable>false</locallyAvailable>\n"
                        + "    <artifactTimestamp>0</artifactTimestamp>\n"
                        + "  </data>\n"
                        + "</indexBrowserTreeViewResponse>")
                    .withHeader("Content-Type", "application/xml")));

    wireMockRule.stubFor(get(
        urlEqualTo(
            "/nexus/service/local/repositories/releases/index_content/software/wings/nexus/rest-client/3.0/rest-client-3.0.jar"))
                             .willReturn(aResponse().withBody(new byte[] {1, 2, 3, 4})));
    // TODO: Need to mock the file input stream
    ArtifactStreamAttributes artifactStreamAttributes = ArtifactStreamAttributes.builder()
                                                            .repositoryName("releases")
                                                            .groupId("software.wings.nexus")
                                                            .artifactName("rest-client")
                                                            .build();
    Map<String, String> artifactMetadata = new HashMap<>();
    artifactMetadata.put(ArtifactMetadataKeys.buildNo, "LATEST");
    Pair<String, InputStream> fileInfo = nexusService.downloadArtifacts(
        nexusConfig, null, artifactStreamAttributes, artifactMetadata, null, null, null, null);

    assertThat(fileInfo).isNotNull();
    assertThat(fileInfo.getKey()).isEqualTo("rest-client-3.0.jar");
  }

  @Test
  @Category(UnitTests.class)
  public void shouldGetLatestVersion() {
    setPomModelWireMock();
    BuildDetails buildDetails =
        nexusService.getLatestVersion(nexusConfig, null, "releases", "software.wings.nexus", "rest-client");
    assertThat(buildDetails).extracting(BuildDetails::getNumber).isEqualTo("3.0");
  }

  @Test
  @Category(UnitTests.class)
  public void shouldGetDockerRepositories() {
    assertThat(nexusService.getRepositories(nexusThreeConfig, null, RepositoryFormat.docker.name()))
        .hasSize(3)
        .containsEntry("docker-group", "docker-group");
  }

  @Test
  @Category(UnitTests.class)
  public void shouldDockerImages() {
    assertThat(nexusService.getGroupIdPaths(nexusThreeConfig, null, "docker-group", RepositoryFormat.docker.name()))
        .hasSize(1)
        .contains("wingsplugins/todolist");
  }

  @Test
  @Category(UnitTests.class)
  public void shouldDockerTags() {
    assertThat(nexusService.getBuilds(nexusThreeConfig, null,
                   ArtifactStreamAttributes.builder()
                       .artifactStreamType(ArtifactStreamType.NEXUS.name())
                       .metadataOnly(true)
                       .jobName("docker-group")
                       .imageName("wingsplugins/todolist")
                       .nexusDockerPort("5000")
                       .repositoryType("docker")
                       .build(),
                   10))
        .hasSize(3)
        .extracting(buildDetails -> buildDetails.getNumber())
        .contains("latest");
  }

  private void setPomModelWireMock() {
    // First return the POM Model
    wireMockRule.stubFor(
        get(urlEqualTo("/nexus/service/local/artifact/maven?r=releases&g=software.wings.nexus&a=rest-client&v=LATEST"))
            .willReturn(aResponse()
                            .withStatus(200)
                            .withBody("<project>\n"
                                + "  <modelVersion>4.0.0</modelVersion>\n"
                                + "  <groupId>software.wings.nexus</groupId>\n"
                                + "  <artifactId>rest-client</artifactId>\n"
                                + "  <version>3.0</version>\n"
                                + "  <packaging>jar</packaging>\n"
                                + "  <description>POM was created by Sonatype Nexus</description>\n"
                                + "</project>")
                            .withHeader("Content-Type", "application/xml")));
  }

  @Test
  @Category(UnitTests.class)
  public void testExistsVersionWithExtensionNexus2x() {
    mockResponsesNexus2xForExistVersions();
    assertThat(nexusService.existsVersion(nexusConfig, null, "releases", "io.harness.test", "demo", "jar", null))
        .isTrue();
  }

  @Test(expected = ArtifactServerException.class)
  @Category(UnitTests.class)
  public void testNoVersionsFoundWithInvalidClassifierNexus2x() {
    mockResponsesNexus2xForExistVersions();
    nexusService.existsVersion(nexusConfig, null, "releases", "io.harness.test", "demo", null, "sources");
  }

  @Test
  @Category(UnitTests.class)
  public void testVersionExistsWithValidClassifierExtensionNexus2x() {
    mockResponsesNexus2xForExistVersions();
    assertThat(nexusService.existsVersion(nexusConfig, null, "releases", "io.harness.test", "demo", "jar", "binary"))
        .isTrue();
  }

  private void mockResponsesNexus2xForExistVersions() {
    wireMockRule.stubFor(
        get(urlEqualTo("/nexus/service/local/repositories/releases/index_content/io/harness/test/demo/"))
            .willReturn(aResponse().withStatus(200).withBody(XML_RESPONSE_INDEX_TREE_BROWSER)));

    wireMockRule.stubFor(
        get(urlMatching("/nexus/service/local/repositories/releases/content/io%2Fharness%2Ftest%2Fdemo%2F1.0%2F"))
            .willReturn(aResponse()
                            .withStatus(200)
                            .withBody(XML_CONTENT_RESPONSE_2)
                            .withHeader("Content-Type", "application/xml")));
    wireMockRule.stubFor(
        get(urlEqualTo("/nexus/service/local/repositories/releases/content/io%2Fharness%2Ftest%2Fdemo%2F2.0%2F"))
            .willReturn(aResponse()
                            .withStatus(200)
                            .withBody(XML_CONTENT_RESPONSE_1)
                            .withHeader("Content-Type", "application/xml")));
  }
  @Test
  @Category(UnitTests.class)
  public void testExistsVersionWithExtensionNexus3x() {
    assertThat(
        nexusService.existsVersion(nexusThreeConfig, null, "maven-releases", "mygroup", "myartifact", "jar", "binary"))
        .isTrue();
  }

  @Test(expected = ArtifactServerException.class)
  @Category(UnitTests.class)
  public void testNoVersionsFoundWithInvalidClassifierNexus3x() {
    nexusService.existsVersion(nexusThreeConfig, null, "maven-releases", "mygroup", "myartifact", null, "source");
  }
}
