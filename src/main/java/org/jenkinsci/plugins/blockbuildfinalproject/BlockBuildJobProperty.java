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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import com.google.common.collect.ImmutableList;

import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.AutoCompletionCandidates;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.util.FormValidation;

/**
 * BlockBuildJobProperty is an immutable class that holds the project properties for this plugin.
 * @author Chad Rosenquist
 *
 */
public class BlockBuildJobProperty extends JobProperty<Job<?,?>> {
    private static final Logger LOGGER = Logger.getLogger(BlockBuildJobProperty.class.getName());
    
    // true if blocking on upstream projects is enabled
    private final boolean useBlockBuildUpstreamProject;
    
    // true if blocking on downstream projects is enabled
    private final boolean useBlockBuildDownstreamProject;
    
    // immutable list of final upstream projects
    private final ImmutableList<String> finalUpstreamProjectsList;
    
    // immutable list of final downstream projects
    private final ImmutableList<String> finalDownstreamProjectsList;
    
    /**
     * DataBoundConstruct
     * Created by Jenkins to store the properties for this plugin.
     * 
     * @param useBlockBuildUpstreamProject   true if blocking on upstream projects is enabled
     * @param finalUpstreamProjects      comma separated list of final projects 
     * @param useBlockBuildDownstreamProject true if blocking on downstream projects is enabled
     * @param finalDownstreamProjects    comma separated list of final projects
     */
    @DataBoundConstructor
    public BlockBuildJobProperty(
            boolean useBlockBuildUpstreamProject,
            String finalUpstreamProjects,
            boolean useBlockBuildDownstreamProject,
            String finalDownstreamProjects) {
        this.useBlockBuildUpstreamProject = useBlockBuildUpstreamProject;
        this.useBlockBuildDownstreamProject = useBlockBuildDownstreamProject;
        
        finalUpstreamProjectsList = projectsAsStringToImmutableList(finalUpstreamProjects);
        finalDownstreamProjectsList = projectsAsStringToImmutableList(finalDownstreamProjects);
    }
    
    /**
     * Convert projects, which are a comma delimited string, into an immutable lists.
     * 
     * @param projectsAsString list of projects, comma delimited string
     * @return                 list of projects, immutable list
     */
    private ImmutableList<String> projectsAsStringToImmutableList(String projectsAsString) {
        ImmutableList.Builder<String> builder = new ImmutableList.Builder<String>();
        String[] projects = Util.fixNull(projectsAsString).trim().split("\\s*,\\s*");
        for (String project : projects) {
            if (StringUtils.isNotEmpty(project)) {
                builder.add(project);
            }
        }
        return builder.build();
    }
    
    /**
     * Constructor
     * Similar to the DataBoundConstructor, but takes a List instead of a string for the final projects.
     * 
     * @param useBlockBuildUpstreamProject    true if blocking on upstream projects is enabled
     * @param finalUpstreamProjectsList   List of final projects 
     * @param useBlockBuildDownstreamProject  true if blocking on downstream projects is enabled
     * @param finalDownstreamProjectsList List of final projects
     */
    public BlockBuildJobProperty(
            boolean useBlockBuildUpstreamProject,
            List<String> finalUpstreamProjectsList,
            boolean useBlockBuildDownstreamProject,
            List<String> finalDownstreamProjectsList) {
        // Upstream
        this.useBlockBuildUpstreamProject = useBlockBuildUpstreamProject;
        ImmutableList.Builder<String> builder = new ImmutableList.Builder<String>();
        builder.addAll(finalUpstreamProjectsList);
        this.finalUpstreamProjectsList = builder.build();
        
        // Downstream
        this.useBlockBuildDownstreamProject = useBlockBuildDownstreamProject;
        builder = new ImmutableList.Builder<String>();
        builder.addAll(finalDownstreamProjectsList);
        this.finalDownstreamProjectsList = builder.build();
    }
    
    /**
     * Default Constructor
     */
    public BlockBuildJobProperty() {
        this.useBlockBuildUpstreamProject = false;
        this.useBlockBuildDownstreamProject = false;
        ImmutableList.Builder<String> builder = new ImmutableList.Builder<String>();
        finalUpstreamProjectsList = builder.build();
        builder = new ImmutableList.Builder<String>();
        finalDownstreamProjectsList = builder.build();
    }

    /**
     * @return true if this plugin should block upstream projects
     */
    public boolean isUseBlockBuildUpstreamProject() {
        return this.useBlockBuildUpstreamProject;
    }
    
    /**
     * @return false if this plugin should block downstream projects
     */
    public boolean isUseBlockBuildDownstreamProject() {
        return this.useBlockBuildDownstreamProject;
    }
    
    /**
     * @return comma delimited list of final upstream projects
     */
    public String getFinalUpstreamProjects() {
        return StringUtils.join(finalUpstreamProjectsList, ",");
    }
    
