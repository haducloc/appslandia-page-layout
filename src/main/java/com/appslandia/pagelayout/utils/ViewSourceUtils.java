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

package com.appslandia.pagelayout.utils;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.appslandia.pagelayout.main.ViewSourceHandler;

/**
 *
 * @author Loc Ha
 *
 */
public class ViewSourceUtils {

  public static String copyIndent(String source) {
    var end = -1;
    while ((++end < source.length()) && Character.isWhitespace(source.charAt(end))) {
    }
    return source.substring(0, end);
  }

  public static List<String> copySubSource(List<String> source, int start, int end) {
    List<String> list = new ArrayList<>();
    for (var i = start; i <= end; i++) {
      list.add(source.get(i));
    }
    return list;
  }

  public static void removeSubSource(List<String> source, int start, int end) {
    for (var i = end; i >= start; i--) {
      source.remove(i);
    }
  }

  public static void removeSubSource(List<String> source, int pos) {
    while (pos < source.size()) {
      source.remove(pos);
    }
  }

  static final Pattern blankLinePattern = Pattern.compile("\\s*");

  public static void removeBlankLines(List<String> source) {
    for (var i = source.size() - 1; i >= 0; i--) {
      if (blankLinePattern.matcher(source.get(i)).matches()) {
        source.remove(i);
      }
    }
  }

  public static void replaceVariables(List<String> source, Map<String, String> variables) {
    for (var i = 0; i < source.size(); i++) {
      var line = source.get(i);
      for (Entry<String, String> entry : variables.entrySet()) {
        var holder = "@\\(\\s*" + Pattern.quote(entry.getKey()) + "\\s*\\)";
        line = Pattern.compile(holder, Pattern.CASE_INSENSITIVE).matcher(line)
            .replaceAll(Matcher.quoteReplacement(entry.getValue()));
      }
      source.set(i, line);
    }
  }

  public static List<String> toVariableList(Map<String, String> variables) {
    List<String> list = new ArrayList<>();
    list.add("<!-- @variables");
    for (Map.Entry<String, String> variable : variables.entrySet()) {
      list.add(" " + variable.getKey() + "=" + variable.getValue());
    }
    list.add("-->");
    return list;
  }

  // <!-- @variables:fileLocation -->
  static final Pattern varFilePattern = Pattern.compile("^\\s*<!--\\s*@variables\\s*:\\s*[^\\s]+\\s*-->\\s*$",
      Pattern.CASE_INSENSITIVE);

  public static void parseVariablesFile(List<String> source, Path configPath, Map<String, String> variables)
      throws Exception {

    // @variables:fileLocation
    while (true) {
      var pos = -1;
      while ((++pos < source.size()) && !varFilePattern.matcher(source.get(pos)).matches()) {
      }
      if (pos == source.size()) {
        break;
      }

      var varFileLine = source.get(pos);
      var varIdx = varFileLine.indexOf(":");
      var fileLocation = varFileLine.substring(varIdx + 1, varFileLine.indexOf("-->", varIdx)).strip();

      var filePath = configPath.resolve(fileLocation);
      if (!Files.exists(filePath)) {
        throw new IllegalArgumentException("The variables file does not exist: " + filePath.toAbsolutePath());
      }
      source.remove(pos);

      // Import Variables
      var props = new Properties();
      try (Reader r = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
        props.load(r);
      }
      props.forEach((k, v) -> variables.put((String) k, (String) v));
    }
  }

  // <!-- @variables
  // title=expression
  // __layout=layout
  // -->

  static final Pattern varStartPattern = Pattern.compile("^\\s*<!--\\s*@variables\\s*$", Pattern.CASE_INSENSITIVE);

  static final Pattern varEndPattern = Pattern.compile("^\\s*-->\\s*$");

  static final Pattern varNameValPattern = Pattern.compile("^\\s*[^\\s=]+\\s*=.*$", Pattern.CASE_INSENSITIVE);

  public static void parseVariables(List<String> source, String viewName, Map<String, String> variables)
      throws Exception {

    // @variables
    while (true) {
      var start = -1;
      while ((++start < source.size()) && !varStartPattern.matcher(source.get(start)).matches()) {
      }
      if (start == source.size()) {
        break;
      }

      var end = start;
      while ((++end < source.size()) && !varEndPattern.matcher(source.get(end)).matches()) {
      }

      if (end == source.size()) {
        throw new IllegalArgumentException("@variables must have a closing directive (viewName=" + viewName + ")");
      }

      // variables: start-end
      for (var i = start + 1; i < end; i++) {
        var nameVal = source.get(i).strip();
        if ((nameVal.isEmpty()) || nameVal.startsWith("//")) {
          continue;
        }
        if (!varNameValPattern.matcher(nameVal).matches()) {
          throw new IllegalArgumentException(
              "Variable is invalid (name/value=" + nameVal + ", viewName=" + viewName + ")");
        }
        var idx = nameVal.indexOf('=');
        variables.put(nameVal.substring(0, idx).strip(), StringUtils.trimToEmpty(nameVal.substring(idx + 1)));
      }

      removeSubSource(source, start, end);
    }
  }

