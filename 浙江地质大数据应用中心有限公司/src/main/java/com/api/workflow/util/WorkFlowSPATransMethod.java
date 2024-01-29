package com.api.workflow.util;

import com.alibaba.fastjson.JSONObject;
import com.api.browser.util.ConditionFactory;
import com.api.customization.zj0601.util.ColorSetUtil;
import com.api.workflow.util.ServiceUtil;
import com.engine.workflow.biz.requestList.RequestAttentionBiz;
import org.apache.commons.lang.StringUtils;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.GCONST;
import weaver.general.Util;
import weaver.general.WorkFlowTransMethod;
import weaver.hrm.User;
import weaver.system.RequestDefaultComInfo;
import weaver.systeminfo.SystemEnv;
import weaver.workflow.request.RequestBaseUtil;
import weaver.workflow.workflow.WorkflowAllComInfo;
import weaver.workflow.workflow.WorkflowConfigComInfo;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * SPA模式下反射类
 * @author liuzy 2017/01/09
 */
@Path("/workflow/wfutil")
public class WorkFlowSPATransMethod extends BaseBean {

    private WorkFlowTransMethod workFlowTransMethod = new WorkFlowTransMethod();
    private weaver.system.RequestDefaultComInfo RequestDefaultComInfo = new RequestDefaultComInfo();
    private String contextPath = Util.null2String(GCONST.getContextPath());//系统路径

    /**
     * 单个浏览框组件渲染所需参数
     */
    @GET
    @Path("/getBrowserProp")
    @Produces(MediaType.TEXT_PLAIN)
    public String getBrowserProp(@Context HttpServletRequest request, @Context HttpServletResponse response){
        boolean support = false;
        Map<String,Object> apidatas = new HashMap<String,Object>();
        try{
            String browsertype = Util.null2String(request.getParameter("browsertype"));
            if("2".equals(browsertype) || "19".equals(browsertype)
                    || "161".equals(browsertype) || "162".equals(browsertype)){
            }else{
                support = true;
                User user = ServiceUtil.getUserByRequest(request, response);
                ConditionFactory conditionFactory = new ConditionFactory(user);
                apidatas.put("browserProp", conditionFactory.generateBrowserBean(browsertype));
            }
        }catch(Exception e){
            apidatas.put("errormsg", e.toString());
            e.printStackTrace();
        }
        apidatas.put("support", support);
        return JSONObject.toJSONString(apidatas);
    }

    public String getWfNewLink(String requestname, String para2){
        return workFlowTransMethod.getWfNewLink(requestname, para2);
    }

    //    /***
//	 * E9待办、已办、我的请求列表等使用的反射方法
//	 */
    public String getWfNewLinkWithTitle3(String requestname, String para2, String requestnamenew) {
        return getWfNewLinkWithTitle3(requestname, para2, requestnamenew,false);
    }
    public String getWfNewLinkWithTitle3(String requestname, String para2, String requestnamenew,boolean showAttentionTag) {
        String[] tempStr = null;
        if(para2.startsWith("S")){
            String[] tempStr_tmp = Util.splitString(para2, "+");
            tempStr = Arrays.copyOfRange(tempStr_tmp,9,tempStr_tmp.length);
        }else{
            tempStr = Util.splitString(para2, "+");
        }
        String requestid = Util.null2String(tempStr[0]);
        String workflowid = Util.null2String(tempStr[1]);
        String nodeid = Util.null2String(tempStr[5]);
        String isremark = Util.null2String(tempStr[6]);
        int userid = Util.getIntValue(Util.null2String(tempStr[7]));

        String convertresult = this.convertRequestNameToNew(requestname, requestnamenew);
        String titleSet = new WorkflowAllComInfo().getTitleset(workflowid);
        boolean needFormat = requestnamenew == null || "".equals(requestnamenew) || "1".equals(titleSet);//仅自定义格式需要强制转换，默认格式可走requestnamenew缓存
        String requestnamelink = null;
        try {
            requestnamelink = workFlowTransMethod.getWfNewLinkWithTitle_AttentionTag2(convertresult, para2, 2, needFormat,showAttentionTag);
        } catch (Exception e) {
            e.printStackTrace();
        }

        String scope = "";
        if(para2.indexOf("scope_") > -1 && para2.indexOf("_scope") > -1) {
            scope = para2.substring(para2.indexOf("scope_"), para2.indexOf("_scope"));
            scope = scope.replace("scope_", "");
        }
        String linkageParams = "";
        //是否开启连续处理
        boolean isOpenContinuationProcess = "1".equals(RequestDefaultComInfo.getIsOpenContinnuationProcess(userid+""));
        if (isOpenContinuationProcess && "doing".equals(scope) && Util.getIntValue(requestid) > 0) {
            linkageParams += "isOpenContinuationProcess=1";
        }

        requestnamelink = this.manageSPARequestNameLink(requestnamelink, requestid, workflowid, nodeid, userid, isremark,linkageParams);
        if(requestnamelink.indexOf("?")>0&&requestnamelink.indexOf("',")>0){
            requestnamelink= requestnamelink.substring(requestnamelink.indexOf("?")+1,requestnamelink.indexOf("',"));}
        return requestnamelink;
    }
    public String getWfNewLinkWithTitle(String requestname, String para2, String requestnamenew) {
        return getWfNewLinkWithTitle(requestname, para2, requestnamenew,false);
    }

