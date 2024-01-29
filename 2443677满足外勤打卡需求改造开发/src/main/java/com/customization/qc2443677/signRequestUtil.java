package com.customization.qc2443677;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.engine.common.util.ServiceUtil;
import com.engine.odoc.util.DocUtil;
import com.engine.workflow.constant.PAResponseCode;
import com.engine.workflow.entity.publicApi.PAResponseEntity;
import com.engine.workflow.publicApi.WorkflowRequestOperatePA;
import com.engine.workflow.publicApi.impl.WorkflowRequestOperatePAImpl;
import com.wbi.util.Util;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.hrm.User;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.Map;

public class signRequestUtil {
    /**
     * 新建流程
     * @param user
     * @param wfId
     * @param mainTable
     */
    public Integer createRequest(HttpServletRequest request,User user, String wfId, JSONArray mainTable) {
        new BaseBean().writeLog("==zj==(创建流程方法进入1111)");

        int requestid = 0;     //创建流程id
        try {
            WorkflowRequestOperatePA workflowRequestOperatePA= ServiceUtil.getService(WorkflowRequestOperatePAImpl.class);
            JSONObject requestJSON = new JSONObject();
            requestJSON.put("detailData","");//明细表数据(非必填)
            requestJSON.put("mainData", JSON.toJSONString(mainTable));//主表数据(必填)
            requestJSON.put("otherParams", "");//其他参数，比如messageType,isnextflow,requestSecLevel，delReqFlowFaild(非必填)
            requestJSON.put("remark", "");//签字意见，默认值流程默认意见若未设置则为空(非必填)
            requestJSON.put("requestLevel", "");//紧急程度(非必填)
            requestJSON.put("requestName", "外勤打卡审批");//流程标题(必填)
            requestJSON.put("workflowId", wfId);//流程Id(必填)
            new BaseBean().writeLog("==zj==(创建流程开始执行方法)");
            new BaseBean().writeLog("==zj==(requestJSON)" + JSON.toJSONString(requestJSON));

            PAResponseEntity resultEntity = workflowRequestOperatePA.doCreateRequest(user, WorkFlowUtil.parseRequestEntity(requestJSON));
            new BaseBean().writeLog("==zj==(流程创建resultEntity)" + JSON.toJSONString(resultEntity));
            boolean isSuccess = resultEntity.getCode() == PAResponseCode.SUCCESS;
            new BaseBean().writeLog("==zj==(创建流程失败信息)" + resultEntity.getErrMsg() + " | code: " + resultEntity.getCode());
            requestid = 0;
            if (isSuccess){
                Object resultData = resultEntity.getData();
                if (resultData != null){
                    JSONObject requestidData =JSONObject.parseObject(JSON.toJSONString(resultData)) ;
                     requestid = requestidData.getInteger("requestid");
                    new BaseBean().writeLog("==zj==(requestid)" + requestid);
                }
            }
        } catch (Exception e) {
            new BaseBean().writeLog("==zj==(创建流程异常)" + JSON.toJSONString(e));
        }

        return requestid;
    }

