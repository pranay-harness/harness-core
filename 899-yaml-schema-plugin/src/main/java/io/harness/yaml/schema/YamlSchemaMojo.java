package io.harness.yaml.schema;

import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.yaml.YamlSdkInitConstants;
import io.harness.yaml.schema.beans.YamlSchemaConfiguration;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import lombok.SneakyThrows;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

@Mojo(name = "generate", defaultPhase = LifecyclePhase.COMPILE, requiresDependencyResolution = ResolutionScope.COMPILE,
    requiresDependencyCollection = ResolutionScope.COMPILE)

public class YamlSchemaMojo extends AbstractMojo {
  /**
   * The base path where the schema will be stored in resources.
   */
  public String generationFolder = YamlSdkInitConstants.schemaBasePath;

  @Parameter List<String> rootClasses;
  /**
   * The Maven project.
   */
  @Parameter(defaultValue = "${project}", readonly = true) private MavenProject project;

  /**
   * The classloader used for loading generator modules and classes.
   */
  private URLClassLoader classLoader;

  /**
   * Invoke the schema generator.
   *
   * @throws MojoExecutionException An exception in case of errors and unexpected behavior
   */
  @SneakyThrows
  public void execute() throws MojoExecutionException {
    if (IsRunningOnJenkins()) {
      return;
    }
    classLoader = getClassLoader();

    Thread.currentThread().setContextClassLoader(classLoader);
    JacksonClassHelper jacksonClassHelper = new JacksonClassHelper();
    SwaggerGenerator swaggerGenerator = new SwaggerGenerator();
    YamlSchemaGenerator generator = new YamlSchemaGenerator(jacksonClassHelper, swaggerGenerator);
    YamlSchemaConfiguration yamlSchemaConfiguration =
        YamlSchemaConfiguration.builder().generatedPathRoot(generationFolder).classLoader(classLoader).build();
    rootClasses.forEach(clazz -> {
      try {
        final Class<?> currentClazz = classLoader.loadClass(clazz);
        generator.generateJsonSchemaForRootClass(yamlSchemaConfiguration, swaggerGenerator, currentClazz);
      } catch (ClassNotFoundException e) {
        throw new InvalidRequestException("class " + clazz + "not found");
      }
    });
  }

  private boolean IsRunningOnJenkins() {
    final String platform = System.getProperty("PLATFORM");
    return EmptyPredicate.isNotEmpty(platform) && platform.equals("jenkins");
  }

  /**
   * Construct the classloader based on the project classpath.
   *
   * @return The classloader
   */
  private URLClassLoader getClassLoader() throws MojoExecutionException {
    if (this.classLoader == null) {
      List<String> runtimeClasspathElements = null;
      try {
        runtimeClasspathElements = project.getRuntimeClasspathElements();

      } catch (DependencyResolutionRequiredException e) {
        this.getLog().error("Failed to resolve runtime classpath elements", e);
      }
      if (runtimeClasspathElements == null) {
        throw new MojoExecutionException("No class loader found");
      }

      URL[] runtimeUrls = new URL[runtimeClasspathElements.size()];
      for (int i = 0; i < runtimeClasspathElements.size(); i++) {
        String element = runtimeClasspathElements.get(i);
        try {
          runtimeUrls[i] = new File(element).toURI().toURL();
        } catch (MalformedURLException e) {
          this.getLog().error("Failed to resolve runtime classpath element", e);
        }
      }
      this.classLoader = new URLClassLoader(runtimeUrls, Thread.currentThread().getContextClassLoader());
    }

    return this.classLoader;
  }
}