    public String getWfNewLinkWithTitle_AttentionTag(String requestname, String para2, String requestnamenew) {
        return getWfNewLinkWithTitle(requestname, para2, requestnamenew,true);
    }

    public String getWfNewLinkWithTitle(String requestname, String para2, String requestnamenew,boolean showAttentionTag) {
        new BaseBean().writeLog("==zj==(para2)" + para2);
        String[] tempStr = null;
        if(para2.startsWith("S")){
            String[] tempStr_tmp = Util.splitString(para2, "+");
            tempStr = Arrays.copyOfRange(tempStr_tmp,9,tempStr_tmp.length);
        }else{
            tempStr = Util.splitString(para2, "+");
        }
        String requestid = Util.null2String(tempStr[0]);
        String workflowid = Util.null2String(tempStr[1]);
        String nodeid = Util.null2String(tempStr[5]);
        String isremark = Util.null2String(tempStr[6]);
        int userid = Util.getIntValue(Util.null2String(tempStr[7]));

        String convertresult = this.convertRequestNameToNew(requestname, requestnamenew);
        String titleSet = new WorkflowAllComInfo().getTitleset(workflowid);
        boolean needFormat = requestnamenew == null || "".equals(requestnamenew) || "1".equals(titleSet);//仅自定义格式需要强制转换，默认格式可走requestnamenew缓存
        //列表显示标题 新增加了 自定义标题，上面2行的判断已经不合适了
        /*String convertresult = requestname;
        boolean needFormat = true;*/
        String requestnamelink = workFlowTransMethod.getWfNewLinkWithTitle_AttentionTag(convertresult, para2, needFormat,showAttentionTag);


        String scope = "";
        if(para2.indexOf("scope_") > -1 && para2.indexOf("_scope") > -1) {
            scope = para2.substring(para2.indexOf("scope_"), para2.indexOf("_scope"));
            scope = scope.replace("scope_", "");
        }
        String linkageParams = "";
        //是否开启连续处理
        boolean isOpenContinuationProcess = "1".equals(RequestDefaultComInfo.getIsOpenContinnuationProcess(userid+""));
        if (isOpenContinuationProcess && "doing".equals(scope) && Util.getIntValue(requestid) > 0) {
            linkageParams += "isOpenContinuationProcess=1";
        }

        requestnamelink = this.manageSPARequestNameLink(requestnamelink, requestid, workflowid, nodeid, userid, isremark,linkageParams);
        new BaseBean().writeLog("==zj==(getWfNewLinkWithTitle)"+requestnamelink);
        return requestnamelink;
    }

    public String getWfNewLinkWithTitle2(String requestname, String para2, String requestnamenew) throws Exception {
        return getWfNewLinkWithTitle2(requestname, para2, requestnamenew,false);
    }
    public String getWfNewLinkWithTitle_AttentionTag2(String requestname, String para2, String requestnamenew) throws Exception {
        return getWfNewLinkWithTitle2(requestname, para2, requestnamenew,true);
    }

    public String getWfNewLinkWithTitle2(String requestname, String para2, String requestnamenew,boolean showAttentionTag) throws Exception {
        String[] tempStr = null;
        if(para2.startsWith("S")){
            String[] tempStr_tmp = Util.splitString(para2, "+");
            tempStr = Arrays.copyOfRange(tempStr_tmp,9,tempStr_tmp.length);
        }else{
            tempStr = Util.splitString(para2, "+");
        }
        String requestid = Util.null2String(tempStr[0]);
        String workflowid = Util.null2String(tempStr[1]);
        String nodeid = Util.null2String(tempStr[5]);
        String isremark = Util.null2String(tempStr[6]);
        int userid = Util.getIntValue(Util.null2String(tempStr[7]));

        String convertresult = this.convertRequestNameToNew(requestname, requestnamenew);
        String titleSet = new WorkflowAllComInfo().getTitleset(workflowid);
        boolean needFormat = requestnamenew == null || "".equals(requestnamenew) || "1".equals(titleSet);//仅自定义格式需要强制转换，默认格式可走requestnamenew缓存
        String requestnamelink = workFlowTransMethod.getWfNewLinkWithTitle_AttentionTag2(convertresult, para2, 2, needFormat,showAttentionTag);

        String scope = "";
        if(para2.indexOf("scope_") > -1 && para2.indexOf("_scope") > -1) {
            scope = para2.substring(para2.indexOf("scope_"), para2.indexOf("_scope"));
            scope = scope.replace("scope_", "");
        }
        String linkageParams = "";
        //是否开启连续处理
        boolean isOpenContinuationProcess = "1".equals(RequestDefaultComInfo.getIsOpenContinnuationProcess(userid+""));
        if (isOpenContinuationProcess && "doing".equals(scope) && Util.getIntValue(requestid) > 0) {
            linkageParams += "isOpenContinuationProcess=1";
        }

        requestnamelink = this.manageSPARequestNameLink(requestnamelink, requestid, workflowid, nodeid, userid, isremark,linkageParams);
        return requestnamelink;
    }