    /**
     * 提交流程
     * @param user
     * @param requestId
     * @param mainTable
     */
    public static Boolean submitRequest(User user, int requestId, JSONArray mainTable) {
        new BaseBean().writeLog("==zj==(提交流程触发)" + "111111");
        WorkflowRequestOperatePA workflowRequestOperatePA= ServiceUtil.getService(WorkflowRequestOperatePAImpl.class);
        new BaseBean().writeLog("==zj==(提交流程触发)" + "222222");
        JSONObject requestJSON = new JSONObject();
        requestJSON.put("mainData",mainTable);//主表数据(非必填)
        requestJSON.put("otherParams", "");//其他参数，比如messageType,isnextflow,requestSecLevel，delReqFlowFaild(非必填)
        requestJSON.put("remark", "");//签字意见，默认值流程默认意见若未设置则为空(非必填)
        new BaseBean().writeLog("==zj==(requestId)" + requestId);
        requestJSON.put("requestId", requestId);//流程Id(必填)
        PAResponseEntity resultEntity = workflowRequestOperatePA.submitRequest(user, WorkFlowUtil.parseRequestEntity(requestJSON));
        boolean isSuccess = resultEntity.getCode() == PAResponseCode.SUCCESS;
        new BaseBean().writeLog("==zj==(提交流程失败信息)" + resultEntity.getErrMsg() + " | code: " + resultEntity.getCode());
        return isSuccess;

    }
    /**
     * 设置主表数据
     * @param user
     * @return
     */
    public JSONArray setMainTable(HttpServletRequest request,User user, Map<String, Object> params, Map<String,Object> otherParams){
        SimpleDateFormat times = new SimpleDateFormat("HH:mm");
        RecordSet rs = new RecordSet();
        JSONArray mainTable = new JSONArray();
        JSONArray imagefile = new JSONArray();
       String sql ="";
        try{
            //获取打卡数据
            String signDate = Util.null2String(otherParams.get("signDate"));    //签到日期
            String signTime = Util.null2String(otherParams.get("signTime"));    //签到时间
            if (signTime.length() > 5){
                signTime = signTime.substring(0,5);
            }
            String addr = Util.null2String(otherParams.get("address"));         //签到地点
            String signType = Util.null2String(otherParams.get("signType"));    //考勤类型
            String isInCom = Util.null2String(otherParams.get("isInCom"));    //是否是有效考勤卡
            String userType = Util.null2String(otherParams.get("userType"));    //用户类型
            String signfrom = Util.null2String(otherParams.get("signfrom"));    //打卡来源
            String belongdate = Util.null2String(otherParams.get("belongdate"));    //归属日期
            String remark = Util.null2String(otherParams.get("remark"));    //备注
            String browser = Util.null2String(otherParams.get("browser"));    //浏览器类型
            //这里把备注标签去除一下
            remark = replaceStr(remark);
            new BaseBean().writeLog("==zj==(备注标签)" +remark);
            String longitude = Util.null2String(otherParams.get("longitude"));    //经度
            String latitude = Util.null2String(otherParams.get("latitude"));    //纬度
            String attachment = Util.null2String(otherParams.get("attachment"));    //附件id
            new BaseBean().writeLog("==zj==(附件id值)" + JSON.toJSONString(attachment));
            //获取附件下载路径
            String imageName = "";//图片名称
            if (attachment.length() <= 0 || attachment == null){
                new BaseBean().writeLog("==zj==(附件id是否为空)" + "".equals(attachment));
            }else {
                //查询对应fileid的信息
                String[] attachments = attachment.split(",");
                for (int i = 0; i < attachments.length; i++) {
                    String selectSql = "select * from imagefile where imagefileid = "+attachments[i];
                    new BaseBean().writeLog("==zj==(selectSql)" + JSON.toJSONString(selectSql));
                    rs.executeQuery(selectSql);
                    if (rs.next()){
                        imageName = Util.null2String(rs.getString("imagefilename"));
                    }
                    if (!imageName.contains(".")){
                        //这里修改下imagefile的对应信息
                        imageName += imageName+".jpg";
                        String upDatesql = "update imagefile set imagefilename = "+imageName+" where imagefileid = "+attachments[i];
                        new BaseBean().writeLog("==zj==(修改图片信息sql)" + JSON.toJSONString(upDatesql));
                        rs.executeUpdate(upDatesql);
                    }
                    String filePath = getAttachmentPath(request,user,attachments[i],browser);
                    JSONObject imagefileDatas = new JSONObject();
                    imagefileDatas.put("filePath",filePath);
                    imagefileDatas.put("fileName",imageName);
                    imagefile.add(imagefileDatas);
                }
                }
            //把表单数据设置一下
                sql = "select * from hrmresource where id=" + user.getUID();
                rs.executeQuery(sql);
                if (rs.next()){
                    //打卡人
                    JSONObject mainTablaDatas = new JSONObject();
                    mainTablaDatas.put("fieldName","dkr");
                    mainTablaDatas.put("fieldValue",user.getUID()+"");
                    mainTable.add(mainTablaDatas);
                    //打卡日期
                     mainTablaDatas = new JSONObject();
                    mainTablaDatas.put("fieldName","dkrq");
                    mainTablaDatas.put("fieldValue",signDate);
                    mainTable.add(mainTablaDatas);
                    //打卡时间
                     mainTablaDatas = new JSONObject();
                    mainTablaDatas.put("fieldName","dksj");
                    mainTablaDatas.put("fieldValue",signTime);
                    mainTable.add(mainTablaDatas);
                    //打卡地点
                    mainTablaDatas = new JSONObject();
                    mainTablaDatas.put("fieldName","dkdd");
                    mainTablaDatas.put("fieldValue",addr);
                    mainTable.add(mainTablaDatas);
                    //申请人
                    mainTablaDatas = new JSONObject();
                    mainTablaDatas.put("fieldName","sqr");
                    mainTablaDatas.put("fieldValue",user.getUID()+"");
                    mainTable.add(mainTablaDatas);
                    //申请日期
                    mainTablaDatas = new JSONObject();
                    mainTablaDatas.put("fieldName","sqrq");
                    mainTablaDatas.put("fieldValue",signDate);
                    mainTable.add(mainTablaDatas);
                    //申请单位(分部)
                    mainTablaDatas = new JSONObject();
                    mainTablaDatas.put("fieldName","sqdw");
                    mainTablaDatas.put("fieldValue", Util.null2String(rs.getString("subcompanyid1")));
                    mainTable.add(mainTablaDatas);
                    //申请部门
                    mainTablaDatas = new JSONObject();
                    mainTablaDatas.put("fieldName","sqbm");
                    mainTablaDatas.put("fieldValue", Util.null2String(rs.getString("departmentid")));
                    mainTable.add(mainTablaDatas);
                    //联系方式
                    mainTablaDatas = new JSONObject();
                    mainTablaDatas.put("fieldName","lxfs");
                    mainTablaDatas.put("fieldValue", Util.null2String(rs.getString("mobile")));
                    mainTable.add(mainTablaDatas);
                    //考勤类型
                    mainTablaDatas = new JSONObject();
                    mainTablaDatas.put("fieldName","kqlx");
                    mainTablaDatas.put("fieldValue",signType);
                    mainTable.add(mainTablaDatas);
                    //用户类型
                    mainTablaDatas = new JSONObject();
                    mainTablaDatas.put("fieldName","yhlx");
                    mainTablaDatas.put("fieldValue",userType);
                    mainTable.add(mainTablaDatas);
                    //是否有效考勤卡
                    mainTablaDatas = new JSONObject();
                    mainTablaDatas.put("fieldName","sfsyxkqdk");
                    mainTablaDatas.put("fieldValue",isInCom);
                    mainTable.add(mainTablaDatas);
                    //考勤来源
                    mainTablaDatas = new JSONObject();
                    mainTablaDatas.put("fieldName","kqly");
                    mainTablaDatas.put("fieldValue",signfrom);
                    mainTable.add(mainTablaDatas);
                    //归属日期
                    mainTablaDatas = new JSONObject();
                    mainTablaDatas.put("fieldName","gsrq");
                    mainTablaDatas.put("fieldValue",belongdate);
                    mainTable.add(mainTablaDatas);
                    //备注
                    mainTablaDatas = new JSONObject();
                    mainTablaDatas.put("fieldName","bz");
                    mainTablaDatas.put("fieldValue",remark);
                    mainTable.add(mainTablaDatas);
                    //附件图片
                    mainTablaDatas = new JSONObject();
                    mainTablaDatas.put("fieldName","sctp");
                    if (attachment.length() <= 0 || attachment == null){
                        mainTablaDatas.put("fieldValue","");
                    }else {
                        mainTablaDatas.put("fieldValue",imagefile);
                    }
                    mainTable.add(mainTablaDatas);

                }

           new BaseBean().writeLog("==zj==(mainTable)" + JSON.toJSONString(mainTable));
        }catch (Exception e){
            new BaseBean().writeLog("==zj==(设置主表数据报错)" + e);
        }
        return  mainTable;
    }

