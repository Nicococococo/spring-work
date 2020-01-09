package com.lagou.edu.factory;

import com.lagou.edu.annotation.*;
import com.lagou.edu.dao.AccountDao;
import com.lagou.edu.dao.impl.JdbcAccountDaoImpl;
import com.lagou.edu.service.impl.TransferServiceImpl;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.util.*;


/**
 * @author 应癫
 *
 * 工厂类，生产对象（使用反射技术）
 */
public class BeanFactory {

    /**
     * 任务一：读取解析xml，通过反射技术实例化对象并且存储待用（map集合）
     * 任务二：对外提供获取实例对象的接口（根据id获取）
     */

    private static Map<String,Object> singletonObjects = new HashMap<>();  // 存储对象

    /**
     * 扫描包下注解
     * @param basePackage 包路径
     */
    public void scan(String basePackage) throws Exception {

        ArrayList<String> fileNameList = new ArrayList<>();
        //项目根路径
        String rootPath = null;
        try {
            rootPath = URLDecoder.decode(this.getClass().getResource("/").getPath(),"UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        String  basePackagePath =basePackage.replaceAll("\\.","\\\\");
        //获取目录下所有文件
        getAllFileName(rootPath+"//"+basePackagePath,basePackagePath,fileNameList);

        for (String name : fileNameList) {

            name=name.replaceAll(".class","");

            try {

                Class clazz =  Class.forName(name);

                if(!clazz.isAnnotation()){

                    //判断是否加了注解注入ioc容器
                    if(clazz.isAnnotationPresent(Component.class)) {

                        Component component = (Component) clazz.getAnnotation(Component.class);

                        Object o = clazz.newInstance();

                        String id = "".equals(component.value()) ? toLowerCaseFirstOne(o.getClass().getSimpleName()): component.value();

                        singletonObjects.put(id,o);

                    }else if(clazz.isAnnotationPresent(Controller.class)){

                        Controller component = (Controller) clazz.getAnnotation(Controller.class);

                        Object o = clazz.newInstance();

                        String id = "".equals(component.value()) ? toLowerCaseFirstOne(o.getClass().getSimpleName()): component.value();

                        singletonObjects.put(id,o);

                    }else if(clazz.isAnnotationPresent(Service.class)){

                        Service component = (Service) clazz.getAnnotation(Service.class);

                        Object o = clazz.newInstance();

                        String id = "".equals(component.value()) ? toLowerCaseFirstOne(o.getClass().getSimpleName()): component.value();

                        singletonObjects.put(id,o);

                    }else if(clazz.isAnnotationPresent(Repository.class)){

                        Repository component = (Repository) clazz.getAnnotation(Repository.class);

                        Object o = clazz.newInstance();

                        String id = "".equals(component.value()) ? toLowerCaseFirstOne(o.getClass().getSimpleName()): component.value();

                        singletonObjects.put(id,o);

                    }

                }


            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //依赖注入
        Iterator<Map.Entry<String, Object>> it = singletonObjects.entrySet().iterator();

        while(it.hasNext()){

            Map.Entry<String, Object> singletonObject = it.next();

            Object o = singletonObject.getValue();

            //获取所有字段
            Field[] fields = o.getClass().getDeclaredFields();
            //遍历字段
            for (Field field : fields) {

                //如果加了@Autowired，进行依赖注入
                if(field.isAnnotationPresent(Autowired.class)) {

                    Object attrObj = null;

                    //ByName：如果加了@Qualifier，根据其value值进行注入
                    //ByType：否则根据其类型进行注入
                    if(field.isAnnotationPresent(Qualifier.class)) {

                        Qualifier qualifier = field.getAnnotation(Qualifier.class);

                        String name = qualifier.value();

                        name = "".equals(name) ? field.getName() : name;
                        //获取注入的属性
                        attrObj = singletonObjects.get(name);

                    }else{
                        //用于查找出几个实现类
                        int i = 0;
                        for (Object value : singletonObjects.values()) {

                            if(field.getType().isAssignableFrom(value.getClass())){
                                attrObj = value;
                                i++;
                            }
                        }
                        //实现类>1,无法按照类型注入，抛出异常
                        if(i > 1) {
                            throw new Exception("多个实现类，无法按类型注入，请使用@Qualifier标明name");
                        }
                    }

                    field.setAccessible(true);
                    try {
                        //注入属性
                        field.set(o,attrObj);

                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }

            //如果标记事务注解@Transactional  生成代理类
            if(o.getClass().isAnnotationPresent(Transactional.class)){

                ProxyFactory proxyFactory = (ProxyFactory) BeanFactory.getBean("proxyFactory");

                Class<?> clazz = o.getClass();
                Class<?>[] interfaces = clazz.getInterfaces();
                //实现了接口使用jdk动态代理，非实现接口的类使用cglib动态代理
                if(interfaces.length > 0) {
                    singletonObject.setValue(proxyFactory.getJdkProxy(o));
                }else {
                    singletonObject.setValue(proxyFactory.getCglibProxy(o));
                }

            }

        }

    }

    /**
     * 递归获取目录下所有文件
     * @param path 全路径
     * @param basePackagePath 包路径
     * @param fileNameList  返回文件名集合
     */
    public void getAllFileName(String path, String basePackagePath, ArrayList<String> fileNameList) {

        File file = new File(path);

        File[] tempList = file.listFiles();

        for (int i = 0; i < tempList.length; i++) {
            //文件
            if (tempList[i].isFile()) {

                String filePath = tempList[i].toString();

                String classPath = filePath.substring(filePath.indexOf(basePackagePath, 0));

                classPath = classPath.replace('\\','.');
                //获取类名称添加到集合
                fileNameList.add(classPath);
            }
            //文件夹
            if (tempList[i].isDirectory()) {
                //递归调用
                getAllFileName(tempList[i].getAbsolutePath(),basePackagePath,fileNameList);
            }
        }
        return;
    }

    /**
     * 格式化名称（首字母大写转小写）
     * @param s
     * @return
     */
    public String toLowerCaseFirstOne(String s){

        if(Character.isLowerCase(s.charAt(0))) {

            return s;
        } else {

            return (new StringBuilder()).append(Character.toLowerCase(s.charAt(0))).append(s.substring(1)).toString();
        }

    }


    /**
     * 解析xml
     */
    public  void parseXml(){

        // 任务一：读取解析xml，通过反射技术实例化对象并且存储待用（map集合）
        // 加载xml
        InputStream resourceAsStream = BeanFactory.class.getClassLoader().getResourceAsStream("beans.xml");
        // 解析xml
        SAXReader saxReader = new SAXReader();
        try {
            Document document = saxReader.read(resourceAsStream);
            Element rootElement = document.getRootElement();
            List<Element> beanList = rootElement.selectNodes("//bean");
            for (int i = 0; i < beanList.size(); i++) {
                Element element =  beanList.get(i);
                // 处理每个bean元素，获取到该元素的id 和 class 属性
                String id = element.attributeValue("id");        // accountDao
                String clazz = element.attributeValue("class");  // com.lagou.edu.dao.impl.JdbcAccountDaoImpl
                // 通过反射技术实例化对象
                Class<?> aClass = Class.forName(clazz);
                Object o = aClass.newInstance();  // 实例化之后的对象

                // 存储到map中待用
                singletonObjects.put(id,o);

            }

            // 实例化完成之后维护对象的依赖关系，检查哪些对象需要传值进入，根据它的配置，我们传入相应的值
            // 有property子元素的bean就有传值需求
            List<Element> propertyList = rootElement.selectNodes("//property");
            // 解析property，获取父元素
            for (int i = 0; i < propertyList.size(); i++) {
                Element element =  propertyList.get(i);   //<property name="AccountDao" ref="accountDao"></property>
                String name = element.attributeValue("name");
                String ref = element.attributeValue("ref");

                // 找到当前需要被处理依赖关系的bean
                Element parent = element.getParent();

                // 调用父元素对象的反射功能
                String parentId = parent.attributeValue("id");
                Object parentObject = singletonObjects.get(parentId);
                // 遍历父对象中的所有方法，找到"set" + name
                Method[] methods = parentObject.getClass().getMethods();
                for (int j = 0; j < methods.length; j++) {
                    Method method = methods[j];
                    if(method.getName().equalsIgnoreCase("set" + name)) {  // 该方法就是 setAccountDao(AccountDao accountDao)
                        method.invoke(parentObject,singletonObjects.get(ref));
                    }
                }

                // 把处理之后的parentObject重新放到map中
                singletonObjects.put(parentId,parentObject);

            }

        } catch (DocumentException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    // 任务二：对外提供获取实例对象的接口（根据id获取）
    public static  Object getBean(String id) {
        return singletonObjects.get(id);
    }

}