    /***
     * E9待办、已办、我的请求列表等使用的反射方法
     */
    public String getWfNewLinkWithTitle(String requestname, String para2) {
        String[] tempStr = Util.splitString(para2, "+");
        String requestnamenew = Util.null2String(tempStr[8]);
        //String convertresult = this.convertRequestNameToNew(requestname, requestnamenew);
        //boolean needFormat = requestnamenew == null || "".equals(requestnamenew);
        //列表显示标题 新增加了 自定义标题，上面2行的判断已经不合适了
        String convertresult = requestname;
        boolean needFormat = true;
        String requestnamelink = workFlowTransMethod.getWfNewLinkWithTitle(convertresult, para2, needFormat);
        String requestid = Util.null2String(tempStr[0]);
        String workflowid = Util.null2String(tempStr[1]);
        String nodeid = Util.null2String(tempStr[5]);
        String isremark = Util.null2String(tempStr[6]);
        int userid = Util.getIntValue(Util.null2String(tempStr[7]));

        requestnamelink = this.manageSPARequestNameLink(requestnamelink, requestid, workflowid, nodeid, userid, isremark, "");
        return requestnamelink;
    }


    private String convertRequestNameToNew(String requestname, String requestnamenew){
        if("".equals(requestnamenew.trim()))
            return requestname;
        if(!requestnamenew.equals(requestname) && requestnamenew.indexOf("（")>-1 && requestnamenew.indexOf("）")>-1){
            String suffix = org.apache.commons.lang3.StringUtils.replaceOnce(requestnamenew,requestname,"").trim();	//后标题后缀
            if(!"".equals(suffix) && !suffix.equals(requestnamenew))
                return requestname+"<B>"+suffix+"</B>";
        }
        return requestnamenew;
    }

    /**
     * 流程关注列表 使用
     * @param requestname
     * @param para2
     * @return
     */
    public String getWfAttentionLinkWithTitle(String requestname, String para2) {
        String isNeedBack = null;
        if (para2.startsWith("S")) {//截取是否需要反馈参数
            String[] tempStr_tmp = Util.splitString(para2, "+");
            para2 = StringUtils.join(Arrays.copyOfRange(tempStr_tmp, 9, tempStr_tmp.length), "+");
            isNeedBack = StringUtils.join(Arrays.copyOfRange(tempStr_tmp, 1, 8), "+");
        }
        String[] tempStr = Util.splitString(para2, "+");
        String requestid = Util.null2String(tempStr[0]);
        String workflowid = Util.null2String(tempStr[1]);
        String languageid = Util.null2String(tempStr[4]);
        String userid = Util.null2String(tempStr[7]);
        String issup = Util.null2String(tempStr[15]);

        //System.out.println("=====para2:"+para2+"  issup:"+issup);
        //System.out.println("=====requestid:"+requestid+" workflowid:"+workflowid+" languageid:"+languageid+" "+"  userid:"+userid);
        String result = "";
        //督办流程
        if("1".equals(issup)){
            String sup_para = requestid+"+"+workflowid+"+"+userid+"+0+"+ languageid;//流程标题参数
            result = workFlowTransMethod.getWfNewLinkByUrger_AttentionTag(requestname,sup_para);
        }else{
            //result = this.getWfShareLinkWithTitle_AttentionTag(requestname,para2);
            result = this.getWfShareLinkWithTitle(requestname,para2,true,isNeedBack);
        }
        return result;
    }

    /**
     * 流程关注列表 使用
     * @param requestname
     * @param para2
     * @return
     * @throws Exception
     */
    public String getWfAttentionLinkWithTitle2(String requestname, String para2)throws Exception{
        String isNeedBack = null;
        if (para2.startsWith("S")) {//截取是否需要反馈参数
            String[] tempStr_tmp = Util.splitString(para2, "+");
            para2 = StringUtils.join(Arrays.copyOfRange(tempStr_tmp, 9, tempStr_tmp.length), "+");
            isNeedBack = StringUtils.join(Arrays.copyOfRange(tempStr_tmp, 1, 8), "+");
        }
        String[] tempStr = Util.splitString(para2, "+");
        String requestid = Util.null2String(tempStr[0]);
        String workflowid = Util.null2String(tempStr[1]);
        String languageid = Util.null2String(tempStr[4]);
        String userid = Util.null2String(tempStr[7]);
        String issup = Util.null2String(tempStr[15]);

        String result = "";
        //督办流程
        if("1".equals(issup)){
            String sup_para = requestid+"+"+workflowid+"+"+userid+"+0+"+ languageid;//流程标题参数
            result = workFlowTransMethod.getWfNewLinkByUrger_AttentionTag(requestname,sup_para);
        }else{
            //result = this.getWfShareLinkWithTitle_AttentionTag2(requestname,para2);
            result = this.getWfShareLinkWithTitle2(requestname,para2,true,isNeedBack);
        }
        return result;
    }

