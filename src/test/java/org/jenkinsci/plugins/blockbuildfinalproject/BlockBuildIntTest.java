/*
 * The MIT License
 *
 * Copyright (c) 2016, Chad Rosenquist
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.jenkinsci.plugins.blockbuildfinalproject;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import com.google.common.collect.ImmutableList;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.queue.CauseOfBlockage;
import hudson.tasks.BuildTrigger;
import hudson.util.OneShotEvent;
import jenkins.model.Jenkins;

/**
 * Tests BlockBuild class
 * 
 * 1. getTransitiveUpOrDownstreamProjectsFinal
 *    Most of the test cases are for downstream projects.  The code for upstream is
 *    almost identical, so only a few upstream test cases were written.
 * 2. checkBuildingDownstream
 * 3. checkBuildingUpstream
 * 4. testProjectBlocking
 *    This is the main test case for this plug-in.  It kicks off two builds in a row.
 * 
 * @author Chad Rosenquist
 *
 */
public class BlockBuildIntTest {
    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();
    
    private FreeStyleProject downstreamGrandparent;
    private FreeStyleProject downstreamParentBrian;
    private FreeStyleProject downstreamChildRemy;
    private FreeStyleProject downstreamParentChad;
    private FreeStyleProject downstreamChildNeil;
    private FreeStyleProject downstreamChildPhoebe;
    
    private FreeStyleProject upstreamGrandparentBill;
    private FreeStyleProject upstreamGrandparentColleen;
    private FreeStyleProject upstreamParentKatie;
    private FreeStyleProject upstreamParentChad;
    private FreeStyleProject upstreamChildNeil;
    
    private FreeStyleProject projectA;
    private FreeStyleProject projectB;
    
    private static final long PROJECT_BUILD_TIME = 2000;      // milliseconds
    private static final long PROJECT_B_WAIT_TIMEOUT = 60;    // seconds
    
    private OneShotEvent projectABuildStarted = new OneShotEvent();
    private OneShotEvent projectBBuildStarted = new OneShotEvent();

    @Before
    public void setUp() throws Exception {
        // Clear the queue from previous tests.
        Jenkins.getInstance().getQueue().clear();
        
        createDownstreamPipeline();
        createUpstreamPipeline();
        createProjectAandB();
    }
    
    /**
     * Creates the downstream pipeline for testing getTransitiveUpOrDownstreamProjectsFinal().
     * 
     * downstream-grandparent -> downstream-parent-brian -> downstream-child-remy
     *                        |
     *                        -> downstream-parent-chad  -> downstream-child-neil
     *                                                   |
     *                                                   -> downstream-child-phoebe
     * @return grandparent project
     * @throws Exception
     */
    private void createDownstreamPipeline() throws Exception {
        // Create projects.
        downstreamGrandparent = jenkinsRule.createFreeStyleProject("downstream-grandparent");
        downstreamParentBrian = jenkinsRule.createFreeStyleProject("downstream-parent-brian");
        downstreamChildRemy = jenkinsRule.createFreeStyleProject("downstream-child-remy");
        downstreamParentChad = jenkinsRule.createFreeStyleProject("downstream-parent-chad");
        downstreamChildNeil = jenkinsRule.createFreeStyleProject("downstream-child-neil");
        downstreamChildPhoebe = jenkinsRule.createFreeStyleProject("downstream-child-phoebe");
        
        // Create triggers.
        downstreamGrandparent.getPublishersList().add(new BuildTrigger("downstream-parent-brian", true));
        downstreamGrandparent.getPublishersList().add(new BuildTrigger("downstream-parent-chad", true));
        downstreamParentBrian.getPublishersList().add(new BuildTrigger("downstream-child-remy", true));
        downstreamParentChad.getPublishersList().add(new BuildTrigger("downstream-child-neil", true));
        downstreamParentChad.getPublishersList().add(new BuildTrigger("downstream-child-phoebe", true));
        
        // Rebuild dependency graph.
        jenkinsRule.jenkins.rebuildDependencyGraph();
    }
    
