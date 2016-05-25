package com.hp.autonomy.frontend.find.hod.authentication;


import com.autonomy.aci.client.services.AciErrorException;
import com.hp.autonomy.hod.client.api.authentication.tokeninformation.UserInformation;
import com.hp.autonomy.hod.client.api.userstore.user.Account;
import com.hp.autonomy.hod.sso.SecurityInfoRetriever;
import com.hp.autonomy.user.UserRoles;
import com.hp.autonomy.user.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
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
                    try {
                        final UserRoles userRoles = userService.getUser(account.getAccount());
                        securityInfo = userRoles.getSecurityInfo();
                        log.debug("User {} found in community, retrieved security info was {}null", account.getAccount(), securityInfo == null ? "": "not ");
                        break;
                    } catch(AciErrorException e) {
                        log.error("Community returned an AciErrorException: {}, with ErrorId: {}", e.getErrorDescription(), e.getErrorId());
                        log.error("Username was: {}", account.getAccount());
                        log.error("Stack trace", e);
                        throw e;
                    } catch(Exception e) {
                        log.error("An unhandled exception occurred while querying community", e);
                        throw e;
                    }
                }
            }
        }

        return securityInfo;
    }
}