    public String getWfShareLinkWithTitle(String requestname, String para2) {
        return getWfShareLinkWithTitle(requestname, para2,false,null);
    }

    public String getWfShareLinkWithTitle_AttentionTag(String requestname, String para2) {
        return getWfShareLinkWithTitle(requestname, para2,true,null);
    }

    public String getWfShareLinkWithTitle_AttentionTag(String requestname, String para2,String paraNeedBack) {
        return getWfShareLinkWithTitle(requestname, para2,true,paraNeedBack);
    }

    public String getWfShareLinkWithTitle(String requestname, String para2,boolean showAttentionTag,String paraNeedBack) {
        String requestnamelink = workFlowTransMethod.getWfShareLinkWithTitle(requestname, para2,showAttentionTag,paraNeedBack);
        String[] tempStr = Util.splitString(para2, "+");
        String requestid = Util.null2String(tempStr[0]);
        String workflowid=Util.null2String(tempStr[1]);
        String nodeid = Util.null2String(tempStr[5]);
        String isremark = Util.null2String(tempStr[6]);
        int userid = Util.getIntValue(Util.null2String(tempStr[7]));
        String linkageParams = tempStr.length > 15 ? Util.null2String(Util.null2String(tempStr[15])) : "";

        requestnamelink = this.manageSPARequestNameLink(requestnamelink, requestid, workflowid, nodeid, userid, isremark,linkageParams);
        return requestnamelink;
    }
    public String getWfShareLinkWithTitle2(String requestname, String para2) throws Exception{
        return getWfShareLinkWithTitle2(requestname, para2,false,null);
    }

    public String getWfShareLinkWithTitle_AttentionTag2(String requestname, String para2) throws Exception{
        return getWfShareLinkWithTitle2(requestname, para2,true,null);
    }

    public String getWfShareLinkWithTitle_AttentionTag2(String requestname, String para2,String paraNeedBack) throws Exception{
        return getWfShareLinkWithTitle2(requestname, para2,true,paraNeedBack);
    }


    public String getWfShareLinkWithTitle2(String requestname, String para2,boolean showAttentionTag,String paraNeedBack) throws Exception{
        String requestnamelink = workFlowTransMethod.getWfShareLinkWithTitle2(requestname, para2,showAttentionTag,paraNeedBack);
        String[] tempStr = Util.splitString(para2, "+");
        String requestid = Util.null2String(tempStr[0]);
        String workflowid = Util.null2String(tempStr[1]);
        String nodeid = Util.null2String(tempStr[5]);
        String isremark = Util.null2String(tempStr[6]);
        int userid = Util.getIntValue(Util.null2String(tempStr[7]));
        String linkageParams = tempStr.length > 15 ? Util.null2String(Util.null2String(tempStr[15])) : "";

        requestnamelink = this.manageSPARequestNameLink(requestnamelink, requestid, workflowid, nodeid, userid, isremark,linkageParams);
        return requestnamelink;
    }

    public String manageSPARequestNameLink(String requestnamelink, String requestid, String workflowid, String nodeid, int userid, String isremark,String linkageParams){
        boolean reqRoute = ServiceUtil.judgeWfFormReqRoute(requestid, workflowid, nodeid, userid, isremark);
        if(reqRoute){
            String funstr = "openFullWindowHaveBarForWFList";
            String funstrnew = "openSPA4Single";
            int idx = requestnamelink.indexOf(funstr);
            if(idx < 0)
                return requestnamelink;
            String str1 = requestnamelink.substring(0, idx);
            String str2 = requestnamelink.substring(idx+funstr.length());
            if(!"".equals(linkageParams)) {
                String replacement = "/main/workflow/req?" + linkageParams +"&";
                requestnamelink = str1 + funstrnew + str2.replaceFirst("/workflow/request/ViewRequestForwardSPA.jsp\\?", replacement);
            } else {
                requestnamelink = str1 + funstrnew + str2.replaceFirst("/workflow/request/ViewRequestForwardSPA.jsp", "/main/workflow/req");
            }
        }
        //自定义报表中打开模板模式流程，需要将参数拼接在url中，否则权限校验无法进入报表校验逻辑
        if(!reqRoute && linkageParams.indexOf("isfromreport")>-1){
            String reqLink = "/workflow/request/ViewRequestForwardSPA.jsp\\?";
            String reqLinkNew = "/workflow/request/ViewRequestForwardSPA.jsp?" +linkageParams +"&";
            requestnamelink = requestnamelink.replaceFirst(reqLink,reqLinkNew);
        }
        return requestnamelink;
    }

    public String getprimaryKey(String name, String pars) {
        return pars;
    }


