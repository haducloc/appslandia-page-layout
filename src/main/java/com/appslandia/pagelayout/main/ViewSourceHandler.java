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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Loc Ha
 *
 */
public abstract class ViewSourceHandler {

  public abstract boolean incViewFile();

  public abstract void handleSource(List<String> source, String sourceView, boolean layoutSource);

  public abstract void insertBody(List<String> layoutSource, int bodyPos, String bodyIndent, List<String> bodySource,
      String incViewFile);

  static final Map<String, ViewSourceHandler> handlers;
  static {
    Map<String, ViewSourceHandler> map = new HashMap<>();

    map.put(".jsp", new JspViewHandler());
    map.put(".jspx", new JspViewHandler());
    map.put(".xhtml", new FaceletViewHandler());
    map.put(".peb", new PebbleViewHandler());

    map.put(".other", new OtherViewHandler());

    // handlers = Collections.unmodifiableMap(map);
    handlers = map;
  }

  public static void registerHandler(String viewSuffix, ViewSourceHandler handler) {
    handlers.put(viewSuffix, handler);
  }

  public static ViewSourceHandler getHandler(String viewSuffix) {
    var handler = handlers.get(viewSuffix);
    if (handler != null) {
      return handler;
    }
    return handlers.get(".other");
  }
}
