package com.api.customization.kq.qc2241191;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.engine.kq.biz.KQFormatBiz;
import com.engine.kq.biz.KQGroupComInfo;
import com.engine.kq.biz.KQGroupMemberComInfo;
import weaver.common.DateUtil;
import weaver.conn.DBUtil;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.HrmUserVarify;
import weaver.hrm.Reminder.KQAutoCardTask;
import weaver.hrm.User;
import weaver.systeminfo.SystemEnv;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KqGroupSet {


//==zj 获取考勤组数据
public List<Object> kqGroupSelect(Map<String,Object> params, User user){

    List<Object> datas = new ArrayList<>();
    Map<String,Object> data = null;
    RecordSet rs = new RecordSet();
    String sql="";

    try{
        //==zj 获取前端数据
        JSONObject jsonObject = JSON.parseObject(Util.null2String(params.get("data")));
        new BaseBean().writeLog("==zj==(获取前端数据-考勤组)" + JSON.toJSONString(jsonObject));
        String groupId = Util.null2String(jsonObject.get("groupId"));
        sql = "select id,groupname from kq_group where (isdelete is null or isdelete <>'1') and id!= " + groupId;
        new BaseBean().writeLog("获取考勤组sql" + sql);
        rs.executeQuery(sql);
        while (rs.next()){
            data = new HashMap<>();
            int id = rs.getInt("id");
            String groupName = Util.null2String(rs.getString("groupname"));

            data.put("id",id);
            data.put("groupName",groupName);
            datas.add(data);
        }

    }catch (Exception e){
        new BaseBean().writeLog("获取考勤组错误信息--" + e);
    }

    return datas;
}

//==zj 执行考勤组成员转换
    public Map<String,Object> KqGroupChange(Map<String,Object> params, User user){
        Map<String, Object> retmap = new HashMap<String, Object>();
        RecordSet rs = new RecordSet();
        String sql = "";
        try{
            //必要的权限判断
            if(!HrmUserVarify.checkUserRight("HrmKQGroup:Add",user)) {
                retmap.put("status", "-1");
                retmap.put("message", SystemEnv.getHtmlLabelName(2012, user.getLanguage()));
                return retmap;
            }

            String ids = Util.null2String(params.get("ids"));   //获取更改成员id
            String groupId = Util.null2String(params.get("groupId"));   //获取考勤组id
            new BaseBean().writeLog("==zj==(要改变的groupid和成员id)" + groupId + " | " + ids);
            List sqlParams = new ArrayList();
            Object[] objects= DBUtil.transListIn(ids,sqlParams);
            new BaseBean().writeLog("==zj==(objects和sqlParams)" + JSON.toJSONString(objects));

                sql="update kq_groupmember set groupid="+groupId+"where id in ("+objects[0]+") ";
                new BaseBean().writeLog("==zj==(更换考勤组成员sql)" + sql + " | " + JSON.toJSONString(sqlParams));
                rs.executeUpdate(sql,sqlParams);


            new KQGroupMemberComInfo().removeCache();

            //格式化考勤
            new KQFormatBiz().formatDateByGroupId(groupId, DateUtil.getCurrentDate());

            KQGroupComInfo kQGroupComInfo = new KQGroupComInfo();
            String auto_checkout = kQGroupComInfo.getAuto_checkout(groupId);
            String auto_checkin = kQGroupComInfo.getAuto_checkin(groupId);
            if("1".equalsIgnoreCase(auto_checkin) || "1".equalsIgnoreCase(auto_checkout)){
                //如果开启了自动打卡，保存考勤组成员之后需要格式化下缓存
                KQAutoCardTask kqAutoCardTask = new KQAutoCardTask();
                kqAutoCardTask.initAutoCardTask(groupId);
            }
            retmap.put("status", "1");
            retmap.put("message","保存成功");
        }catch (Exception e){
            retmap.put("status", "-1");
            retmap.put("message","保存失败");
            new BaseBean().writeLog("==zj==(考勤组成员更换数据处理--错误)" + e);
        }
        return retmap;
    }
}