    /**
     * Creates the upstream pipeline for testing getTransitiveUpOrDownstreamProjectsFinal().
     * 
     * upstream-grandparent-bill    <- |
     *                                 | 
     * upstream-grandparent-colleen <- | <- upstream-parent-katie <- |
     *                                                               |
     *                                      upstream-parent-chad  <- | <- upstream-child-neil
     * @throws Exception
     */
    private void createUpstreamPipeline() throws Exception {
        // Create projects.
        upstreamGrandparentBill = jenkinsRule.createFreeStyleProject("upstream-grandparent-bill");
        upstreamGrandparentColleen = jenkinsRule.createFreeStyleProject("upstream-grandparent-colleen");
        upstreamParentKatie = jenkinsRule.createFreeStyleProject("upstream-parent-katie");
        upstreamParentChad = jenkinsRule.createFreeStyleProject("upstream-parent-chad");
        upstreamChildNeil = jenkinsRule.createFreeStyleProject("upstream-child-neil");
        
        // Create triggers.
        upstreamGrandparentBill.getPublishersList().add(new BuildTrigger("upstream-parent-katie", true));
        upstreamGrandparentColleen.getPublishersList().add(new BuildTrigger("upstream-parent-katie", true));
        upstreamParentKatie.getPublishersList().add(new BuildTrigger("upstream-child-neil", true));
        upstreamParentChad.getPublishersList().add(new BuildTrigger("upstream-child-neil", true));
        
        // Rebuild dependency graph.
        jenkinsRule.jenkins.rebuildDependencyGraph();
    }
    
    /**
     * Creates a simple pipeline for testing checkBuildingUpstream and checkBuildingDownstream.
     * 
     * project-a  -> project-b
     * 
     * @throws Exception
     */
    private void createProjectAandB() throws Exception {
        // Create the projects.
        projectA = jenkinsRule.createFreeStyleProject("project-a");
        projectB = jenkinsRule.createFreeStyleProject("project-b");
        
        // After project A completes, we want B to immediately kick off.
        projectB.setQuietPeriod(0);
        
        // Project A sends notification when it kicks off.
        projectA.getBuildersList().add(new TestBuilderSignal(projectABuildStarted));
        projectB.getBuildersList().add(new TestBuilderSignal(projectBBuildStarted));
                
        // Have each project sleep, or "build", for 2 seconds.
        projectA.getBuildersList().add(new TestBuilderSleep(PROJECT_BUILD_TIME));
        projectB.getBuildersList().add(new TestBuilderSleep(PROJECT_BUILD_TIME));
        
        // Create triggers.
        projectA.getPublishersList().add(new BuildTrigger("project-b", true));
        
        // Have each project block on each other, using this plug-in.
        BlockBuildJobProperty.updateBlockBuildJobPropertyInProject(projectA,
                new BlockBuildJobProperty(false, "", true, "project-b"));
        projectA.addProperty(new BlockBuildJobProperty(false, "", true, "project-b"));
        BlockBuildJobProperty.updateBlockBuildJobPropertyInProject(projectB,
                new BlockBuildJobProperty(true, "project-a", false, ""));
        
        // Rebuild dependency graph.
        jenkinsRule.jenkins.rebuildDependencyGraph();
    }
    
