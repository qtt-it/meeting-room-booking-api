package com.dki.meeting;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class DocumentationTest {
  private static final Pattern LANGUAGE_MARKERS =
      Pattern.compile("[aAeEiIoOuUyY]\\p{M}+|[\\u0110\\u0111]");
  private static final Pattern LINK = Pattern.compile("\\[[^\\]]+\\]\\(([^)]+)\\)");

  @Test
  void documentationUsesEnglish() throws IOException {
    var failures = new ArrayList<String>();
    for (Path file : markdownFiles()) {
      if (hasLanguageMarkers(Files.readString(file)))
        failures.add(root().relativize(file).toString());
    }
    assertThat(failures).as("Documents contain non-English language markers").isEmpty();
  }

  @Test
  void languageGuardRecognizesComposedAndDecomposedText() {
    String sample = "\u0110\u1eb7t ph\u00f2ng";
    assertThat(hasLanguageMarkers(sample)).isTrue();
    assertThat(hasLanguageMarkers(Normalizer.normalize(sample, Normalizer.Form.NFD))).isTrue();
    assertThat(hasLanguageMarkers("Approved bookings cannot overlap.")).isFalse();
  }

  @Test
  void localDocumentationLinksResolve() throws IOException {
    var failures = new ArrayList<String>();
    for (Path file : markdownFiles()) {
      var matcher = LINK.matcher(Files.readString(file));
      while (matcher.find()) {
        String target = matcher.group(1);
        if (target.matches("^(https?:|mailto:|#).*")) continue;
        String local = target.split("#", 2)[0];
        if (!local.isBlank() && !Files.exists(file.getParent().resolve(local)))
          failures.add(root().relativize(file) + ": " + local);
      }
    }
    assertThat(failures).as("Broken local documentation links").isEmpty();
  }

  private boolean hasLanguageMarkers(String value) {
    return LANGUAGE_MARKERS.matcher(Normalizer.normalize(value, Normalizer.Form.NFD)).find();
  }

  private Path root() {
    Path path = Path.of("").toAbsolutePath();
    while (path != null) {
      if (Files.isDirectory(path.resolve("docs")) && Files.isDirectory(path.resolve("libs")))
        return path;
      path = path.getParent();
    }
    throw new IllegalStateException("Cannot locate the repository root");
  }

  private List<Path> markdownFiles() throws IOException {
    var files = new ArrayList<Path>();
    Files.walkFileTree(
        root(),
        new SimpleFileVisitor<Path>() {
          @Override
          public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            return Set.of(".git", "target", "node_modules", ".local")
                    .contains(dir.getFileName().toString())
                ? FileVisitResult.SKIP_SUBTREE
                : FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            if (file.toString().endsWith(".md")) files.add(file);
            return FileVisitResult.CONTINUE;
          }
        });
    return files;
  }
}
