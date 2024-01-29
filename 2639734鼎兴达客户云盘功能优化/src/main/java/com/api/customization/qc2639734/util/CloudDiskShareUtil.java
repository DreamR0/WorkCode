package com.api.customization.qc2639734.util;

import com.weaver.general.TimeUtil;
import com.weaver.general.Util;
import weaver.conn.RecordSet;
import weaver.docs.networkdisk.server.NetWorkDiskFileOperateServer;
import weaver.general.BaseBean;
import weaver.hrm.User;
import weaver.systeminfo.SystemEnv;

import java.util.HashMap;
import java.util.Map;

public class CloudDiskShareUtil {

    /**
     * 保存云盘分享文件
     * @param params
     * @param user
     * @return
     */
    public Map<String, Object> save(Map<String,Object> params, User user){
        Map<String,Object> apidatas = new HashMap<String, Object>();
        try {
            //要分享的用户id
            String userids = Util.null2String((String) params.get("resourceids"));//人员id
            //要分享的组id
            String groupids = Util.null2String((String) params.get("sharegroupid"));//群组id
            //被操作（分享）的目录id
            String folderids = Util.null2String((String) params.get("folderids"));//云盘id
            //被操作（分享）的文档id
            String fileids = Util.null2String((String) params.get("fileids"));
            String date = TimeUtil.getCurrentDateString();
            String time = TimeUtil.getOnlyCurrentTimeString();
            RecordSet rs = new RecordSet();
            String sql = "";

            //过滤文件夹、文件，是否是自己的
            NetWorkDiskFileOperateServer ndfo= new NetWorkDiskFileOperateServer();
            fileids = ndfo.getCurrentUserFile(fileids,user);
            folderids = ndfo.getCurrentUserFolder(folderids,user);

            //如果被操作的文件id不为空，即分享文件
            if(!fileids.isEmpty()){
                //
                if(!userids.isEmpty()){
                    for(String fileid : fileids.split(",")){
                        if(fileid.isEmpty()) continue;
                        String fileMes = Util.null2String((String) params.get("file_" + fileid));
                        for(String userid : userids.split(",")){
                            if(userid.isEmpty()) continue;
                            sql = "insert into Networkfileshare(fileid,sharerid,tosharerid,sharedate,sharetime,sharetype,filetype,msgId) values(?,?,?,?,?,?,?,?)";
                            rs.executeUpdate(sql,fileid,user.getUID(),userid,date,time,1,1,fileMes);
                        }
                    }
                }
                if(!groupids.isEmpty()){
                    for(String fileid : fileids.split(",")){
                        if(fileid.isEmpty()) continue;
                        String fileMes = Util.null2String((String) params.get("file_" + fileid));
                        for(String groupid : groupids.split(",")){
                            if(groupid.isEmpty()) continue;
                            sql = "insert into Networkfileshare(fileid,sharerid,tosharerid,sharedate,sharetime,sharetype,filetype,msgId) values(?,?,?,?,?,?,?,?)";
                            rs.executeUpdate(sql,fileid, user.getUID(),groupid,date,time,2,1,fileMes);
                        }
                    }
                }
            }

            //如果被操作的文件id不为空，即分享目录
            if(!folderids.isEmpty()){
                if(!userids.isEmpty()){
                    for(String fid : folderids.split(",")){
                        if(fid.isEmpty()) continue;
                        String folderMes = Util.null2String((String) params.get("folder_" + fid));
                        for(String userid : userids.split(",")){
                            if(userid.isEmpty()) continue;
                            sql = "insert into Networkfileshare(fileid,sharerid,tosharerid,sharedate,sharetime,sharetype,filetype,msgId) values(?,?,?,?,?,?,?,?)";
                            rs.executeUpdate(sql,fid,user.getUID(),userid,date,time,1,2,folderMes);
                        }
                    }
                }
                if(!groupids.isEmpty()){
                    for(String fid : folderids.split(",")){
                        if(fid.isEmpty()) continue;
                        String folderMes = Util.null2String((String) params.get("folder_" + fid));
                        for(String groupid : groupids.split(",")){
                            if(groupid.isEmpty()) continue;
                            sql = "insert into Networkfileshare(fileid,sharerid,tosharerid,sharedate,sharetime,sharetype,filetype,msgId) values(?,?,?,?,?,?,?,?)";
                            rs.executeUpdate(sql,fid, user.getUID(),groupid,date,time,2,2,folderMes);
                        }
                    }
                }
            }
            apidatas.put("sql",sql);
            apidatas.put("api_status", true);
            apidatas.put("success",true);
        }catch (Exception ex){
            ex.printStackTrace();
            apidatas.put("api_status", false);
            apidatas.put("errorMsg", SystemEnv.getHtmlLabelName(500545,user.getLanguage()));//"操作异常"
            apidatas.put("msg", "catch exception : " + ex.getMessage());
            //记录异常日志
            new BaseBean().writeLog ("CloudDiskShareFolderCmd--->:"+ ex.getMessage());

        }
        return apidatas;
    }
}