    public String getprimaryInfo(String userid, String useridall) {
        //System.out.println(userid+"--"+useridall);
        String[] tempStr = Util.splitString(useridall, "+");
        userid = Util.null2String(tempStr[0]);
        useridall = Util.null2String(tempStr[1]);
        String[] userIdall = Util.null2String(useridall).split(",");
        String returnStr = "";
        if (userIdall.length > 1) {
            String name = ""+ SystemEnv.getHtmlLabelName(6074,weaver.general.ThreadVarLanguage.getLang())+"";
            String className = "wf-center-list-mainwf";

            if (userid.equals(userIdall[0])) {
                //主账号

            } else {
                //次账号
                name = ""+ SystemEnv.getHtmlLabelName(18083,weaver.general.ThreadVarLanguage.getLang())+"";
                className = "wf-center-list-mainwf-secondary";
            }
            returnStr += "<div ><div><span class=" + className + " >" + name + "</span ></div></div>";
        }
        return returnStr;
    }


    /**
     * E9相关资源链接反射方法，临时方案(参考RequestResources.java)
     */
    public String getResDisplayHtmlSPA(String filename, String op) {
        String[] arr = op.split("\\+");
        String resid = arr[0];
        String type = arr[1];
        String requestid = arr[2];
        String userid = arr[3];
        String authLinkStr = "&authStr="+arr[4]+"&authSignatureStr="+arr[5];
        authLinkStr += "&f_weaver_belongto_userid="+userid+"&f_weaver_belongto_usertype=0";
        String result = "";
        int suffixIndex = filename.lastIndexOf(".");
        if (suffixIndex != -1) {
            //filename = filename.substring(0, suffixIndex);
        }
        int restype = Util.getIntValue(type);
        if (restype == 1) {
            result = "<a href=\"/workflow/request/ViewRequestForwardSPA.jsp?isrequest=1&isfromresource=1&requestid=" + resid +authLinkStr+ "\" class=\"reqresnameclass\" target=\"_new\">" + filename + "</a>";
        } else if (restype == 2) {
            result = "<a href=\""+ServiceUtil.docViewUrl+"?isrequest=1&id=" + resid + "&requestid=" + requestid +authLinkStr+ "\" class=\"reqresnameclass\" target=\"_new\">" + filename + "</a>";
        } else if (restype == 3) {
            result = "<a href=\""+ServiceUtil.fileViewUrl+"?isrequest=1&imagefileId=" + resid + "&requestid=" + requestid +authLinkStr+ "\" class=\"reqresnameclass\" target=\"_new\">" + filename + "</a>";
        }
        return result;
    }

    /***
     * 移动端调用
     * @param requestname
     * @param para2
     * @return
     * String para2 = "column:requestid+column:workflowid+column:viewtype+0+" + user.getLanguage()
     * 			+ "+column:nodeid+column:isremark+" + user.getUID()
     * 			+ "+column:agentorbyagentid+column:agenttype+column:isprocessed+column:userid+0+column:creater+"+userIDAll;
     *
     *
     * 	para2 = "column:requestid+column:workflowid+column:viewtype+0+" + user.getLanguage()
     * 				+ "+column:nodeid+column:isremark+" + user.getUID()
     * 				+ "+column:agentorbyagentid+column:agenttype+column:isprocessed+" +
     * 					"column:userid+0+column:creater+column:systype+column:workflowtype";
     */
    public String getTitle4Mobile(String requestname, String para2) {
        return getTitle4Mobile(requestname, para2,false);
    }

    public String getTitle4Mobile_AttentionTag(String requestname, String para2) {
        return getTitle4Mobile(requestname, para2,true);
    }

