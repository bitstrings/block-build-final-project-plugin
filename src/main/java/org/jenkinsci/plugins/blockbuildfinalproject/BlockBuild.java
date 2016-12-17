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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Logger;

import com.google.common.collect.ImmutableList;

import hudson.model.AbstractProject;
import hudson.model.Queue.Task;
import hudson.model.queue.CauseOfBlockage;

/**
 * Contains the main functionality that checks if a build should be blocked.
 * 
 * @author Chad Rosenquist
 *
 */
public class BlockBuild {

    private static final Logger LOGGER = Logger.getLogger(BlockBuild.class.getName());
    
    private final AbstractProject<?, ?> project;
    
    /**
     * Direction to search for transitive projects - UP or DOWN.
     * 
     * @author Chad Rosenquist
     *
     */
    private enum SearchDirection { UP, DOWN };
    
    /**
     * Constructor
     * 
     * @param project the project to check if upstream or downstream projects are building
     */
    public BlockBuild(AbstractProject<?, ?> project) {
        this.project = project;
    }
    
    /**
     * Checks if any upstream projects are building.
     * 
     * @return        null if no upstream projects are building.
     *                CauseOfBlockage if an upstream project is found to be building
     */
    public CauseOfBlockage checkBuildingUpstream() {
        BlockBuildJobProperty property = BlockBuildJobProperty.getBlockBuildJobPropertyFromProject(project);
        
        // Return if property is not found or blocking builds is not enabled for this project.
        if ((property == null) || (!(property.isUseBlockBuildUpstreamProject()))) {
            return null;
        }
        
        logMessageIfAdvancedProjectOptionsUpstream();
        
        Set<Task> unblockedTasks = JenkinsWrapper.getUnblockedTasks();
        
        /*
         * Check each upstream project.
         * If it's building or an unblocked task, then the current project should not build.
         */
        ImmutableList<String> finalProjects = property.getFinalUpstreamProjectsAsList();
        Set<AbstractProject<?, ?>> upstreamProjects = getTransitiveUpstreamProjectsFinal(finalProjects);
        
        for (AbstractProject<?, ?> upstreamProject : upstreamProjects) {
            if ((upstreamProject != project)
                && ((upstreamProject.isBuilding()) || (unblockedTasks.contains(upstreamProject)))) {
                LOGGER.fine("Blocking project " + project.getFullName() + " from building because upstream project "
                            + upstreamProject.getFullName() + " is building or unblocked.");
                return new AbstractProject.BecauseOfUpstreamBuildInProgress(upstreamProject);
            }
        }
        
        return null;
    }

    /**
     * This plugin can conflict with Jenkins' built-in functionality to block builds.
     * So log a message if both are enabled on this project.
     */
    private void logMessageIfAdvancedProjectOptionsUpstream() {
        if (project.blockBuildWhenUpstreamBuilding()) {
            LOGGER.info("The project " + project.getFullName() + " has both "
                        + "'Block build when upstream project is building (Advanced Project Options)' and "
                        + "'Block build when upstream project is building - final job (Block Build Final Project Plugin)' checked.");
        }
    }    
    
    /**
     * Checks if any downstream projects are building.
     * 
     * @return        null if no downstream projects are building.
     *                CauseOfBlockage if a downstream project is found to be building
     */
    public CauseOfBlockage checkBuildingDownstream() {
        BlockBuildJobProperty property = BlockBuildJobProperty.getBlockBuildJobPropertyFromProject(project);
        
        // Return if property is not found or blocking builds is not enabled for this project.
        if ((property == null) || (!(property.isUseBlockBuildDownstreamProject()))) {
            return null;
        }
        
        logMessageIfAdvancedProjectOptionsDownstream();
        
        Set<Task> unblockedTasks = JenkinsWrapper.getUnblockedTasks();
        
        
        /*
         * Check each downstream project.
         * If it's building or an unblocked task, then the current project should not build.
         */
        ImmutableList<String> finalProjects = property.getFinalDownstreamProjectsAsList();
        Set<AbstractProject<?, ?>> downstreamProjects = getTransitiveDownstreamProjectsFinal(finalProjects);
        
        for (AbstractProject<?, ?> downstreamProject : downstreamProjects) {
            if ((downstreamProject != project)
                && ((downstreamProject.isBuilding()) || (unblockedTasks.contains(downstreamProject)))) {
                LOGGER.fine("Blocking project " + project.getFullName() + " from building because downstream project "
                            + downstreamProject.getFullName() + " is building or unblocked.");
                return new AbstractProject.BecauseOfDownstreamBuildInProgress(downstreamProject);
            }
        }
        
        return null;
    }