    /**
     * Waits for the last build in the project to finish.
     * 
     * @param project           project
     * @param timeoutInSeconds  timeout in seconds
     * @return                  the result of the build
     * @throws Exception
     */
    private Result waitForLastBuildToFinish(AbstractProject<?, ?> project, long timeoutInSeconds) throws Exception {
        /*
         * Wait for the last build to become available.
         * This means the build has been kicked off.
         */
        long count;
        for (count = 0;
             (project.getLastBuild() == null) && count < (timeoutInSeconds * 10);
             count++) {
            Thread.sleep(100);
        }
        AbstractBuild<?, ?> build = project.getLastBuild();
        assertNotNull(String.format("Project %s did not start to run in %d seconds", project.getFullName(), timeoutInSeconds),
                      build);

        /*
         * For the result to become available.
         * This means the build has finished.
         */
        for (;
             (build.getResult() == null) && (count < (timeoutInSeconds * 10));
             count++) {
            Thread.sleep(100);
        }
        assertNotNull(String.format("Project %s started running, but did not finish after %d seconds.",
                      project.getFullName(), timeoutInSeconds),
                      build.getResult());
        
        /*
         *  Wait a little extra because there were exceptions that the jobs was still running.
         *  There should be some way to make sure all jobs are not running!?
         */
        Thread.sleep(200);
        
        return build.getResult();
    }
    
    /**
     * No final project is given, so all downstream projects should be returned.
     * 
     * @throws Exception
     */
    @Test
    public void testGetTransitiveUpOrDownstreamProjectsFinal_DownstreamFinalNull() throws Exception {
        // Given
        BlockBuild blockBuild = new BlockBuild(downstreamGrandparent);
        
        // When
        Set<AbstractProject<?, ?>> downstreamProjects = blockBuild.getTransitiveDownstreamProjectsFinal(null);
        
        // Then
        assertEquals(5, downstreamProjects.size());
        assertTrue(downstreamProjects.contains(downstreamParentBrian));
        assertTrue(downstreamProjects.contains(downstreamChildRemy));
        assertTrue(downstreamProjects.contains(downstreamParentChad));
        assertTrue(downstreamProjects.contains(downstreamChildNeil));
        assertTrue(downstreamProjects.contains(downstreamChildPhoebe));
    }
    
    /**
     * No final project is given, so all downstream projects should be returned.
     * 
     * @throws Exception
     */
    @Test
    public void testGetTransitiveUpOrDownstreamProjectsFinal_DownstreamFinalEmpty() throws Exception {
        // Given
        BlockBuild blockBuild = new BlockBuild(downstreamGrandparent);
        ImmutableList.Builder<String> builder = new ImmutableList.Builder<String>();
        ImmutableList<String> finalProjects = builder.build();
        
        // When
        Set<AbstractProject<?, ?>> downstreamProjects = blockBuild.getTransitiveDownstreamProjectsFinal(finalProjects);
        
        // Then
        assertEquals(5, downstreamProjects.size());
        assertTrue(downstreamProjects.contains(downstreamParentBrian));
        assertTrue(downstreamProjects.contains(downstreamChildRemy));
        assertTrue(downstreamProjects.contains(downstreamParentChad));
        assertTrue(downstreamProjects.contains(downstreamChildNeil));
        assertTrue(downstreamProjects.contains(downstreamChildPhoebe));
    }
    
    /**
     * Tests with a project that has no downstream projects.
     * 
     * @throws Exception
     */
    @Test
    public void testGetTransitiveUpOrDownstreamProjectsFinal_DownstreamNoProjects() throws Exception {
        // Given
        BlockBuild blockBuild = new BlockBuild(downstreamChildRemy);
        
        // When
        Set<AbstractProject<?, ?>> downstreamProjects = blockBuild.getTransitiveDownstreamProjectsFinal(null);
                      
        // Then
        assertEquals(0, downstreamProjects.size());
    }
    
    /**
     * Tests downstream with two final projects.
     * 
     * @throws Exception
     */
    @Test
    public void testGetTransitiveUpOrDownstreamProjectsFinal_DownstreamTwoFinalProjects() throws Exception {
        // Given
        BlockBuild blockBuild = new BlockBuild(downstreamGrandparent);
        ImmutableList.Builder<String> builder = new ImmutableList.Builder<String>();
        builder.add("downstream-parent-brian");
        builder.add("downstream-parent-chad");
        ImmutableList<String> finalProjects = builder.build();
        
        // When
        Set<AbstractProject<?, ?>> downstreamProjects = blockBuild.getTransitiveDownstreamProjectsFinal(finalProjects);
        
        // Then
        assertEquals(2, downstreamProjects.size());
        assertTrue(downstreamProjects.contains(downstreamParentBrian));
        assertTrue(downstreamProjects.contains(downstreamParentChad));
    }
    
