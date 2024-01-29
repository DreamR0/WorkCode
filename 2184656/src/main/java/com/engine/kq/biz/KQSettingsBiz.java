package com.engine.kq.biz;

import com.engine.kq.log.KQLog;
import weaver.conn.RecordSet;
import weaver.general.Util;

/**
 * 考勤自定义配置类
 */
public class KQSettingsBiz {

    public static KQLog kqLog = new KQLog();

    public static boolean showLeaveTypeSet(String main_key) {
        RecordSet rs = new RecordSet();
        String main_val = "1";
        String sql = "select * from kq_settings where main_key=? ";
        rs.executeQuery(sql, main_key);
        if (rs.next()) {
            main_val = Util.null2String(rs.getString("main_val"));
        }

        return "1".equalsIgnoreCase(main_val);
    }

    /**
     * 考勤报表使用线程格式化
     * @return
     */
    public static boolean getKqformatthread() {
        RecordSet rs = new RecordSet();
        String main_val = "1";
        String sql = "select * from kq_settings where main_key='kqformatthread' ";
        rs.executeQuery(sql);
        if (rs.next()) {
            main_val = Util.null2String(rs.getString("main_val"));
        }
        return "1".equalsIgnoreCase(main_val);
    }

    /**
     * 考勤报表按班次时间点触发计算
     * @return
     */
    public static boolean getKqformatAccurate() {
        RecordSet rs = new RecordSet();
        String main_val = "1";
        String sql = "select * from kq_settings where main_key='kqformataccurate' ";
        rs.executeQuery(sql);
        if (rs.next()) {
            main_val = Util.null2String(rs.getString("main_val"));
        }
        return "1".equalsIgnoreCase(main_val);
    }

    /**
     * 销假流程带出的明细是否要清空默认值
     * @return
     */
    public static boolean is_leaveback_clear() {
      RecordSet rs = new RecordSet();
      String is_leaveback_clear = "0";
      String show_ajax_balance_sql = "select * from kq_settings where main_key='leaveback_clear'";
      rs.executeQuery(show_ajax_balance_sql);
      if(rs.next()) {
        String main_val = rs.getString("main_val");
        if ("1".equalsIgnoreCase(main_val)) {
          is_leaveback_clear = "1";
        }
      }
      return "1".equalsIgnoreCase(is_leaveback_clear);
    }

  /**
   * 开启后，允许早到早走，允许晚到晚走，允许晚走晚到支持考勤流程抵扣；关闭，则不支持考勤流程抵扣；此开关默认关闭
   * @return
   */
  public static boolean is_flow_humanized() {
    RecordSet rs = new RecordSet();
    String is_flow_humanized = "0";
    String show_flow_humanized_sql = "select * from kq_settings where main_key='flow_humanized'";
    rs.executeQuery(show_flow_humanized_sql);
    if(rs.next()) {
      String main_val = rs.getString("main_val");
      if ("1".equalsIgnoreCase(main_val)) {
        is_flow_humanized = "1";
      }
    }
    return "1".equalsIgnoreCase(is_flow_humanized);
  }

  /**
   * 开启了这个开关，晚到晚走，超过了设置的规则也可以按照晚到晚走设置来处理迟到
   * @return
   */
  public static boolean is_lateinlateout_outrule() {
    RecordSet rs = new RecordSet();
    String is_lateinlateout_outrule = "0";
    String show_lateinlateout_outrule_sql = "select * from kq_settings where main_key='lateinlateout_outrule'";
    rs.executeQuery(show_lateinlateout_outrule_sql);
    if(rs.next()) {
      String main_val = rs.getString("main_val");
      if ("1".equalsIgnoreCase(main_val)) {
        is_lateinlateout_outrule = "1";
      }
    }
    return "1".equalsIgnoreCase(is_lateinlateout_outrule);
  }

  /**
   * 开启了这个开关，判断历年是否包含今年之后的假期。
   * @return
   */
  public static boolean is_balanceofleave() {
    RecordSet rs = new RecordSet();
    String is_balanceofleave = "0";
    String show_is_balanceofleave_sql = "select * from kq_settings where main_key='is_balanceofleave'";
    rs.executeQuery(show_is_balanceofleave_sql);
    if(rs.next()) {
      String main_val = rs.getString("main_val");
      if ("1".equalsIgnoreCase(main_val)) {
          is_balanceofleave = "1";
      }
    }
    return "1".equalsIgnoreCase(is_balanceofleave);
  }

  /**
   * 开启了这个开关，弹性工作制可以跨天
   * @return
   */
  public static boolean is_freeAcross() {
    RecordSet rs = new RecordSet();
    String is_balanceofleave = "0";
    String show_is_balanceofleave_sql = "select * from kq_settings where main_key='is_freeAcross'";
    rs.executeQuery(show_is_balanceofleave_sql);
    if(rs.next()) {
      String main_val = rs.getString("main_val");
      if ("1".equalsIgnoreCase(main_val)) {
        is_balanceofleave = "1";
      }
    }
    return "1".equalsIgnoreCase(is_balanceofleave);
  }

  /**
   * 开启了这个开关，打卡按钮默认会显示获取地理位置
   * @return
   */
  public static boolean isFirstLocation() {
    RecordSet rs = new RecordSet();
    String is_balanceofleave = "0";
    String show_is_balanceofleave_sql = "select * from kq_settings where main_key='isFirstLocation'";
    rs.executeQuery(show_is_balanceofleave_sql);
    if(rs.next()) {
      String main_val = rs.getString("main_val");
      if ("1".equalsIgnoreCase(main_val)) {
        is_balanceofleave = "1";
      }
    }
    return "1".equalsIgnoreCase(is_balanceofleave);
  }

  /**
   * 强制归档考勤流程处理，true的话强制归档流程写入报表且生成调休、扣减假期
   * false的话，不做考勤相关处理
   * @return
   */
  public static boolean isforceflow_attend() {
    RecordSet rs = new RecordSet();
    String isforceflow_attend = "1";
    String show_isforceflow_attend_sql = "select * from kq_settings where main_key='forceflow_attend'";
    rs.executeQuery(show_isforceflow_attend_sql);
    if(rs.next()) {
      String main_val = rs.getString("main_val");
      if ("0".equalsIgnoreCase(main_val)) {
        isforceflow_attend = "0";
      }
    }
    return "1".equalsIgnoreCase(isforceflow_attend);
  }

}