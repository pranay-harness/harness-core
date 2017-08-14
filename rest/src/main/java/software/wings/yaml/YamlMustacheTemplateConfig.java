package software.wings.yaml;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.DumperOptions;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.Yaml;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.RestResponse;
import software.wings.beans.Service;
import software.wings.utils.ArtifactType;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by bsollish on 8/11/17
 */
public class YamlMustacheTemplateConfig {
  public List<Application> applications() {
    List<Application> applications = new ArrayList<>();

    Application app1 = new Application();
    app1.setName("TestApp1");
    app1.setDescription("blah app1 blah");

    Service service1 = new Service();
    service1.setName("TestService1");
    service1.setArtifactType(ArtifactType.WAR);
    service1.setDescription("blah service1 blah");

    Service service2 = new Service();
    service2.setName("TestService2");
    service2.setArtifactType(ArtifactType.WAR);
    service2.setDescription("blah service2 blah");

    List<Service> services1 = app1.getServices();
    services1.add(service1);
    services1.add(service2);
    app1.setServices(services1);

    Environment env1 = new Environment();
    env1.setName("Development");
    env1.setEnvironmentType(EnvironmentType.NON_PROD);

    Environment env2 = new Environment();
    env2.setName("Quality Assurance");
    env2.setEnvironmentType(EnvironmentType.NON_PROD);

    Environment env3 = new Environment();
    env3.setName("Production");
    env3.setEnvironmentType(EnvironmentType.PROD);

    List<Environment> environments1 = app1.getEnvironments();
    environments1.add(env1);
    environments1.add(env2);
    environments1.add(env3);
    app1.setEnvironments(environments1);

    applications.add(app1);

    Application app2 = new Application();
    app2.setName("TestApp2");
    app2.setDescription("blah app2 blah");

    Service service3 = new Service();
    service3.setName("TestService3");
    service3.setArtifactType(ArtifactType.WAR);
    service3.setDescription("blah service3 blah");

    Service service4 = new Service();
    service4.setName("TestService4");
    service4.setArtifactType(ArtifactType.WAR);
    service4.setDescription("blah service4 blah");

    List<Service> services2 = app2.getServices();
    services2.add(service3);
    services2.add(service4);
    app2.setServices(services2);

    List<Environment> environments2 = app2.getEnvironments();
    environments2.add(env1);
    environments2.add(env2);
    environments2.add(env3);
    app2.setEnvironments(environments2);

    applications.add(app2);

    return applications;
  }

  public static void main(String[] args) throws IOException {
    MustacheFactory mf = new DefaultMustacheFactory();

    Mustache mustache = mf.compile("rest/src/main/resources/templates/mustacheyaml/config_with_partials.mustache");

    Writer writer = new StringWriter();
    mustache.execute(writer, new YamlMustacheTemplateConfig()).flush();
    String configYaml = "---\n" + writer.toString();

    System.out.println(configYaml);

    //---------------------------
    /*
    Yaml yamlObj = new Yaml();

    try {
      // NOTE: we don't do anything with the Yaml Object
      yamlObj.load(configYaml);

      System.out.println(yamlObj);
      System.out.println(Yaml.dump(yamlObj));

    } catch (Exception e) {
      e.printStackTrace();
    }
    */
    //---------------------------

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    Config config = null;

    if (configYaml != null && !configYaml.isEmpty()) {
      try {
        config = mapper.readValue(configYaml, Config.class);

        //-------
        System.out.println("************************");

        DumperOptions dumpOpts = new DumperOptions();
        dumpOpts.setPrettyFlow(true);
        Yaml yaml = new Yaml(new YamlRepresenter(), dumpOpts);
        String yamlFromConfig = yaml.dump(config);

        System.out.println(yamlFromConfig);
        //-------

      } catch (Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }

    System.out.println("************ Roundtrip ************");

    if (config != null) {
      mustache.execute(writer, config).flush();
      configYaml = "---\n" + writer.toString();

      System.out.println(configYaml);
    }
  }
}
