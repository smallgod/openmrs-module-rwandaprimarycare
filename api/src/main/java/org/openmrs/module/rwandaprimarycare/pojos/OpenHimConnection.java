package org.openmrs.module.rwandaprimarycare.pojos;

public class OpenHimConnection {

    private String openhimUrl;
    private String openhimClientId;
    private String openhimPassword;
    private String loginUserName;
    private String loginUserPassword;
    private String status;

    public String getOpenhimUrl() {
        return openhimUrl;
    }

    public void setOpenhimUrl(String openhimUrl) {
        this.openhimUrl = openhimUrl;
    }

    public String getOpenhimClientId() {
        return openhimClientId;
    }

    public void setOpenhimClientId(String openhimClientId) {
        this.openhimClientId = openhimClientId;
    }

    public String getOpenhimPassword() {
        return openhimPassword;
    }

    public void setOpenhimPassword(String openhimPassword) {
        this.openhimPassword = openhimPassword;
    }

    public String getLoginUserName() {
        return loginUserName;
    }

    public void setLoginUserName(String loginUserName) {
        this.loginUserName = loginUserName;
    }

    public String getLoginUserPassword() {
        return loginUserPassword;
    }

    public void setLoginUserPassword(String loginUserPassword) {
        this.loginUserPassword = loginUserPassword;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
