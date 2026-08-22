package com.recruitcrm.patterns.decorator;


public abstract class JobPostingDecorator implements JobPostingComponent {
    protected final JobPostingComponent wrapped;

    protected JobPostingDecorator(JobPostingComponent wrapped) {
        this.wrapped = wrapped;
    }
}
