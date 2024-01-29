package com.api.customization.zj0601.util;

import com.engine.systeminfo.bean.ApplicationOfCornerMarker;
import com.engine.systeminfo.dto.AppDTO;
import com.engine.systeminfo.service.ApplicationOfCornerMarkerService;
import com.engine.workflow.publicApi.impl.WorkflowPAImpl;
import weaver.general.BaseBean;
import weaver.hrm.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * qc#1543238 移动角标实现类
 * <a href="https://e-cloudstore.com/doc.html?appId=234f2f6ec0924ee699e242ce9808471d">参考文档</a>
 */
public class WFCornerMarkerCFImpl implements ApplicationOfCornerMarkerService {//必须实现该接口

    /**
     * 是否为数字字符串
     */
//    public static final String NUMBER_TEXT = "^([0-9]+)$";

    /**
     * 计算获得待办数量
     * <p>原生待办接口：/api/system/appmanage/corner</p>
     * @see com.engine.systeminfo.web.AppManageAction#corner(HttpServletRequest, HttpServletResponse)
     * 获得自定义列表数据
     * <p>接口地址：/api/portal/element/workflowtab</p>
     * @see com.engine.portal.web.ElementsAction#getWorkflowTabDataJson(HttpServletRequest, HttpServletResponse)
     * 设置组成元素
     * <p>接口地址：/api/portal/setting/saveSetting</p>
     * @see com.engine.portal.web.ElementSettingAction#saveElementSettingJson(HttpServletRequest, HttpServletResponse)
     * @see com.engine.portal.web.ElementSettingAction#getElementSettingJson
     * 获得组成标签
     * <p>接口地址/api/mobile/portal/elements/elementJson</p>
     * @see com.api.portal.web.MobileElementsAction#getHpAllElementJson(HttpServletRequest, HttpServletResponse)
     * 获得待办数量列表
     * <p>接口地址：/api/workflow/paService/getToDoWorkflowRequestCount</p>
     * @see com.engine.workflow.web.WorkflowPAAction#getToDoWorkflowRequestCount(HttpServletRequest, HttpServletResponse)
     * @param app
     * @return
     */
    @Override
    public ApplicationOfCornerMarker compute(AppDTO app) {
        User user = app.getUser();
        WorkflowPAImpl workflowPA = new WorkflowPAImpl();
        Map<String, String> conditions = new HashMap<>();
        long requestCount = workflowPA.getRequestCount(user, "", conditions, true, true);
        int count = 1;
        if (requestCount >= 0){
             count = Integer.parseInt(requestCount+"");
        }
        new BaseBean().writeLog("==zj==(requestCount)" + requestCount);
        return new ApplicationOfCornerMarker(count);
    }

    /**
     * 获得字符串数字，如“012你好1”返回012
     */
//    public String getPrefixNumberText(String str){
//        String number = "";
//        for (int i=0; i<str.length(); i++) {
//            if(Pattern.matches(NUMBER_TEXT, str.charAt(i)+"")){
//                number+=str.charAt(i);
//            }else{
//                break;
//            }
//        }
//        return number;
//    }

}
