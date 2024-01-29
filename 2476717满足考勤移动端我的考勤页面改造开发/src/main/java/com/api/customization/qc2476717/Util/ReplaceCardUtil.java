package com.api.customization.qc2476717.Util;

import com.alibaba.fastjson.JSON;
import com.api.customization.qc2476717.Bean.CardBean;
import com.engine.kq.biz.KQWorkTime;
import com.engine.kq.entity.WorkTimeEntity;
import com.engine.kq.wfset.bean.SplitBean;
import com.weaver.formmodel.util.DateHelper;
import weaver.conn.RecordSet;
import weaver.formmode.data.ModeDataIdUpdate;
import weaver.formmode.setup.ModeRightInfo;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReplaceCardUtil {
    String tableName= new BaseBean().getPropValue("qc2476717","tableName");
    /**
     * 将本条流程补卡数据插入到建模表中
     */
    public Boolean insertCard(User user,Map<String,String> flowCardMap){
        Boolean isSucess = false;
        RecordSet rs = new RecordSet();
        String sqlBase = "";

        String userId = flowCardMap.get("userId");//人员id

        String signDate = flowCardMap.get("signDate");//补卡日期
        String signTime = flowCardMap.get("signTime");//补卡时间
        String signType = flowCardMap.get("signType");//补卡类型
        String signMonth = signDate.substring(0,7);//补卡年月
        String serial = "";
        String kqryid = "";
        String qdrq2 = signDate;
        try {
            //获取班次名称
            if (!"".equals(signDate) && !"".equals(userId)){
                serial = getSerial(userId,signDate);
            }
            //构建建模表数据
            int modeDataId = getModeDataId(tableName, user);
            if (modeDataId > 0) {
                sqlBase = "update " + tableName + " set userid = '" + userId + "',signdate = '" + signDate + "',serial = '" + serial + "',signtime = '" + signTime + "',signtype = '" + signType + "',kqryid = '"+kqryid+"',qdrq2 = '"+qdrq2 + "',bkyf = '"+signMonth+"' where id = '" + modeDataId + "'";
                new BaseBean().writeLog("==zj==(流程补卡数据插入)" + sqlBase);
                isSucess =  rs.executeUpdate(sqlBase);
            }

        } catch (Exception e) {
           new BaseBean().writeLog("==zj==(流程补卡数据插入失败)" + JSON.toJSONString(e));
        }

        return isSucess;
    }

    /**
     * 获取班次名称
     * @param userId
     * @param signDate
     * @return
     */
    public String getSerial(String userId,String signDate){
        new BaseBean().writeLog("==zj==(获取班次人员和考勤日期)" + userId + "==" + signDate);
        String serial = "";
        try {
            if (!"".equals(userId) && !"".equals(signDate)){
                KQWorkTime kt = new KQWorkTime();
                WorkTimeEntity workTime = kt.getWorkTime(userId, signDate);
                String serialId = workTime.getSerialId();
               if (!"".equals(serialId)){
                   RecordSet rs = new RecordSet();
                    String sql = "select * from kq_ShiftManagement where id = '"+serialId+"'";
                    new BaseBean().writeLog("==zj==(获取班次信息)" + JSON.toJSONString(sql));
                    rs.executeQuery(sql);
                    if (rs.next()){
                        serial = Util.null2String(rs.getString("serial"));
                    }
               }
            }
        } catch (Exception e) {
            new BaseBean().writeLog("==zj==(获取班次信息错误)" + JSON.toJSONString(e));
        }

        return serial;
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
     * 获取指定日期范围的补卡数据
     * @param user
     * @param fromDate
     * @param toDate
     * @return
     */
    public String getRemindCard(User user,String fromDate,String toDate){
        String count = "0";
        RecordSet rs = new RecordSet();
        String sql = "select count(*) as count from "+tableName+" where userid = '"+user.getUID()+"' and signdate >= '"+fromDate+"' and signdate <= '"+toDate+"'";
        new BaseBean().writeLog("==zj==(获取日期范围的补卡数据)" + sql);
        rs.executeQuery(sql);
        if (rs.next()){
            count = Util.null2String(rs.getString("count"));
        }
        return count;
    }

}
