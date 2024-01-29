package com.api.customization.qc2508343.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import weaver.general.BaseBean;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class JsonUtil {

    /**
     * 获取前端传递的json数据
     * @param request
     * @return
     */
    public JSONObject getJson(HttpServletRequest request){
        String json="";
        try {
            //从前端获取输入字节流
            ServletInputStream requestInputStream = request.getInputStream();
            //将字节流转换为字符流,并设置字符编码为utf-8
            InputStreamReader ir = new InputStreamReader(requestInputStream,"utf-8");
            //使用字符缓冲流进行读取
            BufferedReader br = new BufferedReader(ir);
            //开始拼装json字符串
            String line = null;
            StringBuilder sb = new StringBuilder();
            while((line = br.readLine())!=null) {
                sb.append(line);
            }

             json = sb.toString();

        } catch (IOException e) {
           new BaseBean().writeLog("==zj==(前端数据获取错误)" + JSON.toJSONString(e));
        }
        return (JSONObject)JSONObject.parse(json);
    }
}