    public String getTitle4Mobile(String requestname, String para2,boolean showAttentionTag) {
        String[] tempStr = null;
        boolean isNeedBack = false;
        if(para2.startsWith("S")){
            String[] tempStr_tmp = Util.splitString(para2, "+");
            tempStr = Arrays.copyOfRange(tempStr_tmp,9,tempStr_tmp.length);
            isNeedBack = isNeedBack(Arrays.copyOfRange(tempStr_tmp,1,8),Util.null2String(tempStr[2]));
        }else{
            tempStr = Util.splitString(para2, "+");
        }
        RecordSet rs = new RecordSet();
        String returnStr = "";
        String requestid = Util.null2String(tempStr[0]);
        String workflowid = Util.null2String(tempStr[1]);
        String viewtype = Util.null2String(tempStr[2]);
        int isovertime = Util.getIntValue(tempStr[3], 0);
        int userlang = Util.getIntValue(Util.null2String(tempStr[4]), 7);
        String nodeid = Util.null2String(tempStr[5]);
        String isremark = Util.null2String(tempStr[6]);
        String userID = Util.null2String(tempStr[7]);
        String agentorbyagentid = "";
        String agenttype = "";
        String workflowtype = "";
        String[] userIdall = Util.null2String(tempStr[14]).split(",");
        String userMain = Util.null2String(tempStr[11]);
        if (tempStr.length >= 10) {
            agenttype = Util.null2String(tempStr[9]);
        }
        if(showAttentionTag){       // 需要展示关注的流程  ，添加督办处理
            String issup = Util.null2String(tempStr[tempStr.length-1]);
            if("1".equals(issup) && tempStr.length == 16){
                String urgerParams = requestid + "+" + workflowid + "+" + userID + "+" + "0" + "+" + userlang;
                return workFlowTransMethod.getWfNewLinkByUrger(requestname,urgerParams,true,showAttentionTag);
            }
        }

        String paramstr = "";
//        WorkflowComInfo workflowComInfo = new WorkflowComInfo();
//        workflowtype = workflowComInfo.getWorkflowtype(workflowid);

        //异构系统还要调用...
        if (tempStr.length >= 16 && Util.getIntValue(requestid) < 0) {
//            String _userid = Util.null2String(tempStr[11]);
//            String myrequest = Util.null2String(tempStr[12]);
//            String creater = Util.null2String(tempStr[13]);
//            String syscode = Util.null2String(tempStr[14]);
            workflowtype = Util.null2String(tempStr[15]);
        }

        paramstr = "&_workflowid=" + workflowid + "&_workflowtype=" + workflowtype;//方便异构系统使用
        String nodetitle = "";
        //后续添加 是否使用requestnamenew
        if (true) {
            int isbill = 0;
            int formid = 0;
            // //根据后台设置在MAIL标题后加上流程中重要的字段
            rs.execute("select formid,isbill from workflow_base where id="
                    + workflowid);
            if (rs.next()) {
                formid = rs.getInt(1);
                isbill = rs.getInt(2);

            }
            requestname = new RequestBaseUtil().formatRequestname(requestname, workflowid, requestid, isbill, formid, userlang);
        }

        String attentionTag = "";
        if(showAttentionTag)attentionTag = new RequestAttentionBiz().getAttentionTag(Util.getIntValue(requestid),Util.getIntValue(userID),0,userlang,true,"");

        boolean isprocessed = false;
        boolean canContinue = false;
        //新增是否显示[退回][代理]前缀显示
        boolean hasAgentTip = false;
        boolean hasRejectTip = false;
        rs.executeSql("select isprocessed, isremark, userid, nodeid,islasttimes,agenttype,isbereject,takisremark from workflow_currentoperator where requestid = "
                + requestid
                + " and userid=" + userID
                + " order by receivedate desc, receivetime desc");
        while (rs.next()) {
            String isbereject = Util.null2String(rs.getString("isbereject"));
            String islasttimes = Util.null2String(rs.getString("islasttimes"));
            String takisremark = Util.null2String(rs.getString("takisremark"));
            String agenttype1 = Util.null2String(rs.getString("agenttype"));
            String isremark_tmp = Util.null2String(rs.getString("isremark"));
            String isprocessed_tmp = Util.null2String(rs
                    .getString("isprocessed"));
            String userid_tmp = Util.null2String(rs.getString("userid"));
            if ((isremark_tmp.equals("0") && (isprocessed_tmp.equals("0") || isprocessed_tmp
                    .equals("3") || isprocessed_tmp.equals("2")))
                    || isremark_tmp.equals("5")) {
                isprocessed = true;
            }
            //判断是否显示【退回】【代理】标识
            if ("1".equals(islasttimes) && "2".equals(agenttype1) && (("0".equals(isremark) && !"-2".equals(takisremark)) || "1".equals(isremark) || "7".equals(isremark) || "8".equals(isremark) || "9".equals(isremark) || "11".equals(isremark))) {
                hasAgentTip = true;
            }
            if ("1".equals(islasttimes) && "1".equals(isbereject) && ("0".equals(isremark) && !"-2".equals(takisremark))) {
                hasRejectTip = true;
            }
            // 如果是被抄送或转发，判断是否正是某节点的操作人，取最后一次
            if (("8".equals(isremark) || "9".equals(isremark) || "1"
                    .equals(isremark))
                    && userID.equals(userid_tmp)
                    && "0".equals(isremark_tmp)
                    && canContinue == false) {
                int nodeid_tmp = Util.getIntValue(rs.getString("nodeid"), 0);
                if (nodeid_tmp != 0) {
                    isremark = isremark_tmp;
                    nodeid = "" + nodeid_tmp;
                    canContinue = true;
                }
            }
            if (isprocessed == true && canContinue == true) {
                break;
            }
        }
        // 改为按要求显示自定义流程标题的前缀信息
        if ("0".equals(isremark)) {
            rs.executeSql("select nodetitle from workflow_flownode where workflowid="
                    + workflowid + " and nodeid=" + nodeid);
            if (rs.next()) {
                nodetitle = Util.null2String(rs.getString("nodetitle"));
            }
        }
        if (!"".equals(nodetitle) && !"null".equalsIgnoreCase(nodetitle)) {
            nodetitle = "（" + nodetitle + "）";
            requestname = nodetitle + requestname;
        }
        if(hasAgentTip){
            requestname = SystemEnv.getHtmlLabelName(390272,userlang) + requestname;
        }
        if(hasRejectTip){
            requestname = SystemEnv.getHtmlLabelName(390271,userlang) + requestname;
        }
        if (viewtype.equals("0")) {
            // 新流程,粗体链接加图片
            if (isprocessed) {
                returnStr = requestname + "<img style=\"\n" +
                        "    width: 6px;\n" +
                        "    height: 6px;\n" +
                        "    margin-left: 3px;\n" +
                        "    \n" +
                        "\" src='"+contextPath+"/images/ecology8/statusicon/BDOut_wev8.png' title='"
                        + SystemEnv.getHtmlLabelName(19081, userlang)
                        + "'/>";
            } else if ("1".equals(agenttype)) {
                returnStr = requestname + "<img  style=\"\n" +
                        "    width: 6px;\n" +
                        "    height: 6px;\n" +
                        "    margin-left: 3px;\n" +
                        "    \n" +
                        "\" src='"+contextPath+"/images/ecology8/statusicon/BDNew_wev8.png' title='"
                        + SystemEnv.getHtmlLabelName(19154, userlang)
                        + "'/>";
            } else {
                returnStr = requestname + "<img style=\"\n" +
                        "    width: 6px;\n" +
                        "    height: 6px;\n" +
                        "    margin-left: 3px;\n" +
                        "    \n" +
                        "\"  src='"+contextPath+"/images/ecology8/statusicon/BDNew_wev8.png' title='"
                        + SystemEnv.getHtmlLabelName(19154, userlang)
                        + "'/>";
            }
        } else if (viewtype.equals("-1") || isNeedBack) {
            // 旧流程,有新的提交信息未查看,普通链接加图片
            if (isprocessed) {
                returnStr = requestname + "<img style=\"\n" +
                        "    width: 6px;\n" +
                        "    height: 6px;\n" +
                        "    margin-left: 3px;\n" +
                        "    \n" +
                        "\"   src='"+contextPath+"/images/ecology8/statusicon/BDOut_wev8.png' title='"
                        + SystemEnv.getHtmlLabelName(19081, userlang)
                        + "'/>";
            } else {
                returnStr = requestname + "<img style=\"\n" +
                        "    width: 6px;\n" +
                        "    height: 6px;\n" +
                        "    margin-left: 3px;\n" +
                        "    \n" +
                        "\" src='"+contextPath+"/images/ecology8/statusicon/BDNew2_wev8.png' title='"
                        + SystemEnv.getHtmlLabelName(20288, userlang)
                        + "'/>";
            }
        } else {
            // 旧流程,普通链接
            if (isprocessed) {
                returnStr = requestname + "<img style=\"\n" +
                        "    width: 6px;\n" +
                        "    height: 6px;\n" +
                        "    margin-left: 3px;\n" +
                        "    \n" +
                        "\" src='"+contextPath+"/images/ecology8/statusicon/BDOut_wev8.png' title='"
                        + SystemEnv.getHtmlLabelName(19081, userlang)
                        + "'/>";
            } else {
                returnStr = requestname;
            }
        }
        //images/ecology8/mainwf_wev8.png

//        if (userIdall.length > 1) {
//            String name = "主";
//            String className = "wf-center-list-mainwf";
//            if (userMain.equals(userIdall[0])) {
//                //主账号
//
//            } else {
//                //次账号
//                name = "次";
//                className = "wf-center-list-mainwf-secondary";
//            }
//            returnStr += "<div class=" + className + "><div><span>" + name + "</span></div></div>";
//        }
        //==zj 移动端流程添加颜色
        //==zj 这里处理流程标题的颜色，在指定的viewcondition下
        String viewcondition = Util.null2String(tempStr[tempStr.length - 1]);
        String style = "";
        if (viewcondition.compareTo("82") >= 0 && viewcondition.compareTo("88") <= 0){
            ColorSetUtil colorSetUtil = new ColorSetUtil();
            if ("82".equals(viewcondition)) {
                //全部标签流程按优先级显示颜色
                style = colorSetUtil.setflowAllColor(requestid);
            }else {
                style = colorSetUtil.FlowTitleColor(viewcondition,false);
            }

            new BaseBean().writeLog("==zj==(移动端-流程标题颜色)"+style);
        }

        //这里添加标题颜色
        if (viewcondition.compareTo("82") >= 0 && viewcondition.compareTo("88") <= 0){
            return "<span class='wf-center-list-requestname' "+style+">" + returnStr + "</span>"+attentionTag;
        }else {
            return "<span class='wf-center-list-requestname'>" + returnStr + "</span>"+attentionTag;
        }
    }

