# method-nullability-plugin

Eclipse plugin that adds a hover on methods showing method nullability.

## Development setup

Install Eclipse Neon with PDE.
[PDE](http://www.eclipse.org/pde/) is included in the [Eclipse Committers build](http://www.eclipse.org/downloads/packages/eclipse-ide-eclipse-committers/neon2).

## Test setup

Run the plugin with the standard `Run` or `Debug` launch buttons.
A new Eclipse will open.
Unpack and import the test project (see important files) into the workspace of the opened Eclipse.
The test projects requires [M2E](http://www.eclipse.org/m2e/index.html) to be installed, so install it in the opened Eclipse. Add 'http://download.eclipse.org/releases/neon' to the 'Available Software Sites' so dependencies of [M2E] can be resolved. 
Open a class in the test project and hover a method call.
It should show a nullability popup instead of a the standard JavaDoc popup.

## Important files

### plugin.xml, build.properties, META-INF/MANIFEST.MF

Open with the Plugin-Manifest Editor.
Declares the used extension points and the provided extensions, as well as the dependencies to other plugins.
Sets the files available at the plugin run-time in the build properties, i.e., the database and libraries.

### lib

Contains the JARs of the third-party dependencies:

#### commons-csv

Used by `DatabaseFiller` to recreate the plugin's nullability database from raw data if available.

#### guava

Used for its hashing function, used to create key from hovered method for nullbility lookup.

#### sqlite

Used for the nullability database.

### src/main/resources

#### method-nullability-plugin-test.zip

Test project to import into test workspace.

#### method-nullability.db

SQLite database, contains indexes for method nullability lookup.

## Possible issues

So far only tested on OS X, might have path seperator issues on other platforms.
If so, try to replace `/` with `File.separator` to make it work.
