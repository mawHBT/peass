package de.dagere.peass.dependency.execution;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.dependency.execution.gradle.AndroidVersionUtil;
import de.dagere.peass.dependency.execution.gradle.FindDependencyVisitor;

public class GradleParseUtil {

   private static final Logger LOG = LogManager.getLogger(GradleParseUtil.class);

   public static void writeInitGradle(final File init) {
      if (!init.exists()) {
         try (FileWriter fw = new FileWriter(init)) {
            final PrintWriter pw = new PrintWriter(fw);
            pw.write("allprojects{");
            pw.write(" repositories {");
            pw.write("  mavenLocal();");
            pw.write("  maven { url 'https://maven.google.com' };");
            fw.write(" }");
            fw.write("}");
            pw.flush();
         } catch (final IOException e) {
            e.printStackTrace();
         }
      }
   }

   public static FindDependencyVisitor setAndroidTools(final File buildfile) {
      FindDependencyVisitor visitor = null;
      try {
         LOG.debug("Editing: {}", buildfile);

         visitor = parseBuildfile(buildfile);
         final List<String> gradleFileContents = Files.readAllLines(Paths.get(buildfile.toURI()));

         if (visitor.getBuildTools() != -1) {
            updateBuildTools(visitor);
         }

         if (visitor.getBuildToolsVersion() != -1) {
            updateBuildToolsVersion(visitor);
         }

         Files.write(buildfile.toPath(), gradleFileContents, StandardCharsets.UTF_8);
      } catch (final IOException e) {
         e.printStackTrace();
      }
      return visitor;
   }

   public static FindDependencyVisitor parseBuildfile(final File buildfile) throws IOException, FileNotFoundException {
      FindDependencyVisitor visitor = new FindDependencyVisitor(buildfile);
      return visitor;

   }

   public static void updateBuildTools(final FindDependencyVisitor visitor) {
      final int lineIndex = visitor.getBuildTools() - 1;
      final String versionLine = visitor.getLines().get(lineIndex).trim().replaceAll("'", "").replace("\"", "");
      final String versionString = versionLine.split(":")[1].trim();
      if (AndroidVersionUtil.isLegalBuildTools(versionString)) {
         final String runningVersion = AndroidVersionUtil.getRunningVersion(versionString);
         if (runningVersion != null) {
            visitor.getLines().set(lineIndex, "'buildTools': '" + runningVersion + "'");
         } else {
            visitor.setHasVersion(false);
         }
      }
   }

   public static void updateBuildToolsVersion(final FindDependencyVisitor visitor) {
      final int lineIndex = visitor.getBuildToolsVersion() - 1;
      final String versionLine = visitor.getLines().get(lineIndex).trim().replaceAll("'", "").replace("\"", "");
      final String versionString = versionLine.split(" ")[1].trim();
      if (AndroidVersionUtil.isLegalBuildToolsVersion(versionString)) {
         LOG.info(lineIndex + " " + versionLine);
         final String runningVersion = AndroidVersionUtil.getRunningVersion(versionString);
         if (runningVersion != null) {
            visitor.getLines().set(lineIndex, "buildToolsVersion " + runningVersion);
         } else {
            visitor.setHasVersion(false);
         }
      }
   }

   public static ProjectModules getModules(final File projectFolder) {
      final File settingsFile = new File(projectFolder, "settings.gradle");
      final List<File> modules = new LinkedList<>();
      if (settingsFile.exists()) {
         try (BufferedReader reader = new BufferedReader(new FileReader(settingsFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
               parseModuleLine(projectFolder, modules, line);
            }
         } catch (IOException e) {
            throw new RuntimeException(e);
         }
      } else {
         LOG.debug("settings-file {} not found", settingsFile);
      }
      modules.add(projectFolder);
      return new ProjectModules(modules);
   }

   private static void parseModuleLine(final File projectFolder, final List<File> modules, final String line) {
      final String[] splitted = line.replaceAll("[ ,]+", " ").split(" ");
      if (splitted[0].equals("include")) {
         for (int candidateIndex = 1; candidateIndex < splitted.length; candidateIndex++) {
            final String candidate = splitted[candidateIndex].substring(1, splitted[candidateIndex].length() - 1);
            final File module = new File(projectFolder, candidate.replace(':', File.separatorChar));
            if (module.exists()) {
               modules.add(module);
            } else {
               LOG.error(line + " not found! Was looking in " + module.getAbsolutePath());
            }
         }
      }
   }

   public static void addJUnitVersionSpringBoot(final FindDependencyVisitor visitor) {
      visitor.getLines().add("ext['junit-jupiter.version']='5.8.1'");
   }
}
