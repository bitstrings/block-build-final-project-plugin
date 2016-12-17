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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import hudson.model.AbstractProject;
import hudson.model.Queue.Task;
import jenkins.model.Jenkins;

/**
 * Static class that provides wrappers around certain Jenkins functions.
 * 
 * For example, FindBugs was returning
 * NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE
 * for the following code:
 * {@code Set<Task> unblockedTasks = Jenkins.getInstance().getQueue().getUnblockedTasks();}
 * so it was put in a wrapper function that correctly checks the nulls.
 * 
 * @author Chad Rosenquist
 */
public final class JenkinsWrapper {
    private static final Logger LOGGER = Logger.getLogger(JenkinsWrapper.class.getName());

    /**
     * Returns the list of unblocked tasks in Jenkins.
     * 
     * @return list of unblocked tasks.  The list is empty if there are any null pointers.
     * 
     * This function was created because FindBugs was generating
     * NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE
     * from the following code:
     * {@code Set<Task> unblockedTasks = Jenkins.getInstance().getQueue().getUnblockedTasks();}
     */
    public static Set<Task> getUnblockedTasks() {
        Set<Task> unblockedTasks;
        try {
            unblockedTasks = Jenkins.getInstance().getQueue().getUnblockedTasks();            
        }
        catch (NullPointerException nullException) {
            unblockedTasks = new HashSet<Task>();
            LOGGER.severe("Jenkins.getInstance().getQueue().getUnblockedTasks() threw a NullPointerException.  This should never happen!");
        }
        return unblockedTasks;
    }
    
    /**
     * Returns all the AbstractProject instances in Jenkins.
     * 
     * @return list of projects.  The list is empty if there are any null pointers.
     */
    @SuppressWarnings("rawtypes") // should really be List<AbstractProject<?, ?>>
    public static List<AbstractProject> getAbstractProjects() {
        List<AbstractProject> projects;
        try {
            projects = Jenkins.getInstance().getAllItems(AbstractProject.class);
        }
        catch (NullPointerException nullException) {
            projects = new ArrayList<AbstractProject>();
            LOGGER.severe("Jenkins.getInstance().getItems(AbstractProject.class) threw a NullPointerException.  This should never happen!");
        }
        return projects;
    }
}