    //反馈黄点提示判断
    public boolean isNeedBack(String paraNeedBack,String viewtype){
        String tempStr4NeedBack[] = Util.splitString(paraNeedBack, "+");
        return isNeedBack(tempStr4NeedBack,viewtype);
    }

    //反馈黄点提示判断
    public boolean isNeedBack(String tempStr4NeedBack[],String viewtype){
        String viewDate = Util.null2String(tempStr4NeedBack[0]).trim();
        String viewTime = Util.null2String(tempStr4NeedBack[1]).trim();
        String lastFeedBackDate = Util.null2String(tempStr4NeedBack[2]).trim();
        String lastFeedBackTime = Util.null2String(tempStr4NeedBack[3]).trim();
        String needwfback = Util.null2String(tempStr4NeedBack[4]).trim();
        String lastFeedBackOperator = Util.null2String(tempStr4NeedBack[5]).trim();
        String userid = Util.null2String(tempStr4NeedBack[6]).trim();
        if("".equals(lastFeedBackDate) || "".equals(lastFeedBackTime) || lastFeedBackOperator.equals(userid) || ("".equals(lastFeedBackOperator) && !"".equals(lastFeedBackDate))){
            return false;
        }else{
            return ("-2".equals(viewtype) && "1".equals(needwfback) && (lastFeedBackDate + " " + lastFeedBackTime).compareTo((viewDate + " " + viewTime).trim()) > 0);
        }
    }