    /**
     * @return comma delimited list of final downstream projects
     */
    public String getFinalDownstreamProjects() {
        return StringUtils.join(finalDownstreamProjectsList, ",");
    }
 
    /**
     * 
     * @return ImmutableList of final upstream projects
     */
    public ImmutableList<String> getFinalUpstreamProjectsAsList() {
        if (finalUpstreamProjectsList == null) {
            // Sometimes this is null!?  How is this possible!?
            ImmutableList.Builder<String> builder = new ImmutableList.Builder<String>();
            return builder.build();
        }
        else {
            return finalUpstreamProjectsList;
        }
    }
    
    /**
     * 
     * @return ImmutableList of final downstream projects
     */
    public ImmutableList<String> getFinalDownstreamProjectsAsList() {
        if (finalDownstreamProjectsList == null) {
            // Sometimes this is null!?  How is this possible!?
            ImmutableList.Builder<String> builder = new ImmutableList.Builder<String>();
            return builder.build();
        }
        else {
            return finalDownstreamProjectsList;
        }
    }

    /**
     * When a project is deleted from Jenkins, call this function to delete that project
     * from BlockBuildJobProperty.  Because BlockBuildJobProperty is immutable, a new instance is created.
     * 
     * @param  deletedName name of the project being deleted
     * @return             new instance of BlockBuildJobProperty that does not contain the delete project
     */
    public BlockBuildJobProperty onDeleted(String deletedName) {
        List<String> upstreamProjects = deleteProjectFromList(deletedName, getFinalUpstreamProjectsAsList());
        List<String> downstreamProjects = deleteProjectFromList(deletedName, getFinalDownstreamProjectsAsList());
        
        return new BlockBuildJobProperty(
                isUseBlockBuildUpstreamProject(),
                upstreamProjects,
                isUseBlockBuildDownstreamProject(),
                downstreamProjects);
    }
    
    /**
     * Removes a deleted project from the list.
     * 
     * @param deletedName name of the delete project
     * @param inputList   immutable list of project names
     * @return            list of projects, without deletedName
     */
    private List<String> deleteProjectFromList(String deletedName, ImmutableList<String> inputList) {
        List<String> projects = new ArrayList<String>(inputList);
        
        if (projects.contains(deletedName)) {
            projects.remove(deletedName);
            LOGGER.finest("Removed project " + deletedName);
        }
        
        return projects;
    }

    /**
     * When a project is renamed in Jenkins, call this function to rename that project
     * in BlockBuildJobProperty.  Because BlockBuildJobProperty is immutable, a new instance is created.
     * @param oldName old name of the project being renamed
     * @param newName new name
     * @return        new instance of BlockBuildJobProperty with the renamed project
     */
    public BlockBuildJobProperty onRenamed(String oldName, String newName) {
        List<String> upstreamProjects = renameProjectInList(oldName, newName, getFinalUpstreamProjectsAsList());
        List<String> downstreamProjects = renameProjectInList(oldName, newName, getFinalDownstreamProjectsAsList());
        
        return new BlockBuildJobProperty(
                isUseBlockBuildUpstreamProject(),
                upstreamProjects,
                isUseBlockBuildDownstreamProject(),
                downstreamProjects);
    }
    
    /**
     * Renames a project in a list.
     * 
     * @param oldName   old name of the project being renamed
     * @param newName   new name
     * @param inputList immutable list of project names
     * @return          new list of projects
     */
    private List<String> renameProjectInList(String oldName, String newName, ImmutableList<String> inputList) {
        List<String> projectList = new ArrayList<String>();
        
        /*
         * The below loop keeps the projects in the same order.  For example:
         * Project A, Project B, Project C
         * 
         * If B is renamed to D, then the order should be:
         * Project A, Project D, Project C
         * 
         * Simply deleting Project B and then adding D to the List could change the order.
         */
        for (String project : inputList) {
            if (project.equals(oldName)) {
                projectList.add(newName);
                LOGGER.finest("Renamed project " + oldName + " to " + newName);
            }
            else {
                projectList.add(project);
            }           
        }
        
        return projectList;
    }
    
    /**
     * Returns the BlockBuildJobProperty for a given project.
     * 
     * @param project  the project
     * @return         the BlockBuildJobProperty
     */
    public static BlockBuildJobProperty getBlockBuildJobPropertyFromProject(AbstractProject<?, ?> project) {
        if (project == null) {
            return null;
        }
        else {
            return project.getProperty(BlockBuildJobProperty.class);
        }
    }
    
    /**
     * Updates the BlockBuildJobProperty for a given project.
     * 
     * @param project      the project
     * @param newProperty  the new BlockBuildJobProperty
     * 
     * If an error occurs, the stack trace is shown and an error message is logged.  Execution tries to continue.
     */
    public static void updateBlockBuildJobPropertyInProject(AbstractProject<?, ?> project, BlockBuildJobProperty newProperty) {
        try {
            project.removeProperty(BlockBuildJobProperty.class);
            project.addProperty(newProperty);
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.severe("Block Build Final Job Plugin - Could not add or delete property for " + project.getFullName());
        }
    }
    
