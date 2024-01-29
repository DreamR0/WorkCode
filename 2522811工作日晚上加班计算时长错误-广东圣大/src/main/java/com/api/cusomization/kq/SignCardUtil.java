package com.api.cusomization.kq;

import com.alibaba.fastjson.JSON;
import com.engine.kq.biz.KQWorkTime;
import com.engine.kq.cmd.attendanceButton.ButtonStatusEnum;
import com.engine.kq.entity.WorkTimeEntity;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;

import java.util.List;
import java.util.Map;

public class SignCardUtil {

    /**
     * 这里取下午下班卡，取距离下班时间最近的卡
     * @param lsCheckInfo
     * @return
     */
    public void getOutCard(String userId,String signEndDateTime4Afternoon,List<Object> lsCheckInfo){
        for (int i = 0; i < lsCheckInfo.size(); i++) {
            Map<String, Object> checkInfoInner = (Map<String, Object>) lsCheckInfo.get(i);
            if (checkInfoInner.get("signType").equals("2")){
                RecordSet rs = new RecordSet();
                String signId = "";
                String signTime = "";
                String signDate = "";
                String signDateTime = "";
                String sql = "select * from hrmschedulesign where 1=1 and isInCom='1' and userid=" + userId +
                        " and signDate+' '+ signTime>='" + signEndDateTime4Afternoon + "'  order by signdate,signtime";
                new BaseBean().writeLog("==zj==（下班卡范围sql）" + sql);
                rs.executeQuery(sql);
                if (rs.next()){
                    signId = Util.null2String(rs.getString("id"));
                    signDate = Util.null2String(rs.getString("signdate"));
                    signTime = Util.null2String(rs.getString("signtime"));
                    signDateTime = signDate+" "+signTime;
                }

                String signDateInner = Util.null2String(checkInfoInner.get("signDate"));
                String signTimeInner = Util.null2String(checkInfoInner.get("signTime"));
                String deduct_signofftime = Util.null2String(checkInfoInner.get("deduct_signofftime"));


                new BaseBean().writeLog("==zj==(checkInfoInner取原来下班卡)" + JSON.toJSONString(checkInfoInner));
                if(!"".equals(signTimeInner)) {
                    String signDateTimeInner = signDateInner + " " + signTimeInner;
                    if(signDateTime.compareTo(signDateTimeInner) < 0) {
                        checkInfoInner.put("signId", signId);//签到签退标识
                        checkInfoInner.put("signType", "2");
                        checkInfoInner.put("signDate", signDateInner);//签到签退日期
                        checkInfoInner.put("signTime", signTime);//签到签退时间
                        checkInfoInner.put("deduct_signofftime", deduct_signofftime);//流程抵扣作为打卡时间
                        checkInfoInner.put("signStatus", ButtonStatusEnum.NORMAL.getStatusCode());
                    }
                }
            }
        }
    }

    /**
     * 这里获取当天班次名称
     * @param userId
     * @param kqDate
     * @return
     */
    public Boolean getserialName(String userId,String kqDate){
        Boolean isNight = false;
        RecordSet rs = new RecordSet();
        String serialNightTable = new BaseBean().getPropValue("qc2522811","serialNighttTable");
        String serialNigthName = "";
        String serial = "";//班次名称
        String serialId = "";//班次id
        try {
            if (!"".equals(userId) && !"".equals(kqDate)){
                KQWorkTime kt = new KQWorkTime();
                WorkTimeEntity workTime = kt.getWorkTime(userId, kqDate);
                 serialId = Util.null2String(workTime.getSerialId());
                if (!"".equals(serialId)){
                    String sql = "select * from kq_ShiftManagement where id = '"+serialId+"'";
                    new BaseBean().writeLog("==zj==(获取班次信息)" + JSON.toJSONString(sql));
                    rs.executeQuery(sql);
                    if (rs.next()){
                        serial = Util.null2String(rs.getString("serial"));
                    }
                }
            }
            //这里查询下维护的夜班班次信息
            if (!"".equals(serial)){
                String serialNightSql = "select * from "+serialNightTable;
                rs.executeQuery(serialNightSql);
                while (rs.next()){
                    serialNigthName  = Util.null2String(rs.getString("ybbcmc"));
                    new BaseBean().writeLog("==zj==(班次信息对比新)" + "serial:" + serial + "  |  " +serialNigthName + " 对比: " + serialNigthName.contains(serial));
                    if (serialNigthName.contains(serial)){
                        isNight = true;
                        break;
                    }
                }
            }




        } catch (Exception e) {
            new BaseBean().writeLog("==zj==(获取班次信息错误)" + JSON.toJSONString(e));
        }

return isNight;
    }
}