  // <!-- @someSection begin -->
  // <!-- @someSection end -->

  static final Pattern sectionStartPattern = Pattern.compile("^\\s*<!--\\s*@[^\\s]+\\s+begin\\s*-->\\s*$",
      Pattern.CASE_INSENSITIVE);

  static final Pattern sectionEndPattern = Pattern.compile("^\\s*<!--\\s*@[^\\s]+\\s+end\\s*-->\\s*$",
      Pattern.CASE_INSENSITIVE);

  public static void parseSections(List<String> viewSource, Map<String, List<String>> sections, String viewName) {
    while (true) {

      var start = -1;
      while ((++start < viewSource.size()) && !sectionStartPattern.matcher(viewSource.get(start)).matches()) {
      }
      if (start == viewSource.size()) {
        break;
      }

      var sectionLine = viewSource.get(start);
      var idx = sectionLine.indexOf("@");
      var sectionName = sectionLine.substring(idx + 1, sectionLine.indexOf(' ', idx)).strip();

      var end = start;
      var hasClosing = true;

      while ((++end < viewSource.size()) && !sectionEndPattern.matcher(viewSource.get(end)).matches()) {
        if (sectionStartPattern.matcher(viewSource.get(end)).matches()) {
          hasClosing = false;
          break;
        }
      }
      if (!hasClosing || (end == viewSource.size())) {
        throw new IllegalArgumentException(
            "@" + sectionName + " must have a closing directive (viewName=" + viewName + ")");
      }
      if (sections.containsKey(sectionName)) {
        throw new IllegalArgumentException("@" + sectionName + " is duplicated (viewName=" + viewName + ")");
      }

      if (end - start > 1) {
        sections.put(sectionName, copySubSource(viewSource, start + 1, end - 1));
      } else {
        sections.put(sectionName, new ArrayList<>());
      }

      removeSubSource(viewSource, start, end);
    }
  }

  // <!-- @doBody -->

  static final Pattern doBodyPattern = Pattern.compile("^\\s*<!--\\s*@doBody\\s*-->\\s*$", Pattern.CASE_INSENSITIVE);

  public static void replaceBody(List<String> layoutSource, String layoutViewName, List<String> viewSource,
      String viewName, ViewSourceHandler handler) {

    // @doBody
    var doBody = false;
    while (true) {

      var pos = -1;
      while ((++pos < layoutSource.size()) && !doBodyPattern.matcher(layoutSource.get(pos)).matches()) {
      }
      if (pos == layoutSource.size()) {
        break;
      }
      if (doBody) {
        throw new IllegalArgumentException("@doBody is duplicated (layoutViewName=" + layoutViewName + ")");
      }

      var bodyLine = layoutSource.get(pos);
      var indent = copyIndent(bodyLine);
      layoutSource.remove(pos);

      var incViewName = ViewUtils.getInclViewName(viewName);
      handler.insertBody(layoutSource, pos, indent, viewSource, incViewName);

      doBody = true;
    }

    if (!doBody) {
      throw new IllegalArgumentException("@doBody is required (layoutViewName=" + layoutViewName + ")");
    }
  }

  // <!-- @someSection? -->

  static final Pattern sectionPattern = Pattern.compile("^\\s*<!--\\s*@\\w+(\\?)?\\s*-->\\s*$",
      Pattern.CASE_INSENSITIVE);

  public static void replaceSections(List<String> layoutSource, String viewName, Map<String, List<String>> sections) {

    while (true) {
      var pos = -1;
      while ((++pos < layoutSource.size()) && !sectionPattern.matcher(layoutSource.get(pos)).matches()) {
      }
      if (pos == layoutSource.size()) {
        break;
      }

      var sectionLine = layoutSource.get(pos);
      var sectionName = sectionLine.substring(sectionLine.indexOf("@") + 1, sectionLine.indexOf("-->")).strip();

      var sectionRequired = true;
      if (sectionName.endsWith("?")) {
        sectionName = sectionName.substring(0, sectionName.length() - 1);
        sectionRequired = false;
      }

      var sectionSource = sections.get(sectionName);
      if (sectionSource != null) {

        sectionSource.add(0, "<!-- @" + sectionName + " begin -->");
        sectionSource.add("<!-- @" + sectionName + " end -->");

        layoutSource.remove(pos);
        layoutSource.addAll(pos, sectionSource);
      } else {
        if (sectionRequired) {
          throw new IllegalArgumentException("@" + sectionName + " is required (viewName=" + viewName + ")");
        } else {
          layoutSource.set(pos, "<!-- @" + sectionName + "? undefined -->");
        }
      }
    }
  }
}