    /**
     * No final project is given, so all upstream projects should be returned.
     * 
     * @throws Exception
     */
    @Test
    public void testGetTransitiveUpOrDownstreamProjectsFinal_UpstreamFinalEmpty() throws Exception {
        // Given
        BlockBuild blockBuild = new BlockBuild(upstreamChildNeil);
        ImmutableList.Builder<String> builder = new ImmutableList.Builder<String>();
        ImmutableList<String> finalProjects = builder.build();
        
        // When
        Set<AbstractProject<?, ?>> upstreamProjects = blockBuild.getTransitiveUpstreamProjectsFinal(finalProjects);
        
        // Then
        assertEquals(4, upstreamProjects.size());
        assertTrue(upstreamProjects.contains(upstreamParentKatie));
        assertTrue(upstreamProjects.contains(upstreamParentChad));
        assertTrue(upstreamProjects.contains(upstreamGrandparentBill));
        assertTrue(upstreamProjects.contains(upstreamGrandparentColleen));
    }
    
    /**
     * Tests upstream with two final projects.
     * 
     * @throws Exception
     */
    @Test
    public void testGetTransitiveUpOrDownstreamProjectsFinal_UpstreamTwoFinalProjects() throws Exception {
        // Given
        BlockBuild blockBuild = new BlockBuild(upstreamChildNeil);
        ImmutableList.Builder<String> builder = new ImmutableList.Builder<String>();
        builder.add("upstream-parent-katie");
        builder.add("upstream-parent-chad");
        ImmutableList<String> finalProjects = builder.build();
        
        // When
        Set<AbstractProject<?, ?>> upstreamProjects = blockBuild.getTransitiveUpstreamProjectsFinal(finalProjects);
        
        // Then
        assertEquals(2, upstreamProjects.size());
        assertTrue(upstreamProjects.contains(upstreamParentKatie));
        assertTrue(upstreamProjects.contains(upstreamParentChad));
    }
    
    /**
     * Tests when BlockBuildJobProperty is not found.
     * @throws IOException
     */
    @Test
    public void testCheckBuildingDownstream_NullProperty() throws IOException {
        FreeStyleProject project = jenkinsRule.createFreeStyleProject("test-project");
        BlockBuild blockBuild = new BlockBuild(project);
        
        assertNull(blockBuild.checkBuildingDownstream());
    }
    
    /**
     * Tests when blocking on downstream jobs is not enabled.
     * @throws IOException
     */
    @Test
    public void testCheckBuildingDownstream_NotEnabled() throws IOException {
        FreeStyleProject project = jenkinsRule.createFreeStyleProject("test-project");
        BlockBuildJobProperty.updateBlockBuildJobPropertyInProject(project,
                new BlockBuildJobProperty(false, "", false, ""));
        BlockBuild blockBuild = new BlockBuild(project);
        
        assertNull(blockBuild.checkBuildingDownstream());
    }
    
    /**
     * Tests when project-a does not have any downstream jobs building.
     * @throws Exception
     */
    @Test
    public void testCheckBuildingDownstream_NoDownstreamJobsAreBuilding() throws Exception {
        // Given
        // project-b is not running.
        
        // When
        BlockBuild blockBuild = new BlockBuild(projectA);
        CauseOfBlockage blockage = blockBuild.checkBuildingDownstream();
        
        // Then
        assertNull("project-a should not have any downstream jobs building.", blockage);
    }
    
