package software.wings.yaml;

import static org.mockito.Mockito.mock;

import com.esotericsoftware.yamlbeans.YamlConfig;
import com.esotericsoftware.yamlbeans.YamlWriter;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.beans.RestResponse;
import software.wings.service.intfc.yaml.YamlGitService;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.util.Arrays;

public class YamlOrderAndIndentTest {
  private static final YamlGitService yamlGitSyncService = mock(YamlGitService.class);

  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Test
  public void testYamlOrder() {
    logger.info("***** testYamlOrder *****");

    /*
    AppYaml appYaml = new AppYaml();
    appYaml.setName("testApp1");
    appYaml.setDescription("This is testApp1");
    appYaml.setServices(Arrays.asList("testService3", "testService2", "testService1"));

    RestResponse<YamlPayload> rr = YamlHelper.getYamlRestResponse(appYaml, "testPayload");
    */

    OrderIndentTestYaml oitYaml = new OrderIndentTestYaml();

    oitYaml.setName("OrderIndentTest");
    oitYaml.setDescription("This is the OrderIndentTest");

    Application.Yaml appYaml1 = new Application.Yaml();
    //    appYaml1.setName("testApp1");
    appYaml1.setDescription("This is testApp1");

    Application.Yaml appYaml2 = new Application.Yaml();
    //    appYaml2.setName("testApp2");
    appYaml2.setDescription("This is testApp2");

    Application.Yaml appYaml3 = new Application.Yaml();
    //    appYaml3.setName("testApp3");
    appYaml3.setDescription("This is testApp3");

    oitYaml.setApplications(Arrays.asList(appYaml1, appYaml3, appYaml2));

    RestResponse<YamlPayload> rr =
        YamlHelper.getYamlRestResponse(yamlGitSyncService, null, null, oitYaml, "testPayload");

    logger.info("\n" + rr.getResource().getYaml());

    //----------------------------------------
    // YamlBeans version:

    try {
      Writer fileWriter = new FileWriter("dummy.yaml");
      Writer stringWriter = new StringWriter();
      YamlConfig yamlConfig = new YamlConfig();

      // yamlConfig.setClassTag("", AppYaml.class);

      yamlConfig.setPropertyElementType(OrderIndentTestYaml.class, "applications", Application.Yaml.class);
      // yamlConfig.setPropertyElementType(AppYaml.class, "services", String.class);

      /*
      yamlConfig.writeConfig.setIndentSize(4);
      yamlConfig.writeConfig.setCanonical(false);
      */

      // yamlConfig.writeConfig.setExplicitFirstDocument(false);
      // yamlConfig.writeConfig.setAlwaysWriteClassname(false);
      // yamlConfig.writeConfig.setUseVerbatimTags(false);
      // yamlConfig.writeConfig.setWriteRootElementTags(false);
      yamlConfig.writeConfig.setWriteRootTags(false);
      // yamlConfig.writeConfig.setWriteRootElementTags(false);

      yamlConfig.writeConfig.setIndentSize(2);

      // with this the empty services list gets output:
      yamlConfig.writeConfig.setWriteDefaultValues(true);

      // yamlConfig.writeConfig.setExplicitEndDocument(true);

      YamlWriter writer = new YamlWriter(stringWriter, yamlConfig);
      writer.write(oitYaml);
      writer.close();

      // NOTE: you must do writer.close() before you can get the string from stringWriter!

      String str = stringWriter.toString();

      // logger.info("BEFORE:\n" + str);

      str = processYamlContent(str);

      // logger.info("AFTER:\n" + str);

    } catch (Exception e) {
      e.printStackTrace();
    }

    Field[] fields = OrderIndentTestYaml.class.getDeclaredFields();

    for (Field f : fields) {
      // logger.info(f.getName());
    }

    // assertThat("fish").isEqualTo("fish");
  }

  private String processYamlContent(String content) {
    StringBuilder sb = new StringBuilder();

    BufferedReader bufReader = new BufferedReader(new StringReader(content));

    String line = null;

    try {
      while ((line = bufReader.readLine()) != null) {
        int index = line.indexOf('!');

        if (index != -1) {
          line = line.substring(0, index);
        }

        //------------
        /*
        // does line start with dash
        String trimmedLine = line.trim();
        index = trimmedLine.indexOf("-");

        if (index == 0) {
          line = "  " + line;
        }
        */
        //------------

        sb.append(line + "\n");
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    return sb.toString();
  }
}
