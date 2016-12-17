package org.jenkinsci.plugins.blockbuildfinalproject;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.collect.ImmutableList;

import hudson.model.AbstractProject;

/**
 * Unit tests BlockBuildJobProperty.
 * 
 * @author Chad Rosenquist
 *
 */
public class BlockBuildJobPropertyUnitTest {

    /**
     * Tests the DataBoundConstructor.
     * Verifies the projects are correct in both string and list form.
     */
    @Test
    public void testDataBoundConstructor() {
        BlockBuildJobProperty property = new BlockBuildJobProperty(
                true,
                "  project1,   project 2   ,",
                true,
                "projectA");
        assertTrue(property.isUseBlockBuildUpstreamProject());
        assertTrue(property.isUseBlockBuildDownstreamProject());
        
        assertEquals("project1,project 2", property.getFinalUpstreamProjects());
        assertEquals("projectA", property.getFinalDownstreamProjects());
        
        ImmutableList<String> upstream = property.getFinalUpstreamProjectsAsList();
        ImmutableList<String> downstream = property.getFinalDownstreamProjectsAsList();
        
        assertEquals(2, upstream.size());
        assertTrue(upstream.contains("project1"));
        assertTrue(upstream.contains("project 2"));
        
        assertEquals(1, downstream.size());
        assertTrue(downstream.contains("projectA"));
    }
    
    /**
     * Tests the list constructor.
     * Verifies the projects are correct in both string and list form.
     */
    @Test
    public void testListConstructor() {
        List<String> upstreamOrig = new ArrayList<String>();
        List<String> downstreamOrig = new ArrayList<String>();
        downstreamOrig.add("project two");
        
        BlockBuildJobProperty property = new BlockBuildJobProperty(
                false,
                upstreamOrig,
                false,
                downstreamOrig);
        
        assertFalse(property.isUseBlockBuildUpstreamProject());
        assertFalse(property.isUseBlockBuildDownstreamProject());
        
        assertEquals("", property.getFinalUpstreamProjects());
        assertEquals("project two", property.getFinalDownstreamProjects());
        
        ImmutableList<String> upstream = property.getFinalUpstreamProjectsAsList();
        ImmutableList<String> downstream = property.getFinalDownstreamProjectsAsList();
        
        assertEquals(0, upstream.size());
        assertEquals(1, downstream.size());
        assertTrue(downstream.contains("project two"));
    }
    
    /**
     * Tests deleting a project.
     */
    @Test
    public void testOnDeleted() {
        BlockBuildJobProperty property = new BlockBuildJobProperty(true, "A, B, C", false, "C, D, E");
        BlockBuildJobProperty newProperty = property.onDeleted("C");
        
        assertTrue(newProperty.isUseBlockBuildUpstreamProject());
        assertFalse(newProperty.isUseBlockBuildDownstreamProject());
        
        /* The order of projects must be preserved.  If a change goes it
         * that changes the order, this is wrong!
         */
        assertEquals("A,B", newProperty.getFinalUpstreamProjects());
        assertEquals("D,E", newProperty.getFinalDownstreamProjects());
    }
    
    /**
     * Tests deleting a project.
     * The project doesn't exist in either upstream or downstream.
     */
    @Test
    public void testOnDeleted_ProjectDoesntExist() {
        BlockBuildJobProperty property = new BlockBuildJobProperty(true, "", false, "C, D, E");
        BlockBuildJobProperty newProperty = property.onDeleted("F");
        
        assertTrue(newProperty.isUseBlockBuildUpstreamProject());
        assertFalse(newProperty.isUseBlockBuildDownstreamProject());
        
        /* The order of projects must be preserved.  If a change goes it
         * that changes the order, this is wrong!
         */
        assertEquals("", newProperty.getFinalUpstreamProjects());
        assertEquals("C,D,E", newProperty.getFinalDownstreamProjects());
    }
    
    /**
     * Tests renaming a project.
     */
    @Test
    public void testOnRenamed() {
        BlockBuildJobProperty property = new BlockBuildJobProperty(true, "A, B, C", false, "C, D, E");
        BlockBuildJobProperty newProperty = property.onRenamed("C", "F");
        
        assertTrue(newProperty.isUseBlockBuildUpstreamProject());
        assertFalse(newProperty.isUseBlockBuildDownstreamProject());
        
        /* The order of projects must be preserved.  If a change goes it
         * that changes the order, this is wrong!
         */
        assertEquals("A,B,F", newProperty.getFinalUpstreamProjects());
        assertEquals("F,D,E", newProperty.getFinalDownstreamProjects());
    }

    /**
     * Tests renaming a project.
     * The project doesn't exist in either upstream or downstream.
     */
    @Test
    public void testOnRenamed2_ProjectDoesntExist() {
        BlockBuildJobProperty property = new BlockBuildJobProperty(true, "A, B, C", false, "C, D, E");
        BlockBuildJobProperty newProperty = property.onRenamed("H", "F");
        
        assertTrue(newProperty.isUseBlockBuildUpstreamProject());
        assertFalse(newProperty.isUseBlockBuildDownstreamProject());
        
        /* The order of projects must be preserved.  If a change goes it
         * that changes the order, this is wrong!
         */
        assertEquals("A,B,C", newProperty.getFinalUpstreamProjects());
        assertEquals("C,D,E", newProperty.getFinalDownstreamProjects());
    }
    
    /**
     * Tests getBlockBuildJobPropertyFromProject() using mocks.
     */
    @Test
    public void testGetBlockBuildJobPropertyFromProject() {
        BlockBuildJobProperty testProperty = new BlockBuildJobProperty(true, "A", false, "B");
        
        /*
         * Create a mocked AbstractProject.
         * When getProperty is called on that AbstractProject, returned the test property created.
         */
        AbstractProject<?, ?> project = Mockito.mock(AbstractProject.class);
        Mockito.when(project.getProperty(BlockBuildJobProperty.class)).thenReturn(testProperty);
        
        BlockBuildJobProperty property = BlockBuildJobProperty.getBlockBuildJobPropertyFromProject(project);
        assertTrue(property.isUseBlockBuildUpstreamProject());
        assertEquals("A", property.getFinalUpstreamProjects());
    }
    
    /**
     * Tests getBlockBuildJobPropertyFromProject() with a null.
     */
    @Test
    public void testGetBlockBuildJobPropertyFromProject_Null() {
        assertNull(BlockBuildJobProperty.getBlockBuildJobPropertyFromProject(null));
    }
    
    /**
     * Tests updateBlockBuildJobPropertyInProject() using mocks.
     * @throws IOException
     */
    @Test
    public void testUpdateBlockBuildJobPropertyInProject() throws IOException {
        BlockBuildJobProperty newProperty = new BlockBuildJobProperty(false, "C", true, "");
        
        AbstractProject<?, ?> project = Mockito.mock(AbstractProject.class);
        BlockBuildJobProperty.updateBlockBuildJobPropertyInProject(project, newProperty);
        
        Mockito.verify(project, Mockito.times(1)).removeProperty(BlockBuildJobProperty.class);
        Mockito.verify(project, Mockito.times(1)).addProperty(newProperty);
        
    }
}