    /**
     * Checks build is blocked when a downstream project is building.
     * @throws Exception
     */
    @Test
    public void testCheckBuildingDownstream_JobIsBuilding() throws Exception {
        // Given
        // Schedule B and wait for it to start running.
        projectB.scheduleBuild2(0);
        projectBBuildStarted.block();
        
        // When
        BlockBuild blockBuild = new BlockBuild(projectA);
        CauseOfBlockage blockage = blockBuild.checkBuildingDownstream();
        
        // Then
        try {
            assertNotNull("CauseOfBlockage should be project-b", blockage);
            assertEquals("Downstream project project-b is already building.", blockage.getShortDescription());
        }
        finally {
            waitForLastBuildToFinish(projectB, PROJECT_B_WAIT_TIMEOUT);
        }
    }
    
    /**
     * Checks the build is blocked when a downstream project is queued to build.
     * @throws Exception
     */
    @Test
    public void testCheckBuildingDownstream_JobIsQueued() throws Exception {
        // Given
        projectB.scheduleBuild2(1);
        
        // When
        BlockBuild blockBuild = new BlockBuild(projectA);
        CauseOfBlockage blockage = blockBuild.checkBuildingDownstream();
        
        // Then
        try {
            assertNotNull("CauseOfBlockage should be project-b", blockage);
            assertEquals("Downstream project project-b is already building.", blockage.getShortDescription());
        }
        finally {
            waitForLastBuildToFinish(projectB, PROJECT_B_WAIT_TIMEOUT);
        }
    }
    
    /**
     * Tests when BlockBuildJobProperty is not found.
     * @throws IOException
     */
    @Test
    public void testCheckBuildingUpstream_NullProperty() throws IOException {
        FreeStyleProject project = jenkinsRule.createFreeStyleProject("test-project");
        BlockBuild blockBuild = new BlockBuild(project);
        
        assertNull(blockBuild.checkBuildingUpstream());
    }
    
    /**
     * Tests when blocking on upstream jobs is not enabled.
     * @throws IOException
     */
    @Test
    public void testCheckBuildingUpstream_NotEnabled() throws IOException {
        FreeStyleProject project = jenkinsRule.createFreeStyleProject("test-project");
        BlockBuildJobProperty.updateBlockBuildJobPropertyInProject(project,
                new BlockBuildJobProperty(false, "", false, ""));
        BlockBuild blockBuild = new BlockBuild(project);
        
        assertNull(blockBuild.checkBuildingUpstream());
    }
    
    /**
     * Tests when project-b does not have any upstream jobs building.
     * @throws Exception
     */
    @Test
    public void testCheckBuildingUpstream_NoUpstreamJobsAreBuilding() throws Exception {
        // Given
        // project-a is not running.
        
        // When
        BlockBuild blockBuild = new BlockBuild(projectB);
        CauseOfBlockage blockage = blockBuild.checkBuildingUpstream();
        
        // Then
        assertNull("project-b should not have any upstream jobs building.", blockage);
    }
    
    /**
     * Checks build is blocked when an upstream project is building.
     * @throws Exception
     */
    @Test
    public void testCheckBuildingUpstream_JobIsBuilding() throws Exception {
        // Given
        // Schedule A and wait for it to start running.
        projectA.scheduleBuild2(0);
        projectABuildStarted.block();
        
        // When
        BlockBuild blockBuild = new BlockBuild(projectB);
        CauseOfBlockage blockage = blockBuild.checkBuildingUpstream();
        
        // Then
        try {
            assertNotNull("CauseOfBlockage should be project-a", blockage);
            assertEquals("Upstream project project-a is already building.", blockage.getShortDescription());
        }
        finally {
            waitForLastBuildToFinish(projectB, PROJECT_B_WAIT_TIMEOUT);
        }
    }
    
