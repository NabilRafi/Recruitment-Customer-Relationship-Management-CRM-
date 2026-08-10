package com.recruitcrm.patterns.decorator;

/** DECORATOR PATTERN — the "Component" interface. */
public interface JobPostingComponent {
    String getDisplayTitle();
    int getVisibilityScore();
}
