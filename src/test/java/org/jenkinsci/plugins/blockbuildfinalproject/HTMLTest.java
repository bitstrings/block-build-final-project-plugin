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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;

import hudson.model.FreeStyleProject;

/**
 * Performs a few simple tests that ensure this plugin shows up when configuring a project.
 * 
 * @author Chad Rosenquist
 *
 */
public class HTMLTest {
    private static final String TEST_PROPERTIES =
            "useBlockBuildUpstreamProject,finalUpstreamProjects,useBlockBuildDownstreamProject,finalDownstreamProjects";
    
    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    @Before
    public void setUp() throws Exception {
    }
    
    /**
     * Tests the default values are correctly populated.
     * 
     * @throws Exception
     */
    @Test
    public void testDefaults() throws Exception {
        // Given
        FreeStyleProject project = jenkinsRule.createFreeStyleProject("defaults-project");
        BlockBuildJobProperty before = new BlockBuildJobProperty(false, "", false, "");
        BlockBuildJobProperty.updateBlockBuildJobPropertyInProject(project, before);
        
        // When
        HtmlPage page = jenkinsRule.createWebClient().getPage(project, "configure");
        HtmlForm form = page.getFormByName("config");
        jenkinsRule.submit(form);
        
        // Then
        BlockBuildJobProperty after = BlockBuildJobProperty.getBlockBuildJobPropertyFromProject(project);
        BlockBuildJobProperty expected = new BlockBuildJobProperty(false, "", false, "");
        jenkinsRule.assertEqualBeans(expected, after, TEST_PROPERTIES);
    }

    /**
     * Enters values into the checkboxes and text fields.
     * 
     * @throws Exception
     */
    @Test
    public void test() throws Exception {
        // Given
        FreeStyleProject project = jenkinsRule.createFreeStyleProject("test-project");
        jenkinsRule.createFreeStyleProject("upstream-project");
        jenkinsRule.createFreeStyleProject("downstream-project");
        BlockBuildJobProperty before = new BlockBuildJobProperty(false, "", false, "");
        BlockBuildJobProperty.updateBlockBuildJobPropertyInProject(project, before);
        
        // When
        //HtmlPage page = jenkinsRule.createWebClient().goTo("job/test-project/configure");
        HtmlPage page = jenkinsRule.createWebClient().getPage(project, "configure");
        
        // Check the upstream and downstream check boxes.
        DomElement upstreamCheckBox = page.getElementByName("useBlockBuildUpstreamProject");
        upstreamCheckBox.click();
        DomElement downstreamCheckBox = page.getElementByName("useBlockBuildDownstreamProject");
        downstreamCheckBox.click();
        
        // Enter a upstream and downstream projects.
        HtmlForm form = page.getFormByName("config");
        HtmlTextInput upstreamTextBox = form.getInputByName("_.finalUpstreamProjects");
        upstreamTextBox.setValueAttribute("upstream-project");
        HtmlTextInput downstreamTextBox = form.getInputByName("_.finalDownstreamProjects");
        downstreamTextBox.setValueAttribute("downstream-project");
        
        jenkinsRule.submit(form);
        
        // Then
        BlockBuildJobProperty after = BlockBuildJobProperty.getBlockBuildJobPropertyFromProject(project);
        BlockBuildJobProperty expected = new BlockBuildJobProperty(true, "upstream-project", true, "downstream-project");
        jenkinsRule.assertEqualBeans(expected, after, TEST_PROPERTIES);
    }

}
