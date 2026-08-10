package com.recruitcrm.patterns.decorator;

/**
 * DECORATOR PATTERN — the abstract "Decorator".
 * Wraps another JobPostingComponent (which might itself already be
 * decorated) and delegates to it, letting each concrete decorator add
 * one small piece of behaviour on top.
 */
public abstract class JobPostingDecorator implements JobPostingComponent {
    protected final JobPostingComponent wrapped;

    protected JobPostingDecorator(JobPostingComponent wrapped) {
        this.wrapped = wrapped;
    }
}
