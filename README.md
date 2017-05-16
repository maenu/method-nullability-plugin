# method-nullability-plugin
Eclipse plugin that augments the method documentation (javadoc) with nullability information and automatically generates Eclipse external annotations (EEA) accordingly to leverage Eclipse's annotation-based null analysis feature.

## Plugin installation
To install the plugin in any Eclipse IDE for normal use, proceed as follows:

* Install Eclipse Neon with [PDE](http://www.eclipse.org/pde/).
* Checkout the 3 source projects into an Eclipse workspace with your Eclipse PDE. Use the site.xml file to build the features and the update site, respectively. 
* In the Eclipse instance you want to install the plugin go to Help -> Install New Software... and install the plugin as usual using the previously built update site.
* After the installation, go to Windows -> Preferences -> Method Nullability to configure the plugin and generate EEA files.

## Plugin configuration
Once the plugin is installed, the following configuration has to be made:
* Go to Windows -> Preferences -> Method Nullability.
* Enable Eclipse annotation-based null analysis in menu 'Errors/Warnings'.
* Generate EEA files and javadoc database: Specify a CSV file (to start with, use sample 'inter-intra_small.csv') and a EEA destination folder (workspace location). Optionally specify an artifactId (e.g. httpclient) if you want to only generate EEA files for a specific set of libraries.
* Modify the max. and min. nullability thresholds as you prefer.

## Development and Test setup
To setup the plugin for development and test, proceed as follows:

* Install Eclipse Neon with PDE. [PDE](http://www.eclipse.org/pde/) is included in the [Eclipse Committers build (http://www.eclipse.org/downloads/packages/eclipse-ide-eclipse-committers/neon2).
* Run the plugin from within your Eclipse PDE workspace with the standard `Run` or `Debug` launch buttons. A new Eclipse will open.
* Unpack and import the test project (see important files) into the workspace of the opened Eclipse. The test projects requires [M2E](http://www.eclipse.org/m2e/index.html) to be installed, so install it in the opened Eclipse. Add 'http://download.eclipse.org/releases/neon' to the 'Available Software Sites' so dependencies of [M2E] can be resolved. 
* Open a class in the test project and move your mouse over a method call. The javadoc hover will have nullability information included in the 'Returns' section, for example '29% check the returned value (85 out of 294 invocations)'.
* To generate Eclipse external annotation (EEA) files, go to Windows -> Preferences -> Method Nullability and use the widget to write EEA files to a workspace location. You have to specify a CSV source that contains the nullability information.

## Important files

### Eclipse projects

#### ch.unibe.scg.methodnullability.plugin
Contains all the source code of the plugin.
Furthermore, the following files are included:

##### plugin.xml, build.properties, META-INF/MANIFEST.MF
Open with the Plugin-Manifest Editor.
Declares the used extension points and the provided extensions, as well as the dependencies to other plugins.
Sets the files available at the plugin run-time in the build properties, i.e., the database and libraries.

##### inter-intra_small.csv
Sample nullability data to use as input to generate EEA files and javadoc database. 

##### src/main/resources/

###### method-nullability-plugin-test.zip
Test project to import into test workspace.

##### method-nullability.db
SQLite database, contains indexes for method nullability lookup.

##### lib/
Contains the JARs of the third-party dependencies:

* commons-csv <br>Used by `DatabaseFiller` to recreate the plugin's nullability database from raw data if available.
* guava <br>Used for its hashing function, used to create key from hovered method for nullbility lookup.
* sqlite-jdbc <br>Used for the nullability database.
* h2 <br>Used as auxiliary database to select from CSV data

#### ch.unibe.scg.methodnullability.feature
Contains the feature information and configuration of the plugin.

#### ch.unibe.scg.methodnullability.updatesite
Contains the site.xml used to build the update site of the plugin.

## Test issues
This plugin has been successfully run on Windows and Mac OS X. 
