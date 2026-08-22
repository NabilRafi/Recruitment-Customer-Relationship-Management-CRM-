package com.recruitcrm.patterns.decorator;


public class FeaturedJobDecorator extends JobPostingDecorator {
    public FeaturedJobDecorator(JobPostingComponent wrapped) {
        super(wrapped);
    }

    @Override
    public String getDisplayTitle() {
        return "[FEATURED] " + wrapped.getDisplayTitle();
    }

    @Override
    public int getVisibilityScore() {
        return wrapped.getVisibilityScore() + 20;
    }
}
