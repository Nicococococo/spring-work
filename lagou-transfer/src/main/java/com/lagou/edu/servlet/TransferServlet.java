package com.lagou.edu.servlet;

import com.lagou.edu.annotation.Autowired;
import com.lagou.edu.annotation.Controller;
import com.lagou.edu.annotation.Qualifier;
import com.lagou.edu.factory.BeanFactory;
import com.lagou.edu.factory.ProxyFactory;
import com.lagou.edu.service.TestService;
import com.lagou.edu.service.impl.TransferServiceImpl;
import com.lagou.edu.utils.JsonUtils;
import com.lagou.edu.pojo.Result;
import com.lagou.edu.service.TransferService;

import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * @author 应癫
 */
@WebServlet(name="transferServlet",urlPatterns = "/transferServlet")
@Controller
public class TransferServlet extends HttpServlet {

    @Autowired
    @Qualifier("transferService")
    private TransferService transferService;
    @Autowired
    @Qualifier("testService")
    private TestService testService;

    @Override
    public void init() throws ServletException {
        transferService = (TransferService) BeanFactory.getBean("transferService");
        testService = (TestService) BeanFactory.getBean("testService");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req,resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        // 设置请求体的字符编码
        req.setCharacterEncoding("UTF-8");

        String fromCardNo = req.getParameter("fromCardNo");
        String toCardNo = req.getParameter("toCardNo");
        String moneyStr = req.getParameter("money");
        int money = Integer.parseInt(moneyStr);

        Result result = new Result();

        try {

            // 1. service层实现接口的情况
            transferService.transfer(fromCardNo,toCardNo,money);
            // 2.service层未实现接口的情况
            //testService.transfer(fromCardNo,toCardNo,money);

            result.setStatus("200");
        } catch (Exception e) {
            e.printStackTrace();
            result.setStatus("201");
            result.setMessage(e.toString());
        }

        // 响应
        resp.setContentType("application/json;charset=utf-8");
        resp.getWriter().print(JsonUtils.object2Json(result));
    }
}
