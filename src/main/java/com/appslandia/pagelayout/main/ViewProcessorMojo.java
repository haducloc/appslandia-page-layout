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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.appslandia.pagelayout.utils.FileNameUtils;

/**
 *
 * @author Loc Ha
 *
 */
@Mojo(name = "process-layout", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class ViewProcessorMojo extends AbstractMojo {

  @Parameter(property = "inputViewsDir", defaultValue = "${project.basedir}/WebContent/WEB-INF/__views")
  protected String inputViewsDir;

  @Parameter(property = "outputViewsDir", defaultValue = "views")
  protected String outputViewsDir;

  @Parameter(property = "configDir", defaultValue = "__config")
  protected String configDir;

  @Parameter(property = "viewSuffixes", defaultValue = ".jsp,.jspx,.xhtml,.peb")
  protected String viewSuffixes;

  @Parameter(property = "skipPlugin", defaultValue = "false")
  private boolean skipPlugin;

  @Parameter(property = "debugVariables", defaultValue = "false")
  private boolean debugVariables;

  @Parameter(property = "removeBlankLines", defaultValue = "false")
  private boolean removeBlankLines;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    this.inputViewsDir = FileNameUtils.toUnixPath(this.inputViewsDir);

    getLog().info("Invoking " + getClass().getName() + ".execute()");

    getLog().info("inputViewsDir: " + this.inputViewsDir);
    getLog().info("outputViewsDir: " + this.outputViewsDir);
    getLog().info("configDir: " + this.configDir);
    getLog().info("viewSuffixes: " + this.viewSuffixes);

    getLog().info("debugVariables: " + this.debugVariables);
    getLog().info("removeBlankLines: " + this.removeBlankLines);
    getLog().info("skipPlugin: " + this.skipPlugin);

    if (this.skipPlugin) {
      getLog().info("Execution skipped because skipPlugin=true.");
      return;
    }

    try {
      new ViewProcessor().inputViewsDir(this.inputViewsDir).outputViewsDir(this.outputViewsDir)
          .configDir(this.configDir).debugVariables(this.debugVariables).removeBlankLines(this.removeBlankLines)
          .process();

    } catch (Exception ex) {
      throw new MojoExecutionException(ex.getMessage(), ex);
    }

    getLog().info("Done " + getClass().getName() + ".execute()");
  }
}
