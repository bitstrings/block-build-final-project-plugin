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

import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;

import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.AutoCompletionCandidates;
import hudson.model.Item;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;

/**
 * Utility functions for autocompleting and checking project names.
 * 
 * These functions are in their own class because they aren't necessarily specific to
 * the Block Build Final Project Plugin.
 * 
 * @author Chad Rosenquist
 *
 */
public final class AutoCompleteUtils {
    @SuppressWarnings("unused")
    private static final Logger LOGGER = Logger.getLogger(AutoCompleteUtils.class.getName());
    
    /**
     * Auto completes a textbox that contains a list of projects.
     *  
     * @param userInput the text the end-user typed into the textbox
     * @return          list of projects that start with value
     * 
     * For example, the end-user types "cool" into the text box.
     * The following projects will be returned:
     * cool-app-build
     * cool-app-test
     */
    public static AutoCompletionCandidates autoCompleteProjects(String userInput) {
        String prefix = Util.fixNull(userInput);
        @SuppressWarnings("rawtypes")
        List<AbstractProject> projects = JenkinsWrapper.getAbstractProjects();
        if (projects == null) {
            return new AutoCompletionCandidates(); 
        }
        AutoCompletionCandidates autoCandidates = new AutoCompletionCandidates();

        for (AbstractProject<?, ?> project : projects) {
            String projectName = project.getFullName();
            if (projectName.toLowerCase().startsWith(prefix.toLowerCase())) {
                autoCandidates.add(projectName);
            }
        }
        
        return autoCandidates;        
    }
    
    /**
     * Checks the projects the end-user entered into a textbox are valid.
     * 
     * @param userInput       list of projects the end-user entered
     * @param currentProject  current project
     * @return                FormValidation.ok - if all the projects are valid
     *                        FormValidation.error - if any of the projects are invalid or not buildable
     */
    public static FormValidation checkProjects(String userInput, AbstractProject<?, ?> currentProject) {
        String[] projects = Util.fixNull(userInput).trim().split("\\s*,\\s*");
        
        for (String project : projects) {
            if (StringUtils.isNotEmpty(project)) {
                Jenkins jenkins = Jenkins.getInstance();
                assert jenkins != null;
                Item item = jenkins.getItem(project, currentProject, Item.class);
                if(item==null) {
                    return FormValidation.error("Invalid project: " + project + " | " + userInput);
                }
                if(!(item instanceof AbstractProject)) {
                    return FormValidation.error("Project is not buildable: " + project);
                }
            }
        }

        return FormValidation.ok();        
    }
}
