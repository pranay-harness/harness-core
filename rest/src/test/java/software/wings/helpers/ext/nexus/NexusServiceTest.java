package software.wings.helpers.ext.nexus;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import software.wings.beans.BambooConfig;
import software.wings.beans.config.NexusConfig;
import software.wings.exception.WingsException;

/**
 * Created by srinivas on 3/30/17.
 */
public class NexusServiceTest {
  /**
   * The Wire mock rule.
   */
  @Rule public WireMockRule wireMockRule = new WireMockRule(8881);

  private static final String DEFAULT_NEXUS_URL = "http://localhost:8881/nexus/";

  private NexusService nexusService = new NexusServiceImpl();

  private NexusConfig nexusConfig = NexusConfig.Builder.aNexusConfig()
                                        .withNexusUrl(DEFAULT_NEXUS_URL)
                                        .withUsername("admin")
                                        .withPassword("admin123")
                                        .build();

  @Test
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
    assertThat(nexusService.getRepositories(nexusConfig)).hasSize(1).containsEntry("snapshots", "Snapshots");
  }

  @Test
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
    assertThat(nexusService.getArtifactPaths(nexusConfig, "releases")).hasSize(2).contains("/fakepath/");
  }

  @Test
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
    assertThat(nexusService.getArtifactPaths(nexusConfig, "releases", "fakepath"))
        .hasSize(1)
        .contains("/fakepath/nexus-client-core/");
  }

  @Test
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
    assertThat(nexusService.getArtifactPaths(nexusConfig, "releases", "/fakepath"))
        .hasSize(1)
        .contains("/fakepath/nexus-client-core/");
  }

  @Test
  public void shouldGetRepositoriesError() {
    wireMockRule.stubFor(get(urlEqualTo("/nexus/service/local/repositories"))
                             .willReturn(aResponse()
                                             .withStatus(200)
                                             .withFault(Fault.MALFORMED_RESPONSE_CHUNK)
                                             .withHeader("Content-Type", "application/xml")));
    assertThatThrownBy(() -> nexusService.getRepositories(nexusConfig))
        .isInstanceOf(WingsException.class)
        .hasMessageContaining("Expected leading");
  }

  @Test
  public void shouldGetArtifactPathsByRepoError() {
    wireMockRule.stubFor(get(urlEqualTo("/nexus/service/local/repositories/releases/content/"))
                             .willReturn(aResponse()
                                             .withStatus(200)
                                             .withFault(Fault.EMPTY_RESPONSE)
                                             .withHeader("Content-Type", "application/xml")));

    assertThatThrownBy(() -> nexusService.getArtifactPaths(nexusConfig, "releases"))
        .isInstanceOf(WingsException.class)
        .hasMessageContaining("unexpected end of stream");
  }

  @Test
  public void shouldGetArtifactPathsByRepoStartsWithUrlError() {
    wireMockRule.stubFor(get(urlEqualTo("/nexus/service/local/repositories/releases/content/fakepath"))
                             .willReturn(aResponse()
                                             .withStatus(200)
                                             .withFault(Fault.RANDOM_DATA_THEN_CLOSE)
                                             .withHeader("Content-Type", "application/xml")));

    assertThatThrownBy(() -> nexusService.getArtifactPaths(nexusConfig, "releases", "fakepath"))
        .isInstanceOf(WingsException.class)
        .hasMessageContaining("unexpected end of stream");
  }

  @Test
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

    assertThat(nexusService.getGroupIdPaths(nexusConfig, "releases")).hasSize(1).contains("/fakepath/");
  }

  @Test
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

    assertThat(nexusService.getArtifactNames(nexusConfig, "releases", "/software/wings/nexus/"))
        .hasSize(1)
        .contains("rest-client");
  }
}
