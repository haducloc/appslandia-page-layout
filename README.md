# Page Layout Tool
## Features

- A powerful tool for managing web page layouts
- Supports defining and referencing variables
- Enables defining sections and injecting them dynamically
- View engines: `JSP/JSPX`, `Facelets`, `Pebble`, etc

## Installation

### Java Version

- Java 11+

### Maven pom.xml

``` XML
<build>
  <plugins>
    <plugin>
      <groupId>com.appslandia</groupId>
      <artifactId>appslandia-page-layout</artifactId>
      <version>1.1</version>

      <configuration>
        <!-- Absolute path to the input views directory -->
        <inputViewsDir>${project.basedir}/WebContent/WEB-INF/__views</inputViewsDir>

        <!-- Output directory, relative to the inputViewsDir -->
        <outputViewsDir>views</outputViewsDir>

        <!-- Configuration directory located under inputViewsDir -->
        <configDir>__config</configDir>

        <!-- File extensions to process -->
        <viewSuffixes>.jsp,.jspx,.xhtml,.peb</viewSuffixes>

        <!-- Optional settings -->
        <debugVariables>false</debugVariables>
        <removeBlankLines>false</removeBlankLines>
        <skipPlugin>false</skipPlugin>
      </configuration>

      <executions>
        <execution>
          <id>process-layout</id>
          <phase>generate-sources</phase>
          <goals>
            <goal>process-layout</goal>
          </goals>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```
### Sample Uses
#### Views Folder Structure

```
__views/

├── config/
│   └── layout.xhtml
├── page1.xhtml

views (generated)/

├── page1_inc.xhtml
└── page1.xhtml

```

#### __views / __config / layout.xhtml
- Manages the main layout, which is reused by other views
- Supports multiple main layouts
- Supports multiple section holders for injection
- `@doBody` is required
- `@variables` is optional

``` HTML
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml"
      xmlns:ui="jakarta.faces.facelets">
<head>
<!-- @variables
  context_path=${request.contextPath}
  other_variable=expression_value
-->
  <link rel="stylesheet" href="@(context_path)/css/app.css" />
</head>
<body>
  <h2>@(page.title)</h2>
    
  <main role="main">
    <!-- @doBody -->
  </main>
	
  <!-- @jsSection? -->
</body>
</html>
```

#### __views / page1.xhtml
- Manages the content of page1, which will be inserted into the @doBody placeholder.

``` HTML
<!--@variables
  page.title=${resources.page1_title}
  __layout=layout
 -->
<?xml version="1.0" encoding="UTF-8"?>
<ui:component xmlns="http://www.w3.org/1999/xhtml"
  xmlns:ui="jakarta.faces.facelets">
  
  ${model.message}
</ui:component>

<!--@jsSection begin -->
<script type="text/javascript">
  // JS code
</script>
<!-- @jsSection end -->
```

#### views / page1.xhtml ( generated)
``` HTML
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml"
      xmlns:ui="jakarta.faces.facelets">
<head>
   <link rel="stylesheet" href="${request.contextPath}/css/app.css" />
</head>
<body>
  <h2>${resources.page1_title}</h2>
    
  <main role="main">
    <!-- @doBody begin -->
    <ui:include src="page1_inc.xhtml" />
    <!-- @doBody end -->
  </main>
	
<!-- @jsSection begin -->
<script type="text/javascript">
  // JS code
</script>
<!-- @jsSection end -->

</body>
</html>
```

#### views / page1_inc.xhtml ( generated)
``` HTML
<?xml version="1.0" encoding="UTF-8"?>
<ui:component xmlns="http://www.w3.org/1999/xhtml"
  xmlns:ui="jakarta.faces.facelets">
  
  ${model.message}
</ui:component>
```

## License
This code is distributed under the terms and conditions of the [MIT license](LICENSE).