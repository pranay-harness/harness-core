package io.harness.yaml.schema;

import io.harness.yaml.schema.beans.YamlSchemaConfiguration;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.reflections.Reflections;

@Mojo(name = "generate", defaultPhase = LifecyclePhase.COMPILE, requiresDependencyResolution = ResolutionScope.COMPILE,
    requiresDependencyCollection = ResolutionScope.COMPILE)

public class YamlSchemaMojo extends AbstractMojo {
  /**
   * The base path where the schema will be stored in resources.
   */
  @Parameter(defaultValue = "schema") public String generationFolder;

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
  public void execute() throws MojoExecutionException {
    classLoader = getClassLoader();

    getLog().info(Reflections.log.getName());
    Logger.getLogger(Reflections.log.getName()).setLevel(Level.OFF);
    YamlSchemaGenerator generator = new YamlSchemaGenerator();
    String path = "src" + File.separator + "main" + File.separator + "resources" + File.separator + generationFolder;
    YamlSchemaConfiguration yamlSchemaConfiguration =
        YamlSchemaConfiguration.builder().generatedPathRoot(path).classLoader(classLoader).build();

    generator.generateYamlSchemaFiles(yamlSchemaConfiguration);
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
