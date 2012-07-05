package org.jbpm.simulation.handler;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.bpmn2.Activity;
import org.eclipse.bpmn2.BoundaryEvent;
import org.eclipse.bpmn2.FlowElement;
import org.eclipse.bpmn2.SequenceFlow;
import org.jbpm.simulation.PathContext;
import org.jbpm.simulation.PathContextManager;

public class ActivityElementHandler extends MainElementHandler {

    public void handle(FlowElement element, PathContextManager manager) {
        PathContext context = manager.getContextFromStack();
        List<SequenceFlow> outgoing = getOutgoing(element);
        List<BoundaryEvent> bEvents = ((Activity) element).getBoundaryEventRefs();
        if (bEvents != null && bEvents.size() > 0) {

            for (BoundaryEvent bEvent : bEvents) {
                manager.addToPath(bEvent, context);
                outgoing.addAll(bEvent.getOutgoing());

            }
            handleSeparatePaths(outgoing, manager);
            handleCombinedPaths(outgoing, manager);
            handleAllPaths(outgoing, manager);
        } else {
            HandlerRegistry.getHandler().handle(element, manager);
        }
    }

    protected void handleSeparatePaths(List<SequenceFlow> outgoing, PathContextManager manager) {
        List<PathContext> locked = new ArrayList<PathContext>();
        PathContext context = manager.getContextFromStack();
        for (SequenceFlow seqFlow : outgoing) {
            FlowElement target = seqFlow.getTargetRef();

            PathContext separatePath = manager.cloneGiven(context);
            manager.addToPath(seqFlow, separatePath);
            super.handle(target, manager);
            separatePath.setLocked(true);

            locked.add(separatePath);
        }

        // unlock
        for (PathContext ctx : locked) {
            ctx.setLocked(false);
        }
    }
    
    protected void handleAllPaths(List<SequenceFlow> outgoing, PathContextManager manager) {
        PathContext context = manager.getContextFromStack();
        context.setCanBeFinished(false);
        int counter = 0;
        for (SequenceFlow seqFlow : outgoing) {
            counter++;
            FlowElement target = seqFlow.getTargetRef();

            if (counter == outgoing.size()) {
                context.setCanBeFinished(true);
            }
            manager.addToPath(seqFlow, context);
            super.handle(target, manager);
        }
    }
    
    protected void handleCombinedPaths(List<SequenceFlow> outgoing, PathContextManager manager) {
 
        // next cover all combinations of paths
        if (outgoing.size() > 2) {
            List<SequenceFlow> copy = new ArrayList<SequenceFlow>(outgoing);
            List<SequenceFlow> andCombination = null;
            for (SequenceFlow flow : outgoing) {

                // first remove one that we currently processing as that is not
                // a combination
                copy.remove(flow);

                for (SequenceFlow copyFlow : copy) {
                    manager.cloneGiven(manager.getContextFromStack());


                    andCombination = new ArrayList<SequenceFlow>();
                    andCombination.add(flow);
                    andCombination.add(copyFlow);

                    handleAllPaths(andCombination, manager);
                }
            }
        }

    }
}
