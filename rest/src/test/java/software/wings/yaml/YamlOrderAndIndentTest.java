package software.wings.yaml;

import com.esotericsoftware.yamlbeans.YamlConfig;
import com.esotericsoftware.yamlbeans.YamlWriter;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.RestResponse;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;

public class YamlOrderAndIndentTest {
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

    AppYaml appYaml1 = new AppYaml();
    appYaml1.setName("testApp1");
    appYaml1.setDescription("This is testApp1");
    appYaml1.setServices(Arrays.asList("testServiceA", "testServiceC", "testServiceB"));

    AppYaml appYaml2 = new AppYaml();
    appYaml2.setName("testApp2");
    appYaml2.setDescription("This is testApp2");
    appYaml2.setServices(Arrays.asList());

    AppYaml appYaml3 = new AppYaml();
    appYaml3.setName("testApp3");
    appYaml3.setDescription("This is testApp3");
    appYaml3.setServices(Arrays.asList("testService3", "testService1", "testService2"));

    oitYaml.setApplications(Arrays.asList(appYaml1, appYaml3, appYaml2));

    RestResponse<YamlPayload> rr = YamlHelper.getYamlRestResponse(oitYaml, "testPayload");

    logger.info("\n" + rr.getResource().getYaml());

    //----------------------------------------
    // YamlBeans version:

    try {
      Writer fileWriter = new FileWriter("dummy.yaml");
      Writer stringWriter = new StringWriter();
      YamlConfig yamlConfig = new YamlConfig();

      // yamlConfig.setClassTag("", AppYaml.class);

      yamlConfig.setPropertyElementType(OrderIndentTestYaml.class, "applications", AppYaml.class);
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

      YamlWriter writer = new YamlWriter(stringWriter, yamlConfig);
      writer.write(oitYaml);
      writer.close();

      // NOTE: you must do writer.close() before you can get the string from stringWriter!

      String str = stringWriter.toString();

      logger.info("BEFORE:\n" + str);

      str = processYamlContent(str);

      logger.info("AFTER:\n" + str);

    } catch (Exception e) {
      e.printStackTrace();
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
