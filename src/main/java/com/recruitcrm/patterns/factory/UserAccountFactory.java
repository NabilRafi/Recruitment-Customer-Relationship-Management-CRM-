package com.recruitcrm.patterns.factory;

import com.recruitcrm.domain.UserAccount;


public interface UserAccountFactory {
    UserAccount createAccount(String name, String email, String extra);
}
