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

import java.util.logging.Logger;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.listeners.ItemListener;

/**
 * Listens for the onDeleted and onRenamed events.
 * 
 * The projects stored in BlockBuildJobProperty as stored as strings.
 * If a project is deleted and BlockBuildJobProperty references it, it must
 * be removed from BlockBuildJobProperty.
 * If a project is renamed, the project must be updated in BlockBuildJobProperty.
 * 
 * @author Chad Rosenquist
 */
@Extension
public class BlockBuildItemListener extends ItemListener {
    private static final Logger LOGGER = Logger.getLogger(BlockBuildItemListener.class.getName());
    
    /**
     * Handles the deleted event.
     * 
     * @param item the item being deleted
     * 
     * 1. Checks that item is of type AbstractProject
     * 2. Gets the AbstractProject and name from item.
     * 3. Loops through all AbstractProjects in Jenkins.
     * 4. Do NOT update the project that is being deleted.  Doing so will
     *    cause the project to not be deleted.  It will be disabled instead.
     * 5. Get the BlockBuildJobProperty from the current project.
     * 6. Because BlockBuildJobProperty is immutable, create a new BlockBuildJobProperty, with the deleted project removed.
     * 7. Remove the old BlockBuildJobProperty from the current project and add the new one in.
     * 8. Log a message if an error.
     */
    @Override
    public void onDeleted(Item item) {
        if (item instanceof AbstractProject) {
            AbstractProject<?, ?> deletedProject = (AbstractProject<?, ?>) item;
            String oldName = deletedProject.getFullName();
            LOGGER.finest("Deleted project " + oldName);
            
            for (AbstractProject<?, ?> currentProject : JenkinsWrapper.getAbstractProjects()) {
                // Do not update the project that is being deleted.
                if (!currentProject.equals(deletedProject)) {
                    BlockBuildJobProperty blockBuildJobProperty = BlockBuildJobProperty.getBlockBuildJobPropertyFromProject(currentProject);
                    if (blockBuildJobProperty != null) {
                        LOGGER.finest("Current project = " + currentProject.getFullName());
                        BlockBuildJobProperty newProperty = blockBuildJobProperty.onDeleted(oldName);
                        BlockBuildJobProperty.updateBlockBuildJobPropertyInProject(currentProject, newProperty);
                    }
                }
            }
        }
        super.onDeleted(item);
    }
    
    /**
     * Handles the renamed event.
     * 
     * @param item    the item being renamed
     * @param oldName the old name of the item
     * @param newName the new name of the item
     * 
     * 1. Checks that item is of type AbstractProject
     * 2. Gets the AbstractProject and name from item.
     * 3. Loops through all AbstractProjects in Jenkins.
     * 4. Get the BlockBuildJobProperty from the current project.
     * 5. Because BlockBuildJobProperty is immutable, create a new BlockBuildJobProperty, with the project renamed.
     * 6. Remove the old BlockBuildJobProperty from the current project and add the new one in.
     * 7. Log a message if an error.
     */
    @Override
    public void onRenamed(Item item, String oldName, String newName) {
        if (item instanceof AbstractProject) {
            LOGGER.finest("Renamed project " + oldName + " to " + newName);
            
            for (AbstractProject<?, ?> currentProject : JenkinsWrapper.getAbstractProjects()) {
                BlockBuildJobProperty blockBuildJobProperty = BlockBuildJobProperty.getBlockBuildJobPropertyFromProject(currentProject);
                if (blockBuildJobProperty != null) {
                    LOGGER.finest("Current project = " + currentProject.getFullName());
                    BlockBuildJobProperty newProperty = blockBuildJobProperty.onRenamed(oldName, newName);
                    BlockBuildJobProperty.updateBlockBuildJobPropertyInProject(currentProject, newProperty);
                }
            }
        }
        super.onRenamed(item, oldName, newName);
    }
}
