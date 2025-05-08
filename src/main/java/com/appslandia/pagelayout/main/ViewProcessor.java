// The MIT License (MIT)
// Copyright © 2015 Loc Ha

// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:

// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.

// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

package com.appslandia.pagelayout.main;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import com.appslandia.pagelayout.utils.Arguments;
import com.appslandia.pagelayout.utils.FileNameUtils;
import com.appslandia.pagelayout.utils.FileUtils;
import com.appslandia.pagelayout.utils.ViewSourceUtils;
import com.appslandia.pagelayout.utils.ViewUtils;

/**
 *
 * @author Loc Ha
 *
 */
public class ViewProcessor {

  private String inputViewsDir = "/WEB-INF/__views";
  private String outputViewsDir = "views";
  private String configDir = "__config";

  private String viewSuffixes = ".jsp,.jspx,.xhtml,.peb";
  private boolean removeBlankLines;
  private boolean debugVariables;

  public ViewProcessor inputViewsDir(String inputViewsDir) {
    this.inputViewsDir = FileNameUtils.toUnixPath(inputViewsDir);
    return this;
  }

  public ViewProcessor outputViewsDir(String outputViewsDir) {
    this.outputViewsDir = outputViewsDir;
    return this;
  }

  public ViewProcessor configDir(String configDir) {
    this.configDir = configDir;
    return this;
  }

  public ViewProcessor viewSuffixes(String viewSuffixes) {
    this.viewSuffixes = viewSuffixes;
    return this;
  }

  public ViewProcessor removeBlankLines(boolean removeBlankLines) {
    this.removeBlankLines = removeBlankLines;
    return this;
  }

  public ViewProcessor debugVariables(boolean debugVariables) {
    this.debugVariables = debugVariables;
    return this;
  }

  protected boolean isViewFile(String viewName) {
    var suffixes = this.viewSuffixes.split(",");
    return Arrays.stream(suffixes).anyMatch(suffix -> viewName.endsWith(suffix));
  }

  public void process() throws Exception {
    Arguments.notNull(this.inputViewsDir);
    Arguments.notNull(this.outputViewsDir);
    Arguments.notNull(this.configDir);
    Arguments.notNull(this.viewSuffixes);

    var inputViewsPath = Paths.get(this.inputViewsDir);
    if (!Files.exists(inputViewsPath)) {
      throw new IllegalArgumentException("The path does not exist: " + inputViewsPath.toAbsolutePath());
    }

    var configPath = inputViewsPath.resolve(this.configDir);
    var outViewPath = inputViewsPath.getParent().resolve(this.outputViewsDir);

    if (outViewPath.toFile().exists()) {
      FileUtils.deleteRecursively(outViewPath);
    }
    doProcess(inputViewsPath, configPath, outViewPath);
  }

