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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.model.FreeStyleProject;


/**
 * Tests BlockBuildItemListener
 * 
 * Tests deleting and renaming projects.
 * 
 * @author Chad Rosenquist
 *
 */
public class BlockBuildItemListenerIntTest {
    
    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    @Before
    public void setUp() throws Exception {
    }

    /**
     * Tests BlockBuildItemListener::onDeleted()
     * 
     * @throws Exception
     */
    @Test
    public void testOnDeleted() throws Exception {
        // Given
        FreeStyleProject deletedProject = jenkinsRule.createFreeStyleProject("deleted-project");
        FreeStyleProject usesDeletedProject = jenkinsRule.createFreeStyleProject("uses-deleted-project");
        jenkinsRule.createFreeStyleProject("other-project");

        BlockBuildJobProperty.updateBlockBuildJobPropertyInProject(deletedProject,
                new BlockBuildJobProperty(true, "deleted-project", false, ""));
        BlockBuildJobProperty.updateBlockBuildJobPropertyInProject(usesDeletedProject,
                new BlockBuildJobProperty(true, "other-project, deleted-project", false, ""));
        
        // When
        deletedProject.delete();
        
        // Then
        BlockBuildJobProperty property = BlockBuildJobProperty.getBlockBuildJobPropertyFromProject(usesDeletedProject);
        assertEquals("other-project", property.getFinalUpstreamProjects());
    }
    
    @Test
    public void testOnRenamed() throws Exception {
        // Given
        FreeStyleProject renamedProject = jenkinsRule.createFreeStyleProject("old-name");
        FreeStyleProject usesRenamedProject = jenkinsRule.createFreeStyleProject("uses-renamed-project");
        jenkinsRule.createFreeStyleProject("other-project"); 
        jenkinsRule.createFreeStyleProject("another-project");
        
        BlockBuildJobProperty.updateBlockBuildJobPropertyInProject(renamedProject,
                new BlockBuildJobProperty(true, "old-name", false, ""));
        BlockBuildJobProperty.updateBlockBuildJobPropertyInProject(usesRenamedProject,
                new BlockBuildJobProperty(true, "other-project,old-name,another-project", false, ""));
        
        // When
        renamedProject.renameTo("new-name");
        
        // Then
        BlockBuildJobProperty property = BlockBuildJobProperty.getBlockBuildJobPropertyFromProject(usesRenamedProject);
        assertEquals("other-project,new-name,another-project", property.getFinalUpstreamProjects());
        
        BlockBuildJobProperty property2 = BlockBuildJobProperty.getBlockBuildJobPropertyFromProject(renamedProject);
        assertEquals("new-name", property2.getFinalUpstreamProjects());        
    }

}
