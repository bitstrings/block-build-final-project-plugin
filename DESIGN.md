# Design
Java source code is located at:
```
src/main/java/org/jenkinsci/plugins/blockbuildfinalproject
```
### BlockBuildJobProperty
Immutable class that extends `JobProperty` and holds the project properties:
* `useBlockBuildUpstreamProject` - true if blocking on upstream projects is enabled
* `useBlockBuildDownstreamProject` - true if blocking on downstream projects is enabled
* `finalUpstreamProjectsList` - list of final upstream projects
* `finalDownstreamProjectsList` - list of final downstream projects

### BlockBuildQueueTaskDispatcher
Extends `QueueTaskDispatcher` and overrides `canRun()`.  Jenkins periodically invokes `canRun()` to determine if a project can build.  If any of the QueueTaskDispatcher's return the project is blocked, that project cannot run.  `BlockBuildQueueTaskDispatcher` uses helper class `BlockBuild` to determine if the current project is blocked by upstream or downstream projects building.

### BlockBuild
Provides APIs `checkBuildingUpstream()` and `checkBuildingDownstream()`.

Provides API `getTransitiveUpOrDownstreamProjectsFinal()`, which is similar to Jenkins built-in APIs `DependencyGraph.getTransitiveUpstream()` and `DependencyGraph.getTransitiveDownstream()`.  The Jenkins methods return all upstream or downstream projects.  The `getTransitiveUpOrDownstreamProjectsFinal()` method searching the dependency graph upstream or downstream, until a a final project is reached.  This API is not specific to this plug-in and could be used by other plug-ins.

### JenkinsWrapper
FindBugs was generating NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE warnings when accessing certain Jenkins functionality.  This class provides a wrapper around those methods and checks for null pointers.

### AutoCompleteUtils
Utility functions for autocompleting and checking project names.

When the user types the name of a project in a textbox, `AutoCompleteUtils.autoCompleteProjects()` is used to autocomplete a list of projects that start with the text entered.

When the user selects a project name, `AutoCompleteUtils.checkProjects()` is used to verify that project is valid.

This class is not specific to this plug-in and can be used by other plug-ins.  I'm surprised Jenkins doesn't provide these functions already!

# Resources
The resources directory provides the GUI components and help files.

### index.jelly
```
src/main/resources/index.jelly
```
Provides the text rendered on the installed plug-ins page.

### BlockBuildJobProperty
Defines the GUI and help files for class BlockBuildJobProperty.
```
src/main/resources/org/jenkinsci/plugins/blockbuildfinalproject
```
* `config.jelly` - Defines the following components:
 ![Alt text](images/block-build-final-project.jpg?raw=true)
* `help-*.html` - The help files for the corresponding components. 

# Tests
Tests are located at:
```
src/test/java/org/jenkinsci/plugins/blockbuildfinalproject
```

### AutoCompleteUtilsIntTest
Integration tests for class `AutoCompleteUtils` using the Jenkins test framework.  Only a few test cases are included here because `AutoCompleteUtilsUnitTest` provides exhaustive test coverage.

### AutoCompleteUtilsUnitTest
Unit tests class `AutoCompleteUtils`.  In hindsight, this test class wasn't worth it.  Way too many methods/classes are mocked.  If the methods in `AutoCompleteUtils` are ever refactored, these test cases will need to be re-written as well.  The plus side is the tests were a great learning experience for `PowerMockito`!

### BlockBuildIntTest
Integration tests class `BlockBuild` using the Jenkins test framework.  Contains test case `testProjectBlocking()`, which is the main test case for this plug-in.

### BlockBuildItemListenerIntTest
Tests deleting and renaming projects.

### BlockBuildJobPropertyUnitTest
Tests class `BlockBuildJobProperty`.

### HTMLTest
Performs a few simple tests that ensure this plugin shows up when configuring a project.

### TestBuilderSignal
Use this build step to fire off a signal that the build has started.

### TestBuilderSleep
Build step that pretends to do work by sleeping.

