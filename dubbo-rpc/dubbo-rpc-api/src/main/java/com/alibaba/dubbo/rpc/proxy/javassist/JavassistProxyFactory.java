/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.rpc.proxy.javassist;

import java.lang.reflect.Method;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.bytecode.Proxy;
import com.alibaba.dubbo.common.bytecode.Wrapper;
import com.alibaba.dubbo.common.utils.ReflectUtils;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.proxy.AbstractProxyFactory;
import com.alibaba.dubbo.rpc.proxy.AbstractProxyInvoker;
import com.alibaba.dubbo.rpc.proxy.InvokerInvocationHandler;

/**
 * JavaassistRpcProxyFactory 

 * @author william.liangf
 */
public class JavassistProxyFactory extends AbstractProxyFactory {

    @SuppressWarnings("unchecked")
    public <T> T getProxy(Invoker<T> invoker, Class<?>[] interfaces) {
        return (T) Proxy.getProxy(interfaces).newInstance(new InvokerInvocationHandler(invoker));
    }

    public <T> Invoker<T> getInvoker(T proxy, Class<T> type, URL url) {
        // TODO Wrapper类不能正确处理带$的类名
        final Wrapper wrapper = Wrapper.getWrapper(proxy.getClass().getName().indexOf('$') < 0 ? proxy.getClass() : type);
        final String config = url.getParameter("interfaces");
        return new AbstractProxyInvoker<T>(proxy, type, url) {
            @Override
            protected Object doInvoke(T proxy, String methodName, 
                                      Class<?>[] parameterTypes, 
                                      Object[] arguments) throws Throwable {
                Object obj = null;
                Exception exception = null;
                
                try {
                    obj = wrapper.invokeMethod(proxy, methodName, parameterTypes, arguments);
                    return obj;
                } catch (Exception e) {
                    exception = e;
                }
                
                if (obj == null ){
                    
                    if (config != null && config.length() > 0) {
                        String[] types = Constants.COMMA_SPLIT_PATTERN.split(config);
                        if (types != null && types.length > 0) {
                            
                            for (int i = 0; i < types.length; i ++) {
                                Class<?> interfaces = ReflectUtils.forName(types[i]);
                                try {
                                    if (methodName.equals("getInvocationHandler")) {
                                        Method method = interfaces.getMethod(methodName, parameterTypes);
                                        return  method.invoke(proxy, arguments);                                
                                    }
                                } catch (NoSuchMethodException e) {
                                    
                                } catch (SecurityException e){
                                    
                                }
                            }
                        }
                    }                    
                }
                              
                if (obj == null && exception != null ) {
                    throw exception;
                }
                
                return obj;
            }
        };
    }

}