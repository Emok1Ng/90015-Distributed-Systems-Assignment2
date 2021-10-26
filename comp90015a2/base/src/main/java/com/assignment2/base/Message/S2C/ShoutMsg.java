package com.assignment2.base.Message.S2C;

import com.assignment2.base.Message.Identity;

public class ShoutMsg extends Identity {
    
    private String content;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