    /**
     * 钉钉、企业微信pc客户端以默认浏览器打开流程
     * @param requestId
     * @return
     */
    public String getOpenByDefaultBrowserFlag(String requestId) {
        return Util.getIntValue(requestId) < 0 ? "0" : Util.null2String(new WorkflowConfigComInfo().getValue("wfOpenByDefaultBrowser"));
    }

    public String commonReqNameTransMethod(String requestname, String para2, String requestnamenew) {
        return commonReqNameTransMethod(requestname, para2, requestnamenew,false);
    }

    public String commonReqNameTransMethod_AttentionTag(String requestname, String para2, String requestnamenew) {
        return commonReqNameTransMethod(requestname, para2, requestnamenew,true);
    }

    /**
     * 通用流程标题解析方法
     * @param requestname requestname
     * @param para2 解析参数 "S+column:viewDate+column:viewTime+column:lastFeedBackDate+column:lastFeedBackTime+column:needwfback+column:lastFeedBackOperator+column:userid+S"//反馈参数不动
     * 				+ "+column:requestid+column:workflowid+column:viewtype+" + user.getLanguage()
     * 				+ "+column:nodeid+column:isremark+" + user.getUID()
     * 				+ "+column:agentorbyagentid+column:agenttype+column:isprocessed+column:userid+column:creater+scope_"+scope+"_scope+column:isbereject+column:takisremark";
     * @param requestnamenew 缓存requestname
     * @param showAttentionTag 是否显示关注标签
     * @return
     */
    public String commonReqNameTransMethod(String requestname,String para2, String requestnamenew,boolean showAttentionTag) {
        String[] tempStr = null;
        if(para2.startsWith("S")){
            String[] tempStr_tmp = Util.splitString(para2, "+");
            tempStr = Arrays.copyOfRange(tempStr_tmp,9,tempStr_tmp.length);
        }else{
            tempStr = Util.splitString(para2, "+");
        }
        String requestid = Util.null2String(tempStr[0]);
        String workflowid = Util.null2String(tempStr[1]);
        String nodeid = Util.null2String(tempStr[4]);
        String isremark = Util.null2String(tempStr[5]);
        int userid = Util.getIntValue(Util.null2String(tempStr[6]));
        String requestnamehtmlnew = Util.null2String(Util.null2String(tempStr[15])).trim();

        String convertresult = this.convertRequestNameToNew(requestname, requestnamenew);
        String titleSet = new WorkflowAllComInfo().getTitleset(workflowid);
        boolean needFormat = "".equals(Util.null2String(requestnamenew)) || "1".equals(titleSet);//仅自定义格式需要强制转换，默认格式可走requestnamenew缓存
        if(!"".equals(requestnamehtmlnew)){//完整标题缓存
            needFormat = false;
            convertresult = requestnamehtmlnew;
        }
        String requestnamelink = workFlowTransMethod.commonReqNameTransMethod(convertresult, para2, needFormat,"",showAttentionTag);

        String scope = "";
        if(para2.indexOf("scope_") > -1 && para2.indexOf("_scope") > -1) {
            scope = para2.substring(para2.indexOf("scope_"), para2.indexOf("_scope"));
            scope = scope.replace("scope_", "");
        }
        String linkageParams = "";
        //是否开启连续处理
        boolean isOpenContinuationProcess = "1".equals(RequestDefaultComInfo.getIsOpenContinnuationProcess(userid+""));
        if (isOpenContinuationProcess && "doing".equals(scope) && Util.getIntValue(requestid) > 0) {
            linkageParams += "isOpenContinuationProcess=1";
        }

        requestnamelink = this.manageSPARequestNameLink(requestnamelink, requestid, workflowid, nodeid, userid, isremark,linkageParams);
        new BaseBean().writeLog("==zj==(requestnamelink)==" + requestnamelink);
        return requestnamelink;
    }

}