    /**
     * This plugin can conflict with Jenkins' built-in functionality to block builds.
     * So log a message if both are enabled on this project.
     */
    private void logMessageIfAdvancedProjectOptionsDownstream() {
        if (project.blockBuildWhenDownstreamBuilding()) {
            LOGGER.info("The project " + project.getFullName() + " has both "
                        + "'Block build when downstream project is building (Advanced Project Options)' and "
                        + "'Block build when downstream project is building - final job (Block Build Final Project Plugin)' checked.");
        }
    }
    
    /**
     * Returns all the transitive upstream projects.  Recursion stops if a project is in finalProjects.
     * 
     * @param finalProjects list of projects to stop searching
     * @return              list of transitive upstream projects
     */
    public Set<AbstractProject<?, ?>> getTransitiveUpstreamProjectsFinal (
            ImmutableList<String> finalProjects) {
        return getTransitiveUpOrDownstreamProjectsFinal(finalProjects, SearchDirection.UP);
    }
    
    /**
     * Returns all the transitive downstream projects.  Recursion stops if a project is in finalProjects.
     * 
     * @param finalProjects list of projects to stop searching
     * @return              list of transitive downstream projects
     */
    public Set<AbstractProject<?, ?>> getTransitiveDownstreamProjectsFinal (
            ImmutableList<String> finalProjects) {
        return getTransitiveUpOrDownstreamProjectsFinal(finalProjects, SearchDirection.DOWN);
    }

    /**
     * Returns all the transitive upstream or downstream projects.  Recursion stops if a project is in finalProjects.
     * 
     * @param finalProjects list of projects to stop searching
     * @param direction     direction to search, either UP for upstream projects or DOWN for downstream projects
     * @return              list of transitive projects
     * 
     * This method is similar to DependencyGraph.getTransitiveDownstream().
     * The difference is this method will stop searching down a branch if it encounters a
     * project in the finalProjects list. 
     */
    private Set<AbstractProject<?, ?>> getTransitiveUpOrDownstreamProjectsFinal (
            ImmutableList<String> finalProjects,
            SearchDirection direction) {
        Set<AbstractProject<?, ?>> visited = new HashSet<AbstractProject<?, ?>>();
        Stack<AbstractProject<?, ?>> queue = new Stack<AbstractProject<?, ?>>();
        
        finalProjects = checkFinalProjectsForNull(finalProjects);
        
        LOGGER.finest("Finding " + direction.name().toLowerCase() + "stream projects for " + project.getFullName());
        
        queue.add(project);
   
        while(!queue.isEmpty()) {
            AbstractProject<?, ?> currentProject = queue.pop();

            @SuppressWarnings("rawtypes")
            List<AbstractProject> dependencyProjects;
            if (direction == SearchDirection.UP) {
                dependencyProjects = currentProject.getUpstreamProjects();
            }
            else {
                dependencyProjects = currentProject.getDownstreamProjects();
            }
            for (AbstractProject<?, ?> childProject : dependencyProjects) {            
                if (visited.add(childProject)) {
                    LOGGER.finest("Adding child project " + childProject.getFullName() + " to list of projects.");
                    
                    if (finalProjects.contains(childProject.getFullName())) {
                        LOGGER.finer("Final project " + childProject.getFullName() + " found."
                                      + "  Will not transervse deeper.");
                        // Note:  project is NOT added to the queue.
                    }
                    else {
                        queue.add(childProject);
                    }
                }
            }
        }
                
        return visited;
    }

    /**
     * Protect against null pointer.  If null, default to an empty list.
     * 
     * @param finalProjects list of final projects
     * @return              list of final projects
     */
    private ImmutableList<String> checkFinalProjectsForNull(ImmutableList<String> finalProjects) {
        if (finalProjects == null) {
            ImmutableList.Builder<String> builder = new ImmutableList.Builder<String>();
            return builder.build();
        }
        else {
            return finalProjects;
        }
    }
}
