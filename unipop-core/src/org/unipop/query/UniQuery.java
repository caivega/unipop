package org.unipop.query;

public class UniQuery {
    private StepDescriptor stepDescriptor;

    public UniQuery(StepDescriptor stepDescriptor) {
        this.stepDescriptor = stepDescriptor;
    }

    public StepDescriptor getStepDescriptor() {
        return stepDescriptor;
    }

    @Override
    public String toString() {
        return "UniQuery{" +
                "stepDescriptor=" + stepDescriptor +
                '}';
    }
}
