package com.lagou.edu.listener;

import com.lagou.edu.dao.AccountDao;
import com.lagou.edu.factory.BeanFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * servlet监听器
 */
public class MyListener implements ServletContextListener {

    /**
     * 启动时执行
     * @param servletContextEvent
     */
    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        BeanFactory beanFactory = new BeanFactory();
        beanFactory.parseXml();
        try {
            beanFactory.scan("com.lagou.edu");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {

    }

}