  protected void doProcess(Path inViewsPath, Path configPath, Path outViewsPath) throws Exception {
    Queue<File> q = new LinkedList<>();
    q.add(inViewsPath.toFile());

    while (!q.isEmpty()) {
      var file = q.remove();

      if (file.equals(configPath.toFile()) || file.equals(outViewsPath.toFile())) {
        continue;
      }
      if (file.isDirectory()) {
        Arrays.stream(file.listFiles()).forEach(f -> q.add(f));
        continue;
      }
      if (!file.isFile()) {
        continue;
      }
      var targetFilePath = outViewsPath.resolve(inViewsPath.relativize(file.toPath()));
      Files.createDirectories(targetFilePath.getParent());

      // View file?
      if (isViewFile(file.getName())) {

        var viewSuffix = ViewUtils.getViewSuffix(file.getName());
        var viewSourceHandler = ViewSourceHandler.getHandler(viewSuffix);

        // View Source
        var model = new ViewModel();
        model.viewName = file.getName();
        model.viewSource = loadSource(file.toPath(), false);

        // Parse Variables (view)
        Map<String, String> viewVariables = new LinkedHashMap<>();
        ViewSourceUtils.parseVariables(model.viewSource, model.viewName, viewVariables);

        // Layout source
        var layoutName = getLayoutName(model.viewName, viewVariables);
        if (layoutName != null) {

          model.layoutViewName = layoutName + viewSuffix;
          model.layoutSource = loadSource(configPath.resolve(model.layoutViewName), true);

          // Parse Variables (layoutViewName)
          ViewSourceUtils.parseVariablesFile(model.layoutSource, configPath, model.mergedVariables);
          ViewSourceUtils.parseVariables(model.layoutSource, model.layoutViewName, model.mergedVariables);
        }

        // Replace variables (view, layout)
        viewVariables.entrySet().stream().forEach(e -> {
          model.mergedVariables.put(e.getKey(), e.getValue());
        });
        if (layoutName != null) {
          ViewSourceUtils.replaceVariables(model.layoutSource, model.mergedVariables);
        }
        ViewSourceUtils.replaceVariables(model.viewSource, model.mergedVariables);

        if (this.debugVariables) {
          model.viewSource.addAll(ViewSourceUtils.toVariableList(model.mergedVariables));
        }

        // Remove blank lines
        if (this.removeBlankLines) {
          if (layoutName != null) {
            ViewSourceUtils.removeBlankLines(model.layoutSource);
          }
          ViewSourceUtils.removeBlankLines(model.viewSource);
        }

        // handleSource (View specific)
        if (layoutName != null) {
          viewSourceHandler.handleSource(model.layoutSource, model.layoutViewName, true);
        }
        viewSourceHandler.handleSource(model.viewSource, model.viewName, false);

        // Parse Sections (view)
        ViewSourceUtils.parseSections(model.viewSource, model.sections, model.viewName);

        // Replace @doBody & sections
        if (layoutName != null) {
          ViewSourceUtils.replaceBody(model.layoutSource, model.layoutViewName, model.viewSource, model.viewName,
              viewSourceHandler);
          ViewSourceUtils.replaceSections(model.layoutSource, model.viewName, model.sections);
        }

        // Save source
        if (layoutName != null) {
          if (viewSourceHandler.incViewFile()) {
            var incViewName = ViewUtils.getInclViewName(model.viewName);
            saveSource(model.viewSource, targetFilePath.getParent().resolve(incViewName));
          }
          saveSource(model.layoutSource, targetFilePath);

        } else {
          // No layout
          saveSource(model.viewSource, targetFilePath);
        }
      } else {
        // Not view file -> Copy directly
        Files.copy(file.toPath(), targetFilePath, StandardCopyOption.REPLACE_EXISTING);
      }
    }
  }

  final Map<String, List<String>> sourceCache = new HashMap<>();

  protected List<String> loadSource(Path sourcePath, boolean cacheSource) throws Exception {
    if (!cacheSource) {
      return Files.readAllLines(sourcePath, StandardCharsets.UTF_8);

    } else {
      // Cache?
      var lines = sourceCache.computeIfAbsent(sourcePath.getFileName().toString(), k -> {
        try {
          return Files.readAllLines(sourcePath, StandardCharsets.UTF_8);
        } catch (IOException ex) {
          throw new UncheckedIOException(ex);
        }
      });
      return new ArrayList<>(lines);
    }
  }

  static void saveSource(List<String> source, Path outFile) throws Exception {
    try (var out = Files.newBufferedWriter(outFile, StandardCharsets.UTF_8)) {
      for (var i = 0; i < source.size(); i++) {
        if (i > 0) {
          out.newLine();
        }
        out.write(source.get(i));
      }
    }
  }

  static String getLayoutName(String viewName, Map<String, String> variables) {
    var layoutName = variables.get("__layout");
    if (layoutName == null) {
      return null;
    }
    if (layoutName.isEmpty()) {
      throw new IllegalArgumentException("__layout is required (viewName=" + viewName + ")");
    }
    return layoutName;
  }

  static class ViewModel {
    String viewName;
    List<String> viewSource;

    final Map<String, String> mergedVariables = new LinkedHashMap<>();
    final Map<String, List<String>> sections = new LinkedHashMap<>();

    String layoutViewName;
    List<String> layoutSource;
  }
}
