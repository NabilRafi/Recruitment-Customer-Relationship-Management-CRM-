package com.recruitcrm.patterns.decorator;

/** Concrete Decorator: marks a posting as Urgent and boosts its visibility. */
public class UrgentJobDecorator extends JobPostingDecorator {
    public UrgentJobDecorator(JobPostingComponent wrapped) {
        super(wrapped);
    }

    @Override
    public String getDisplayTitle() {
        return "[URGENT HIRING] " + wrapped.getDisplayTitle();
    }

    @Override
    public int getVisibilityScore() {
        return wrapped.getVisibilityScore() + 15;
    }
}
