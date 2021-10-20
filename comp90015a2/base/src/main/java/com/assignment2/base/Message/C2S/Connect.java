package com.assignment2.base.Message.C2S;

import com.assignment2.base.Message.Base;

public class Connect extends Base {

    private String port;
    private String ip;

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }
}
