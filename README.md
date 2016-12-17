# Block Build Final Project Plug-in
This plug-in allows a project to block when upstream or downstream projects are building.  Jenkins already provides this functionality built-in, but it blocks on the entire pipeline.  This plug-in allows an optional final project to be specified.  This allows the project to block on part of the pipeline instead of the entire pipeline.

![Alt text](images/block-build-final-project.jpg?raw=true)

## Example
The below pipeline will be used as an example:

![Alt text](images/cool-app-pipeline.jpg?raw=true)

This pipeline compiles, tests, and deploys.  The testing is divided into two projects - cool-app-test-server-only and cool-app-test-server-and-client.  When cool-app-test-server-only is kicked off, the new code is installed in the server workspace and server-side tests are run.  cool-app-test-server-and-client starts a server using the workspace and resources from project cool-app-test-server-only, and then runs tests via the client.  Two projects sharing workspaces and/or resources is not a good design and should be avoid whenever possible.  But in this example, the two test projects having their own workspaces and resources is not practical because of financial and resource constraints, so they must share.

In this example, UserOne kicks off project cool-app-build and shortly afterwards, UserTwo kicks off cool-app-build.

### Default Behavior
The default behavior is:

Time | UserOne | UserTwo
---- | ------- | -------
1 | cool-app-build | queued
2 | cool-app-test-server-only | cool-app-build
3 | cool-app-test-client-and-server | cool-app-test-server-only
4 | cool-app-deploy | cool-app-test-client-and-server
5 | | cool-app-deploy

UserOne and UserTwo end up running tests at the same time!  At time period 3, UserOne is running cool-app-test-client-and-serve and UserTwo is running cool-app-test-server-only.  Because both of the test projects share resources, there will test random test failures.


### Lock
There are plug-ins that allow a project to acquire a lock when it starts and release the lock when it finishes:

Time | UserOne | UserTwo
---- | ------- | -------
1 | cool-app-build | queued
2 | cool-app-test-server-only | cool-app-build
3 | waiting for lock | cool-app-test-server-only
4 | cool-app-test-client-and-server | waiting for lock
5 | cool-app-deploy | cool-app-test-client-and-server
6 | | cool-app-deploy

In this scenario, UserOne delivers its code to the test area and runs the server tests.  Then UserTwo delivers its code to the test area and runs the server tests.  But then UserOne runs cool-app-test-client-and-server, but UserTwo's code and resources are in that workspace, causing failures.

### Jenkins Built-in Behavior
Jenkins already provides a mechanism to block a build when down stream or upstream projects are building.  In cool-app-test-server-only:

![Alt text](images/block-downstream-built-in.jpg?raw=true)

In cool-app-test-client-and-server:

![Alt text](images/block-upstream-built-in.jpg?raw=true)

Time | UserOne | UserTwo
---- | ------- | -------
1 | cool-app-build | queued
2 | cool-app-test-server-only | cool-app-build
3 | cool-app-test-client-and-server | queued
4 | cool-app-deploy | queued
5 | | cool-app-test-server-only
6 | | cool-app-test-client-and-server
7 | | cool-app-deploy

With this approach, we get the desired behavior that only one user is running projects cool-app-test-server-only or cool-app-test-client-and-server at a time.  Also, once a user starts running the test projects, both tests projects are completed before the next user can run tests.

The drawback to this approach is the entire pipeline following the tests is also blocked.  UserTwo must wait for UserOne's cool-app-deploy project to finish.  UserOne should be able to deploy while UserTwo is running tests.  This example pipeline is very small.  Imagine a pipeline with 10 projects after the test projects - very inefficient.

###  Block Build Final Project Plug-in
The Block Build Final Project Plug-in provides a mechanism to block a build when downstream or upstream projects are building and specify final projects to stop searching.  In cool-app-test-server-only:

![Alt text](images/block-downstream-plug-in.jpg?raw=true)

In cool-app-test-client-and-server:

![Alt text](images/block-upstream-plug-in.jpg?raw=true)

Time | UserOne | UserTwo
---- | ------- | -------
1 | cool-app-build | queued
2 | cool-app-test-server-only | cool-app-build
3 | cool-app-test-client-and-server | queued
4 | cool-app-deploy | cool-app-test-server-only
5 | | cool-app-test-client-and-server
6 | | cool-app-deploy

With this plug-in, the following three conditions are met:

1. Only one user can run cool-app-test-server-only or cool-app-test-client-and-server at a time.
2. Once a user starts running the test projects, both tests projects are completed before the next user can run tests.
3. After the test projects are complete, another user may run the test projects.  The second user isn't blocked waiting on the first users entire pipeline to complete.

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

