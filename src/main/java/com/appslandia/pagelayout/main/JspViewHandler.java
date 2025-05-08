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

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.appslandia.pagelayout.utils.ViewSourceUtils;

/**
 *
 * @author Loc Ha
 *
 */
public class JspViewHandler extends ViewSourceHandler {

  @Override
  public boolean incViewFile() {
    return true;
  }

  @Override
  public void insertBody(List<String> layoutSource, int bodyPos, String bodyIndent, List<String> bodySource,
      String incViewFile) {
    layoutSource.add(bodyPos, bodyIndent + "<!-- @doBody begin -->");
    layoutSource.add(bodyPos + 1, bodyIndent + "<%@ include file=\"" + incViewFile + "\" %>");
    layoutSource.add(bodyPos + 2, bodyIndent + "<!-- @doBody end -->");
  }

  @Override
  public void handleSource(List<String> source, String sourceView, boolean layoutSource) {
    handlePageDir(source, sourceView, layoutSource);
  }

  // session="false" trimDirectiveWhitespaces="true" pageEncoding="UTF-8"

  static final Pattern startPageDirPattern = Pattern.compile("^\\s*<%@\\s*page.*");

  static final Pattern endDirPattern = Pattern.compile(".*%>\\s*$");

  static final Pattern sessionAttrPattern = Pattern.compile("session\\s*=\\s*\"\\s*(true|false)\\s*\"");

  static final Pattern trimDirectiveWhitespacesAttrPattern = Pattern
      .compile("trimDirectiveWhitespaces\\s*=\\s*\"\\s*(true|false)\\s*\"");

  static final Pattern pageEncodingAttrPattern = Pattern.compile("pageEncoding\\s*=\\s*\"\\s*[^\\s]+\\s*\"");

  protected void handlePageDir(List<String> source, String sourceView, boolean layoutSource) {
    String pageDirective = null;
    while (true) {

      var start = -1;
      while ((++start < source.size()) && !startPageDirPattern.matcher(source.get(start)).matches()) {
      }
      if (start == source.size()) {
        break;
      }

      var end = start;
      while ((end < source.size()) && !endDirPattern.matcher(source.get(end)).matches()) {
        end++;
      }
      if (end == source.size()) {
        throw new IllegalArgumentException(
            "No close for the '" + source.get(start) + "' (sourceView=" + sourceView + ")");
      }
      if (pageDirective != null) {
        throw new IllegalArgumentException("<% page ... %> is duplicated (sourceView=" + sourceView + ")");
      }

      pageDirective = toDirectiveSource(source, start, end);
      ViewSourceUtils.removeSubSource(source, start, end);
    }

    // No need <%@ page %> for viewSource
    if (!layoutSource) {
      return;
    }

    if (pageDirective != null) {
      Matcher matcher = null;

      // session
      matcher = sessionAttrPattern.matcher(pageDirective);
      if (!matcher.find()) {
        pageDirective = addDirectiveAttribute(pageDirective, " session=\"false\"");
      }

      // trimDirectiveWhitespaces
      matcher = trimDirectiveWhitespacesAttrPattern.matcher(pageDirective);
      if (!matcher.find()) {
        pageDirective = addDirectiveAttribute(pageDirective, " trimDirectiveWhitespaces=\"true\"");
      }

      // pageEncoding
      matcher = pageEncodingAttrPattern.matcher(pageDirective);
      if (matcher.find()) {
        pageDirective = matcher.replaceAll("pageEncoding=\"UTF-8\"");
      } else {
        pageDirective = addDirectiveAttribute(pageDirective, " pageEncoding=\"UTF-8\"");
      }

      pageDirective = pageDirective.replaceAll("\\s{2,}", " ");
      source.add(0, pageDirective);

    } else {
      source.add(0,
          "<%@ page contentType=\"text/html; charset=utf-8\" session=\"false\" trimDirectiveWhitespaces=\"true\" pageEncoding=\"UTF-8\"%>");
    }
  }

  static String toDirectiveSource(List<String> source, int start, int end) {
    var sb = new StringBuilder();
    for (var i = start; i <= end; i++) {
      if (sb.length() > 0) {
        sb.append(" ");
      }
      sb.append(source.get(i).strip());
    }
    return sb.toString();
  }

  static String addDirectiveAttribute(String directive, String attr) {
    var idx = directive.lastIndexOf("%>");
    return directive.substring(0, idx) + attr + "%>";
  }
}