    /**
     * Checks the build is blocked when an upstream project is queued to build.
     * @throws Exception
     */
    @Test
    public void testCheckBuildingUpstream_JobIsQueued() throws Exception {
        // Given
        projectA.scheduleBuild2(1);
        
        // When
        BlockBuild blockBuild = new BlockBuild(projectB);
        CauseOfBlockage blockage = blockBuild.checkBuildingUpstream();
        
        // Then
        try {
            assertNotNull("CauseOfBlockage should be project-a", blockage);
            assertEquals("Upstream project project-a is already building.", blockage.getShortDescription());
        }
        finally {
            waitForLastBuildToFinish(projectB, PROJECT_B_WAIT_TIMEOUT);
        }
    }
    
    /**
     * This is one of the main tests cases for this plugin.  It kicks off multiple jobs
     * and makes sure they correctly block.
     * 
     * Unfortunately, it doesn't work when running in Eclipse. I get the following exception stack:
     * SEVERE: Executor threw an exception
           java.lang.AssertionError: class org.jenkinsci.plugins.blockbuildfinalproject.BlockBuildJobProperty is missing its descriptor
              at jenkins.model.Jenkins.getDescriptorOrDie(Jenkins.java:1189)
              at hudson.model.JobProperty.getDescriptor(JobProperty.java:100)
              at hudson.model.JobProperty.getDescriptor(JobProperty.java:75)
              at hudson.model.Descriptor.toMap(Descriptor.java:880)
              at hudson.model.Job.getProperties(Job.java:547)
              at hudson.model.Build$BuildExecution.cleanUp(Build.java:196)
              at hudson.model.Run.execute(Run.java:1788)
              at hudson.model.FreeStyleBuild.run(FreeStyleBuild.java:43)
              at hudson.model.ResourceController.execute(ResourceController.java:98)
              at hudson.model.Executor.run(Executor.java:408)
              
       If I run mvn install, the test passes.
       
       The expected order of the builds is:
           A1 => B1 => A2 => B2
       Without this plugin, the order would be (which we don't want):
           A1 => B1 & A2 => B2
       With a locking plugin, the order would be (which we don't want either):
           A1 => A2 => B1 => B2

     * @throws Exception
     * @throws ExecutionException
     */
    @SuppressWarnings("deprecation")
    @Test
    public void testProjectBlocking() throws Exception, ExecutionException {
        // Schedule A twice in a row.
        projectA.scheduleBuild2(0);
        projectABuildStarted.block();
        projectA.scheduleBuild2(0);
        
        // Wait for the builds to finish.
        while (projectB.getBuilds().size() < 2)
            Thread.sleep(100);
        Result resultB2 = waitForLastBuildToFinish(projectB, PROJECT_B_WAIT_TIMEOUT);
        assertEquals(Result.SUCCESS, resultB2);
        
        // Get each build.
        AbstractBuild<?, ?> buildA1 = projectA.getBuildByNumber(1);
        AbstractBuild<?, ?> buildA2 = projectA.getBuildByNumber(2);
        AbstractBuild<?, ?> buildB1 = projectB.getBuildByNumber(1);
        AbstractBuild<?, ?> buildB2 = projectB.getBuildByNumber(2);
        
        // The timestamps must be in this order: A1 => B1 => A2 => B2
        long timeA1 = buildA1.getTimeInMillis();
        long durationA1 = buildA1.getDuration();
        long timeB1 = buildB1.getTimeInMillis();
        long durationB1 = buildB1.getDuration();
        long timeA2 = buildA2.getTimeInMillis();
        long durationA2 = buildA2.getDuration();
        long timeB2 = buildB2.getTimeInMillis();
        assertTrue("A1 should have ran and completed before B1 started.", timeA1 + durationA1 < timeB1);
        assertTrue("B1 should have ran and completed before A2 started.", timeB1 + durationB1 < timeA2);
        assertTrue("A2 should have ran and completed before B2 started.", timeA2 + durationA2 < timeB2);
    }

}