    /**
     * 将备注的标签去除
     * @param str
     * @return
     */
    public String replaceStr(String str){
        String remark = "";
        try{
            if(str.length() > 0){
                 remark = str.replaceAll("<p>", "");
                 remark = remark.replaceAll("</p>", "");
            }
        }catch (Exception e){
            new BaseBean().writeLog("==zj==(去除备注标签报错)" + e);
        }
        return remark;
    }

    /**
     * 获取附件路径
     * @param attachment
     * @param user
     * @return
     */
    public String getAttachmentPath(HttpServletRequest request,User user,String attachment,String browser){
        weaver.docs.docs.util.DesUtils des = null;
        try {
            des = new weaver.docs.docs.util.DesUtils();
        } catch (Exception e) {

        }
        String ddcode = user.getUID() + "_" + attachment;
        try {
            ddcode = des.encrypt(ddcode);
        } catch (Exception e) {

        }

        new BaseBean().writeLog("==zj==(ddcode)" + JSON.toJSONString(ddcode));
        String uuid = DocUtil.setUserInfo(user);
        String path = "";
        String ipAdress = new BaseBean().getPropValue("qc2443677","ipAdress");
        //qc2443677
             new BaseBean().writeLog("==zj==(浏览器类型)" + JSON.toJSONString(browser));
             /*String host = request.getScheme()+"s" + "://" + request.getServerName() *//*+ "/"*//* *//*+ request.getServerPort() *//*+ request.getContextPath() + "/";*/
         path = ipAdress + "weaver/weaver.file.FileDownloadForNews?fileid="+attachment+"&uuid="+uuid;
        return path;
    }

    }
