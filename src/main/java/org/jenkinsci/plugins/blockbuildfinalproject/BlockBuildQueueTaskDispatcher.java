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
import hudson.model.Queue;
import hudson.model.queue.CauseOfBlockage;
import hudson.model.queue.QueueTaskDispatcher;

/**
 * Lists for the canRun event using QueueTaskDispatcher.
 * 
 * Whenever Jenkins is about to run an item from the queue, it calls
 * canRun() on all the QueueTaskDispatcher objects.  If any of the
 * canRun()'s return non-null, the item will not run.
 * 
 * @author Chad Rosenquist
 *
 */
@Extension
public class BlockBuildQueueTaskDispatcher extends QueueTaskDispatcher {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = Logger.getLogger(BlockBuildQueueTaskDispatcher.class.getName());

    /**
     * Determines if a job should be blocked because of an upstream or downstream project.
     * 
     * @param item the item the Queue is considering running
     * @return     null if the item can immediate run.
     *             CauseOfBlockage if the item needs to wait
     * 
     * Called whenever Queue is considering if Queue.Item is ready to execute immediately
     * (which doesn't necessarily mean that it gets executed right away
     * it's still subject to executor availability), or if it should be considered blocked.
     *
     */
    @Override
    public CauseOfBlockage canRun(Queue.Item item) {
        // Only check for blocking builds on AbstractProjects.
        if (!(item.task instanceof AbstractProject)) {
            return super.canRun(item);
        }
        
        AbstractProject<?, ?> project = (AbstractProject<?, ?>) item.task;
        CauseOfBlockage blockage = null;
        BlockBuild blockBuild = new BlockBuild(project);
        
        // Use BlockBuild object to check for upstream projects building.
        blockage = blockBuild.checkBuildingUpstream();
        if (blockage != null) {
            return blockage;
        }
        
        // Use BlockBuild object to check for downstream projects building.
        blockage = blockBuild.checkBuildingDownstream();
        if (blockage != null) {
            return blockage;
        }
        
        return super.canRun(item);
    }
}