    /**
     * Provides autocompletion and validation for the project textbox fields.
     * 
     * @author Chad Rosenquist
     *
     */
    @Extension
    public static final class DescriptorImpl extends JobPropertyDescriptor {

        @Override
        public String getDisplayName() {
            return "Block build when upstream or downstream project is building - final project";
        }

        /**
         * Returns always true as it can be used in all types of jobs.
         *
         * @param jobType the job type to be checked if this property is applicable.
         * @return true
         */
        @Override
        public boolean isApplicable(@SuppressWarnings("rawtypes") Class<? extends Job> jobType) {
            return true;
        }
        
        /**
         * Validates useBlockBuildUpstreamProject check box.
         * 
         * @param value   useBlockBuildUpstreamProject check box
         * @param context The current project 
         * @return        warning - If 'Block build when upstream project is building (Advanced Project Options)' is checked.
         *                   This plugin could interfere with this Jenkins builtin option.
         *                ok - If the Advanced Project Options is not checked.
         * 
         * In order for blockBuildWhenUpstreamBuilding() to work, the project must first be saved.
         * If the user checks the Advanced Project Options box and then checks useBlockBuildUpstreamProject, without saving,
         * a warning message will NOT be logged.
         */
        public FormValidation doCheckUseBlockBuildUpstreamProject(@QueryParameter String value, @AncestorInPath AbstractProject<?, ?> context) {
            
            if (context.blockBuildWhenDownstreamBuilding()) {
                return FormValidation.warning(
                        "Both 'Block build when downstream project is building (Advanced Project Options)' and "
                        + "'Block build when downstream project is building - final project (Block Build Final Project Plugin)' checked.  "
                        + "These options may conflict with each other.");
            }
            
            return FormValidation.ok();
        }
        
        /**
         * Validates useBlockBuildDownstreamProject check box.
         * 
         * @param value   useBlockBuildDownstreamProject check box
         * @param context The current project 
         * @return        warning - If 'Block build when downstream project is building (Advanced Project Options)' is checked.
         *                   This plugin could interfere with this Jenkins builtin option.
         *                ok - If the Advanced Project Options is not checked.
         * 
         * In order for blockBuildWhenDownstreamBuilding() to work, the project must first be saved.
         * If the user checks the Advanced Project Options box and then checks useBlockBuildDownstreamProject, without saving,
         * a warning message will NOT be logged.
         */
        public FormValidation doCheckUseBlockBuildDownstreamProject(@QueryParameter String value, @AncestorInPath AbstractProject<?, ?> context) {
            
            if (context.blockBuildWhenDownstreamBuilding()) {
                return FormValidation.warning(
                        "Both 'Block build when downstream project is building (Advanced Project Options)' and "
                        + "'Block build when downstream project is building - final project (Block Build Final Project Plugin)' checked.  "
                        + "These options may conflict with each other.");
            }
            
            return FormValidation.ok();
        }
        
        /**
         * Auto completes the list of final upstream projects.
         * 
         * @param value the text the end-user typed into the textbox
         * @return      list of projects that start with value
         */
        public AutoCompletionCandidates doAutoCompleteFinalUpstreamProjects(@QueryParameter String value) {
            return AutoCompleteUtils.autoCompleteProjects(value);
        }
       
        /**
         * Auto completes the list of final downstream projects.
         * 
         * @param value the text the end-user typed into the textbox
         * @return      list of projects that start with value
         */
        public AutoCompletionCandidates doAutoCompleteFinalDownstreamProjects(@QueryParameter String value) {
            return AutoCompleteUtils.autoCompleteProjects(value);
        }
        
        /**
         * Checks the projects the end-user entered into a textbox are valid.
         * 
         * @param value    list of projects the end-user entered
         * @param context  current project
         * @return         ok - if all the projects are valid
         *                 error - if any of the projects are invalid or not buildable
         */
        public FormValidation doCheckFinalUpstreamProjects(@QueryParameter String value, @AncestorInPath AbstractProject<?, ?> context) {
            return AutoCompleteUtils.checkProjects(value, context);
        }
        
        /**
         * Checks the projects the end-user entered into a textbox are valid.
         * 
         * @param value    list of projects the end-user entered
         * @param context  current project
         * @return         ok - if all the projects are valid
         *                 error - if any of the projects are invalid or not buildable
         */
        public FormValidation doCheckFinalDownstreamProjects(@QueryParameter String value, @AncestorInPath AbstractProject<?, ?> context) {
            return AutoCompleteUtils.checkProjects(value, context);
        }
        
    }


}
