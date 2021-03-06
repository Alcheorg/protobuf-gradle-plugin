import org.apache.commons.io.FileUtils

/**
 * Utility class.
 */
class ProtobufPluginTestHelper {

  static void appendPluginClasspath(File buildFile) {
    URL pluginClasspathResource =
        ProtobufPluginTestHelper.classLoader.findResource("plugin-classpath.txt")
    if (pluginClasspathResource == null) {
      throw new IllegalStateException('Did not find plugin classpath resource, ' +
          'run `testClasses` build task.')
    }

    String pluginClasspath = pluginClasspathResource.readLines()
      .collect { it.replace('\\', '\\\\') } // escape backslashes in Windows paths
      .collect { "'$it'" }
      .join(", ")

    // Add the logic under test to the test build
    buildFile << """
        buildscript {
            dependencies {
                classpath files($pluginClasspath)
            }
        }
    """
  }

  /**
   * Creates a temp test dir with name {@code testProjectName}, which is generated by
   * copying a list of overlay dirs on top of it.
   */
  static File createTestProject(String testProjectName, String... sourceDirs) {
    File dir = new File(System.getProperty('user.dir'), 'build/tests/' + testProjectName)
    FileUtils.deleteDirectory(dir)
    FileUtils.forceMkdir(dir)
    sourceDirs.each {
      FileUtils.copyDirectory(new File(System.getProperty("user.dir"), it), dir)
    }
    File buildFile = new File(dir.path, "build.gradle")
    appendPluginClasspath(buildFile)
    return dir
  }

  /**
   * Initializes {@code projectDir} by copying a list of subprojects into it.
   */
  static void initializeSubProjects(File projectDir, File... subProjects) {
    File settingsFile = new File(projectDir, 'settings.gradle')
    settingsFile.createNewFile()

    subProjects.each {
      File subProjectDir = new File(projectDir.path, it.name)
      FileUtils.copyDirectory(it, subProjectDir)

      settingsFile << """
          include ':$it.name'
          project(':$it.name').projectDir = "\$rootDir/$it.name" as File
      """
    }
    File buildFile = new File(projectDir.path, "build.gradle")
    buildFile.createNewFile()
    appendPluginClasspath(buildFile)
  }
}
