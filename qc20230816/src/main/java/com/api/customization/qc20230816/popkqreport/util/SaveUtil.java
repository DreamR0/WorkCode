package com.api.customization.qc20230816.popkqreport.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.weaver.formmodel.util.DateHelper;
import weaver.conn.RecordSet;
import weaver.formmode.data.ModeDataIdUpdate;
import weaver.formmode.setup.ModeRightInfo;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.User;

import java.util.HashMap;
import java.util.Map;

public class SaveUtil {

    /**
     *保存登录弹窗信息

     */
    public Map<String,Object> SaveUtil(Map<String, Object> params, User user){
        Map<String,Object> resultMap = new HashMap<>();
        RecordSet rs = new RecordSet();
        String sql ="";
        Boolean isHave = false;
        String resourceId = "";
        resourceId =  Util.null2String(params.get("resourceId"));
        if ("".equals(resourceId)){
            resourceId = Util.null2String(user.getUID()); //考勤人员
        }
        String confirmDate = Util.null2String(params.get("confirmDate")); //确认时间
        String kqDate = Util.null2String(params.get("kqDate")); //考勤月份
        String confirmType = Util.null2String(params.get("confirmType")); //确认类型 0--系统确认  1--本人确认
        String poprecordTableName = new BaseBean().getPropValue("qc20230816","poprecordTableName"); //考勤确认记录表

        if (resourceId.equals( Util.null2String(user.getUID()))){
            //如果当前修改人员为当前操作人员，改为本人确认
            confirmType = "1";
        }

        try {
            //先查询是否有考勤记录，有就更新，没有就增加
            sql = "select * from "+poprecordTableName+" where ry='"+resourceId+"' and kqyf='"+kqDate+"'";
            rs.executeQuery(sql);
            if (rs.next()){
                isHave = true;
            }
            if (isHave){
                sql = "update "+poprecordTableName+" set qrsj='"+confirmDate+"',qrqk="+confirmType+" where ry="+resourceId+" and kqyf='"+kqDate+"'";
                 rs.executeUpdate(sql);

            }else {
                //构建建模表数据
                int modeDataId = getModeDataId(poprecordTableName, user);
                new BaseBean().writeLog("==zj==(modeDataId)" + JSON.toJSONString(modeDataId));
                if (modeDataId > 0){
                    sql = " update "+poprecordTableName+" set ry='"+resourceId+"',qrqk='"+confirmType+"',qrsj='"+confirmDate+"',kqyf='"+kqDate+"' where id='"+modeDataId+"'";
                    rs.executeUpdate(sql);
                }
            }
        } catch (Exception e) {
            new BaseBean().writeLog("==zj==(保存弹窗确认信息报错)" + JSON.toJSONString(e));
        }
        resultMap.put("status","1");
        resultMap.put("message","保存成功");
        return  resultMap;
    }

    /**
     * 构建建模表数据
     * @param tableName
     * @param user
     * @return
     */
    public int getModeDataId(String tableName, User user){
        int modeId = getModeId(tableName);
        if (modeId==-1){
            return -1;
        }
        int id = ModeDataIdUpdate.getInstance().getModeDataNewIdByUUID(tableName, modeId, user.getUID(), (user.getLogintype()).equals("1") ? 0 : 1,
                DateHelper.getCurrentDate(), DateHelper.getCurrentTime());
        ModeRightInfo ModeRightInfo = new ModeRightInfo();
        ModeRightInfo.setNewRight(true);
        ModeRightInfo.editModeDataShare(user.getUID(),Integer.parseInt(modeId+""),id);//新建的时候添加共享
        ModeRightInfo.addDocShare(user.getUID(),Integer.parseInt(modeId+""),id);//新建的时候添加文档共享
        return id;
    }

    /**
     * 获取建模id
     * @param tablename
     * @return
     */
    public int getModeId(String tablename){
        RecordSet rs = new RecordSet();
        int modeid = -1;
        String sql = "select id as modeid,formid from modeinfo where FORMID = (select id from workflow_bill where TABLENAME = ?)";
        rs.executeQuery(sql,tablename);
        if (rs.next()){
            modeid = rs.getInt("modeid");
        }
        return modeid;
    }

    /**
     * 考勤报表里面确认
     * @params
     * @return
     */
    public Map<String,Object> SaveSysUtil(JSONObject params){
        String sql = "";
        String resourceId = ""; //人员id
        String confirmDate = "";    //确认时间
        String kqDate = "";         //考勤日期
        String confirmType = "";    //确认类型  0--系统确认  1--本人确认
        String poprecordTableName = new BaseBean().getPropValue("qc20230816","poprecordTableName"); //考勤确认记录表
        RecordSet rs = new RecordSet();
        Boolean isHave = false;


        try {
            String data = JSONArray.toJSONString(params.get("data"));
            JSONArray datas = JSONArray.parseArray(data);
            new BaseBean().writeLog("==zj==(JSONArray)" + JSON.toJSONString(datas));

            //开始循环
            for (int i = 0; i < datas.size(); i++) {
                JSONObject userData = datas.getJSONObject(i);
                resourceId = Util.null2String(userData.get("resourceId"));
                confirmDate = Util.null2String(userData.get("confirmDate"));
                kqDate = Util.null2String(userData.get("kqDate"));
                confirmType = Util.null2String(userData.get("confirmType"));

                //先查询是否有考勤记录，有就更新，没有就增加
                sql = "select * from "+poprecordTableName+" where ry='"+resourceId+"' and kqyf='"+kqDate+"'";
                rs.executeQuery(sql);
                if (rs.next()){
                    isHave = true;
                }
                if (isHave){
                    sql = "update "+poprecordTableName+" set qrsj='"+confirmDate+"',qrqk="+confirmType+" where ry="+resourceId+" and kqyf='"+kqDate+"'";
                    rs.executeUpdate(sql);

                }else {
                    //构建建模表数据
                    User user = new User(1);
                    int modeDataId = getModeDataId(poprecordTableName, user);
                    new BaseBean().writeLog("==zj==(modeDataId)" + JSON.toJSONString(modeDataId));
                    if (modeDataId > 0){
                        sql = " update "+poprecordTableName+" set ry='"+resourceId+"',qrqk='"+confirmType+"',qrsj='"+confirmDate+"',kqyf='"+kqDate+"' where id='"+modeDataId+"'";
                        rs.executeUpdate(sql);
                    }
                }

            }
        } catch (Exception e) {
            new BaseBean().writeLog("==zj==(考勤内报表报错)" + JSON.toJSONString(e));
        }

        Map<String,Object> result = new HashMap<>();
        result.put("status","1");
        result.put("message","保存成功");

        return result;
    }

}
