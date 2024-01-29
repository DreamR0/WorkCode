package com.api.customization.qc2669355.util;

import com.alibaba.fastjson.JSON;
import com.engine.kq.biz.KQBalanceOfLeaveBiz;
import com.engine.kq.biz.KQLeaveRulesComInfo;
import com.weaver.formmodel.util.DateHelper;
import weaver.conn.RecordSet;
import weaver.formmode.data.ModeDataIdUpdate;
import weaver.formmode.setup.ModeRightInfo;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.User;

import java.util.Calendar;

public class BalanceOfLeaveUtil {
    private  String BalanceOfLeaveTable  = new BaseBean().getPropValue("qc2669355","BalanceOfLeaveTable");//记录事假
    private String tiaoxiuTypeId = new BaseBean().getPropValue("qc2669355","tiaoxiuId");

    /**
     * 获取当前人员的调休余额
     * @param resourceId    人员id
     * @return
     */
    public Double getRestAmount(String resourceId){
        Double allRestAmountD=0.0;
        try {
            //看看调休有多少
            KQLeaveRulesComInfo rulesComInfo = new KQLeaveRulesComInfo();

            Calendar today = Calendar.getInstance();
            /**今年*/
            String currentDate = Util.add0(today.get(Calendar.YEAR), 4) + "-"
                    + Util.add0(today.get(Calendar.MONTH) + 1, 2) + "-"
                    + Util.add0(today.get(Calendar.DAY_OF_MONTH), 2);

            String allRestAmount = KQBalanceOfLeaveBiz.getRestAmount("" + resourceId, tiaoxiuTypeId, currentDate, true, true);
            if (!"".equals(allRestAmount)){
                allRestAmountD = Double.parseDouble(allRestAmount);
            }
        } catch (Exception e) {
            new BaseBean().writeLog("==zj==(获取调休余额错误)" + e);
        }

        return allRestAmountD;
    }

    /**
     * 用来事假抵扣调休时长
     * @param user  用来创建建模表--管理员
     * @param resourceId    考勤人员id
     * @param fromDate      默认为上个月月末
     * @param absenceHoursD 上月事假时长
     * @param allRestAmountD    当前人员全部调休时长
     * @param newLeaveType  假期类型
     */
    public void addUsedAmount(User user,String resourceId,String fromDate,Double absenceHoursD,Double allRestAmountD,String newLeaveType){
        Double remainRestAmountD = 0.0;//已抵扣调休的事假时长
        new BaseBean().writeLog("==zj==(人员id - 当月事假 - 总计调休 - 日期)" + resourceId + " | " + absenceHoursD + " | " + allRestAmountD + " | " + fromDate);
        if (allRestAmountD <= 0 || absenceHoursD <=0 ){
            //如果没有调休余额和事假就不执行动作

        } else if (allRestAmountD > absenceHoursD){
            //如果调休余额 > 事假时长，全部扣除
            KQBalanceOfLeaveBiz.addUsedAmount(resourceId, fromDate, tiaoxiuTypeId, absenceHoursD+"", "","",fromDate);
            saveRemainRestAmount(user,absenceHoursD,resourceId,fromDate);
        }else if (absenceHoursD > allRestAmountD){
            remainRestAmountD = absenceHoursD - ( absenceHoursD - allRestAmountD);//抵扣调休的事假时长
            KQBalanceOfLeaveBiz.addUsedAmount(resourceId, fromDate, tiaoxiuTypeId, remainRestAmountD+"", "","",fromDate);
            //将抵扣完的事假时长保存到建模表中
            saveRemainRestAmount(user,remainRestAmountD,resourceId,fromDate);
        }
    }

    /**
     * 将每月以转为调休的事假时长保存到建模表中
     * @param user  用来创建建模表 -- 管理员
     * @param remainRestAmountD 已抵扣调休的事假时长
     * @param resourceId    人员id
     * @param fromDate      抵扣日期 -- 默认为上月月末
     */
    public void saveRemainRestAmount(User user,Double remainRestAmountD,String resourceId,String fromDate){
        RecordSet rs = new RecordSet();
        String kqMonth = "";    //考勤年月
        if (fromDate.length() >= 7){
            kqMonth = fromDate.substring(0,7);
        }
        deleteRemainRestAmount(resourceId,kqMonth);//先删除下对应月份的抵扣调休事假数据
        int modeDataId = getModeDataId(BalanceOfLeaveTable, user);
        if (modeDataId > 0){
            String sql = "update "+BalanceOfLeaveTable+" set kqry="+resourceId+",kqyf='"+kqMonth+"',dkdx="+remainRestAmountD+" where id="+modeDataId;
            rs.executeUpdate(sql);
        }

    }

    /**
     * 删除对应人员对应考勤月份的事假抵扣信息
     * @param resourceId
     * @param kqMonth
     */
    public void deleteRemainRestAmount(String resourceId,String kqMonth){
        RecordSet deleteRS = new RecordSet();
        String sql = "delete from "+BalanceOfLeaveTable+" where kqry="+resourceId+" and kqyf='"+kqMonth+"'";
        deleteRS.executeUpdate(sql);
    }

    public boolean selectRemainRestAmount(String resourceId,String kqMonth){
        Boolean isHave = false;
        RecordSet selectRS = new RecordSet();
        String sql = "select *  from "+BalanceOfLeaveTable+" where kqry="+resourceId+" and kqyf='"+kqMonth+"'";
        new BaseBean().writeLog("==zj==(查询上月是否已生成抵扣事假)" + JSON.toJSONString(sql));
        selectRS.executeQuery(sql);
        if (selectRS.next()){
            isHave = true;
        }
        return isHave;

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
}
