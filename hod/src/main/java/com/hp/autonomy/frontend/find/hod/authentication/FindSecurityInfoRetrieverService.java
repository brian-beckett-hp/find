package com.hp.autonomy.frontend.find.hod.authentication;


import com.hp.autonomy.hod.client.api.authentication.tokeninformation.UserInformation;
import com.hp.autonomy.hod.client.api.userstore.user.Account;
import com.hp.autonomy.hod.sso.SecurityInfoRetriever;
import com.hp.autonomy.user.UserRoles;
import com.hp.autonomy.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FindSecurityInfoRetrieverService implements SecurityInfoRetriever {

    @Autowired
    private UserService userService;

    @Override
    public String getSecurityInfo(UserInformation userInformation) {
        String securityInfo = null;

        if(userInformation.getAccounts() != null) {
            for(Account account: userInformation.getAccounts()) {
                if(account.getType().equals(Account.Type.ONSITE)) {
                    final UserRoles userRoles = userService.getUser(account.getAccount());
                    securityInfo = userRoles.getSecurityInfo();
                    break;
                }
            }
        }

        return securityInfo;
    }
}
