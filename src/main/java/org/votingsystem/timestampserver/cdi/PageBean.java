package org.votingsystem.timestampserver.cdi;

import org.votingsystem.util.Messages;

import javax.ejb.AccessTimeout;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Helper class to hold variables and methods needed by some web pages
 *
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Named("pageBean")
@SessionScoped
@AccessTimeout(value = 10, unit = TimeUnit.MINUTES)
public class PageBean implements Serializable {

    private static final Logger log = Logger.getLogger(PageBean.class.getName());

    public String applicationCodeMsg(String applicationCodeStr) {
        return Messages.currentInstance().get(applicationCodeStr);
    }

}