package com.recruitcrm.patterns.factory;

import com.recruitcrm.domain.Company;
import com.recruitcrm.domain.UserAccount;


public class CompanyFactory implements UserAccountFactory {
    @Override
    public UserAccount createAccount(String name, String email, String extra) {
        // "extra" is the industry for a company
        return new Company(name, email, extra);
    }
}
