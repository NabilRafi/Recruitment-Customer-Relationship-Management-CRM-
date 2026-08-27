package com.recruitcrm.domain;

import java.io.Serializable;


public interface UserAccount extends Serializable {
    String getName();
    String getEmail();
    String getRole();
    String describe();

    
    String getExtra();
}
