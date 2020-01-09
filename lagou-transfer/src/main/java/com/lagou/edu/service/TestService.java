package com.lagou.edu.service;

import com.lagou.edu.annotation.Autowired;
import com.lagou.edu.annotation.Qualifier;
import com.lagou.edu.annotation.Service;
import com.lagou.edu.annotation.Transactional;
import com.lagou.edu.dao.AccountDao;
import com.lagou.edu.pojo.Account;

/**
 * @author 应癫
 */
@Service("testService")
@Transactional
public class TestService {

    @Autowired
    private AccountDao accountDao;

    public void transfer(String fromCardNo, String toCardNo, int money) throws Exception {

        Account from = accountDao.queryAccountByCardNo(fromCardNo);
        Account to = accountDao.queryAccountByCardNo(toCardNo);

        from.setMoney(from.getMoney()-money);
        to.setMoney(to.getMoney()+money);

        accountDao.updateAccountByCardNo(to);

        //int a = 1/0;

        accountDao.updateAccountByCardNo(from);

    }

}
