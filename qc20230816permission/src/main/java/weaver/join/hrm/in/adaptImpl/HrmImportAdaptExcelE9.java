/**
 * Title:        Excel人员导入适配器a
 * Company:      泛微软件
 *
 * @author: 冯拥兵
 * @version: 1.0
 * create date : 2010-6-2
 * modify log:
 * <p>
 * Description:  对Excel人员导入模板进行验证和数据解析，形成人员数据Map集合
 * Excel读取方案，通过初始化字段，形成标准的导入字段列名数组与数据库字段数组，然后通过从Excel读取的模板字段名数组
 * 的名称，建立Excel字段列号与数据库导入字段的Map映射关系，通过列号就可以找到对应的导入字段，就可以实现模板列的动态
 * 调整，同时可以通过反正找到对应的属性字段
 */
package weaver.join.hrm.in.adaptImpl;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

//import org.apache.poi.hssf.usermodel.HSSFCell;
import com.alibaba.fastjson.JSON;
import com.api.customization.qc20230816.permission.util.CheckSetUtil;
import com.engine.hrm.biz.HrmClassifiedProtectionBiz;
import com.engine.hrm.util.HrmWeakPasswordUtil;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
//import org.apache.poi.hssf.usermodel.HSSFRow;
//import org.apache.poi.hssf.usermodel.HSSFSheet;
//import org.apache.poi.hssf.usermodel.HSSFWorkbook;
//import org.apache.poi.poifs.filesystem.POIFSFileSystem;

import org.apache.poi.ss.usermodel.*;
//import org.apache.poi.ss.usermodel.Workbook;
//import org.apache.poi.ss.usermodel.WorkbookFactory;
import weaver.common.DateUtil;
import weaver.conn.RecordSet;
import weaver.file.FileManage;
import weaver.file.FileUploadToPath;
import weaver.file.ImageFileManager;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.settings.ChgPasswdReminder;
import weaver.hrm.settings.RemindSettings;
import weaver.join.hrm.in.HrmResourceVo;
import weaver.join.hrm.in.IHrmImportAdapt;
import weaver.join.hrm.in.processImpl.HrmImportProcess;
import weaver.systeminfo.SystemEnv;

import static com.engine.hrm.cmd.importresource.SaveImportResourceCmd.threadLocal;

public class HrmImportAdaptExcelE9 extends BaseBean implements IHrmImportAdapt {

    private String fileName;

    private String importType;

    private Sheet sheet;

    private int sheetCount = 0;

    private List errorInfo = new ArrayList();

    private String temFields[];

    //标准模板字段
    private String tempField = "分部,部门," +
            "编号,姓名,登录名,密码,安全级别,性别," +
            "岗位,职务,职务类型,职称,职级," +
            "职责描述,直接上级,助理,状态," +
            "办公室,办公地点,办公电话,移动电话,其他电话," +
            "传真,电子邮件,系统语言,出生日期,民族," +
            "籍贯,户口,身份证号码," +
            "婚姻状况,政治面貌,入团日期,入党日期," +
            "工会会员,学历,学位,健康状况,身高," +
            "体重,用工性质,合同开始日期,合同结束日期,试用期结束日期,现居住地,家庭联系方式,暂住证号码," +
            "入职日期,参加工作日期," +
            "公积金帐户,账号类型,主账号,身高(cm),体重(kg),工资账号户名,工资银行,工资账号," +
            "办公室电话,显示顺序,人员密级";

    //存放必填列下标数组，包含自定义字段中的必填列
    private String requiredFields[];

    //必填列标准模板或字段下标
    private String requiredField = "0,1,3,8,9,10,18";

    //标准导入字段数组
    private String voFields[];

    //标准导入字段
    private String voField = "subcompanyid1,departmentid," +
            "workcode,lastname,loginid,password,seclevel,sex," +
            "jobtitle,jobactivityid,jobgroupid,jobcall,joblevel," +
            "jobactivitydesc,managerid,assistantid,status," +
            "workroom,locationid,telephone,mobile,mobilecall," +
            "fax,email,systemlanguage,birthday,folk," +
            "nativeplace,regresidentplace,certificatenum," +
            "maritalstatus,policy,bememberdate,bepartydate," +
            "islabouunion,educationlevel,degree,healthinfo,height," +
            "weight,usekind,startdate,enddate,probationenddate," +
            "residentplace,homeaddress,tempresidentnumber," +
            "companystartdate,workstartdate," +
            "accumfundaccount,accounttype,belongto,height,weight,accountname,bankid1,accountid1," +
            "telephone,dsporder,classification";

    //基本信息字段数组最后下标
    private int baseFieldsLastIndex = 0;

    //基础自定义信息字段数组最后下标
    private int baseFieldsLastIndex1 = 0;

    //个人自定义信息数组最后下标
    private int personFieldsLastIndex = 0;

    //工作自定义信息数组最后下标
    private int workFieldLastIndex = 0;

    // 重复性验证标准数据库字段下标 例如
    private int keyFieldIndex = 2;

    //  重复性验证excel字段下标
    private int keyColumn = -1;

    // 用于做重复性验证，重复性与行键值对，<编号,4>,<编号,5>
    private Map repeatKeyMap = new HashMap();

    private int certificateNumIndex = 29;
    private int certificateNumColumn = -1;
    private Map<Object,String> certificateNums = new HashMap<>();

    Map<String,Map> checkInfos = new HashMap();

    private String[] checkKeys = new String[]{"loginid","workcode"};

    private int accounttypeIndex = 50;
    private int accounttypeColumn = -1;

    // 用于存贮HrmResource所有方法参数内容格式为<methodNmae,Method对象>,通过fieldsMaping中的对应关系可以找到方法和参数
    private Map parameterTypes = new HashMap();

    // HrmResourceVo povo对象属性类型，用于反射赋值
    private Map fieldTypes = new HashMap();

    //excel字段数组下标与标准字段数组下标对应关系<excelFieldIndex,voFieldIndex>
    private Map fieldsMap = new HashMap();

    //人员map集合
    private LinkedHashMap hrmResourceMap = new LinkedHashMap();

    Map cusFieldValMap = new HashMap();     //个人信心自定义字段与工作信息自定字段数据验证条件

    private int userlanguage = 7;   //登录语言

    //弱密码禁止保存：false-允许保存弱密码、true-不允许保存弱密码
    private boolean weakPasswordDisable = false;
    //判断弱密码
    private HrmWeakPasswordUtil hrmWeakPasswordUtil;
    //qc20230816 自定义修改权限
    private String isOpen = new BaseBean().getPropValue("qc20230816permission","isopen");

    /**
     * map集合创建类
     * @param fu   上传参数
     * @return List
     */
    public List creatImportMap(FileUploadToPath fu) {
        try {
            writeLog("lxr2018>>>language2=" + userlanguage);

            //没开启了分级保护
            Boolean isOpen= HrmClassifiedProtectionBiz.isOpenClassification();
            if(!isOpen){
                voField = "subcompanyid1,departmentid," +
                        "workcode,lastname,loginid,password,seclevel,sex," +
                        "jobtitle,jobactivityid,jobgroupid,jobcall,joblevel," +
                        "jobactivitydesc,managerid,assistantid,status," +
                        "workroom,locationid,telephone,mobile,mobilecall," +
                        "fax,email,systemlanguage,birthday,folk," +
                        "nativeplace,regresidentplace,certificatenum," +
                        "maritalstatus,policy,bememberdate,bepartydate," +
                        "islabouunion,educationlevel,degree,healthinfo,height," +
                        "weight,usekind,startdate,enddate,probationenddate," +
                        "residentplace,homeaddress,tempresidentnumber," +
                        "companystartdate,workstartdate," +
                        "accumfundaccount,accounttype,belongto,height,weight,accountname,bankid1,accountid1," +
                        "telephone,dsporder";
                tempField = "分部,部门," +
                        "编号,姓名,登录名,密码,安全级别,性别," +
                        "岗位,职务,职务类型,职称,职级," +
                        "职责描述,直接上级,助理,状态," +
                        "办公室,办公地点,办公电话,移动电话,其他电话," +
                        "传真,电子邮件,系统语言,出生日期,民族," +
                        "籍贯,户口,身份证号码," +
                        "婚姻状况,政治面貌,入团日期,入党日期," +
                        "工会会员,学历,学位,健康状况,身高," +
                        "体重,用工性质,合同开始日期,合同结束日期,试用期结束日期,现居住地,家庭联系方式,暂住证号码," +
                        "入职日期,参加工作日期," +
                        "公积金帐户,账号类型,主账号,身高(cm),体重(kg),工资账号户名,工资银行,工资账号," +
                        "办公室电话,显示顺序";
            }
            // 初始化数据
            initDataSource(fu);

            if (!errorInfo.isEmpty()) {
                deleteFile();
                return errorInfo;
            }
            //初始化模板字段
            initTempFields();

            // 模板验证
            valExcelTemp();

            if (!errorInfo.isEmpty()) {
                deleteFile();
                return errorInfo;
            }

            // 读取数据并验证
            readExcel();

            deleteFile();
            return errorInfo;
        } catch (NegativeArraySizeException e) {
            errorInfo.add(SystemEnv.getHtmlLabelName(83615, userlanguage));//目前尚不清楚，这种异常的产生原因
            writeLog(e);
            return errorInfo;
        } catch (Exception e) {
            errorInfo.add(SystemEnv.getHtmlLabelName(83617, userlanguage));//Excel导入错误，请阅读注意事项并检查模板文件
            writeLog(e);
            return errorInfo;
        }

    }

    private void deleteFile() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(30 * 1000);
                    ImageFileManager.deletePdfImageFile(Util.getIntValue(fileName));
                } catch (InterruptedException e) {
                    writeLog(e);
                }
            }
        }).start();
    }

    /**
     * 获取人员map
     */
    public Map getHrmImportMap() {

        return hrmResourceMap;
    }

    /**
     * 获取上传文件，初始化参数
     * @param fu
     * @return
     */
    public List initDataSource(FileUploadToPath fu) {
        this.importType = fu.getParameter("importType");

        // 重复性验证字段
        String keyField = fu.getParameter("keyField");

        voFields = voField.split(",");

        for (int i = 0; i < voFields.length; i++) {
            if (keyField.equals(voFields[i])) {
                keyFieldIndex = i;
            }
            if ("certificatenum".equalsIgnoreCase(voFields[i])) {
                certificateNumIndex = i;
            }
            if ("accounttype".equalsIgnoreCase(voFields[i])) {
                accounttypeIndex = i;
            }
            if (keyFieldIndex != 2 && certificateNumIndex != 29) break;
        }
        try {
            Workbook workbook = null;
            this.fileName = fu.getParameter("excelfile");
            ImageFileManager manager = new ImageFileManager();
            manager.getImageFileInfoById(Util.getIntValue(this.fileName));
            workbook = WorkbookFactory.create(manager.getInputStream());//new HSSFWorkbook(new POIFSFileSystem(manager.getInputStream()));
            this.sheetCount = workbook.getNumberOfSheets();
            this.sheet = workbook.getSheetAt(this.sheetCount > 1 ? 1 : 0);
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
            errorInfo.add(SystemEnv.getHtmlLabelName(83618, userlanguage));                    //上传文件不是excel文件
            writeLog(e);
        } catch (IndexOutOfBoundsException e) {
            errorInfo.add(SystemEnv.getHtmlLabelName(83619, userlanguage));
            writeLog(e);
        }

        ChgPasswdReminder reminder = new ChgPasswdReminder();
        RemindSettings settings = reminder.getRemindSettings();
        //判断是否启用【弱密码禁止保存】
        this.weakPasswordDisable = Util.null2String(settings.getWeakPasswordDisable()).equals("1");
        try {
            //判断是否为弱密码
            hrmWeakPasswordUtil = new HrmWeakPasswordUtil();
        } catch (Exception e) {
            errorInfo.add("加载弱密码集合出错");
            writeLog(e);
        }
        return errorInfo;
    }

    /**
     * 验证模板格式
     *
     * @return
     */
    public List valExcelTemp() {
        int rowIndex = (this.sheetCount > 1 ? 1 : 0);
        Row row;
        Cell cell;
        String cellValue;
        List fieldList = new ArrayList();
        try {
             row = sheet.getRow(rowIndex);
            int first = row.getFirstCellNum();
            int last = row.getLastCellNum();
            String field = "";
            for (int i = 0; i < row.getLastCellNum(); i++) {
                boolean flag = false;
                cell = row.getCell((short) i);
                if (cell != null) {
                    cellValue = getCellValue(cell).trim();
                    for (int k = 0; k < temFields.length; k++) {
                        if (cellValue.equals(temFields[k])) {
                            fieldsMap.put(new Integer(i), new Integer(k));
                            fieldList.add(cellValue);
                            if (keyFieldIndex == k)  //如果重复性验证标准数据库字段下标与标准excel模板下标相等，则找到对应的excel验证列
                                keyColumn = i;
                            if (certificateNumIndex == k) {
                                certificateNumColumn = i;
                            }
                            if (accounttypeIndex == k) {
                                accounttypeColumn = i;
                            }
                            flag = true;
                            break;
                        }
                    }
                    if (!flag)
                        errorInfo.add(getCellPosition(i, (rowIndex + 1)) + "[" + cellValue + "]" + " 不是模板中字段，请检查是否有误");      //不是模板中字段，请检查是否有误
                    if (importType.equals("add")) {  //如果是插入操作才验证固定列
                        if (i == 0 && !cellValue.equals(temFields[0]))
                            errorInfo.add("分部必须在第" + (rowIndex + 1) + "行第1列");   //【分部】固定列
                        if (i == 1 && !cellValue.equals(temFields[1]))
                            errorInfo.add("部门必须在第" + (rowIndex + 1) + "行第2列");    // 【部门】固定列
                    } else {
                        //如果是更新，并且存在分部部门列，就规定分部、部门在第1、2列
                        if (cellValue.equals(temFields[0]) && !temFields[1].equals(getCellValue(rowIndex, 1).trim()))
                            errorInfo.add("更新时有分部则后一列必须为部门，且第" + (rowIndex + 1) + "行第1列为分部，第2列为部门");   //【分部】固定列
                        else if (cellValue.equals(temFields[0]) && i != 0)
                            errorInfo.add("分部必须在第" + (rowIndex + 1) + "行第1列");   //【分部】固定列


                        if (cellValue.equals(temFields[1]) && !temFields[0].equals(getCellValue(rowIndex, 0).trim()))
                            errorInfo.add("更新时有部门则前一列必须为分部，且第" + (rowIndex + 1) + "行第1列为分部，第2列为部门");   //【分部】固定列
                        else if (cellValue.equals(temFields[1]) && i != 1)
                            errorInfo.add("部门必须在第" + (rowIndex + 1) + "行第2列");   //【部门】固定列

                    }
                }
            }
            if (keyColumn == -1)
                errorInfo.add("[" + temFields[keyFieldIndex] + "]" + " 您所选的重复性验证字段不在模板中");  //您所选的重复性验证字段不在模板中

            if (importType.equals("add")) {  //如果是插入操作才验证必填列
                for (int j = 0; j < requiredFields.length; j++) {
                    boolean flag = false;
                    for (int k = 0; k < fieldList.size(); k++) {
                        if (temFields[Integer.parseInt(requiredFields[j])].equals((String) fieldList.get(k))) {
                            flag = true;
                            break;
                        }
                    }
                    if (!flag)
                        errorInfo.add("[" + temFields[Integer.parseInt(requiredFields[j])] + "]" + " 为必填字段");  //必填字段
                }
            }
        } catch (IllegalArgumentException e) {
            errorInfo.add(SystemEnv.getHtmlLabelName(83617, userlanguage));             //   excel模板有误时
            writeLog(e);
        }
        return errorInfo;
    }

    /**
     * 初始化反射所需要的方法和字段Map，parameterTypes，fieldTypes
     */
    public void initReflectParam() {
        Class hrmResourceClass = HrmResourceVo.class;
        Method hrmResourceMethods[] = hrmResourceClass.getDeclaredMethods();

        for (int i = 0; i < hrmResourceMethods.length; i++) {
            parameterTypes.put(hrmResourceMethods[i].getName(),
                    hrmResourceMethods[i]);
        }

        Field hrmResourceFields[] = hrmResourceClass.getDeclaredFields();

        for (int i = 0; i < hrmResourceFields.length; i++) {
            Class fieldTypeClass = hrmResourceFields[i].getType();
            fieldTypes.put(hrmResourceFields[i].getName(), fieldTypeClass
                    .getName());
        }
    }

    /**
     * 读取excel数据
     *
     * @return
     */
    public List readExcel() {

        initReflectParam(); // 初始化反射参数

        boolean flag = true; // 标记验证是否通过
        Row row;
        Cell cell;
        int rowNum = 0; // 行号
        int cellNum = 0; // 列号
        int firstRow = (this.sheetCount > 1 ? 2 : 1);
        String keyValue = "";
        int lastRow = sheet.getLastRowNum();
        String subCompany = "";
        String department = "";
        String cellValue;
        int fieldIndex = 0;
        HrmResourceVo hrmResourceVo = null;

        for (int i = firstRow; i <= lastRow; i++) {
            flag = true;
            row = sheet.getRow(i);
            if (row == null) {
                errorInfo.add(SystemEnv.getHtmlLabelName(15323, userlanguage) + " " + (i + 1) + " " + SystemEnv.getHtmlLabelName(83622, userlanguage));
                continue;
            }
            rowNum = row.getRowNum();
            String baseFieldsValue = "";
            String baseFields = "";
            String personFieldsValue = "";
            String workFieldsValue = "";
            String personFields = "";
            String workFields = "";
            if (repeatValidate(row, rowNum)) { // 重复性验证
              continue;
            }

            hrmResourceVo = new HrmResourceVo();

            for (int cellIndex = 0; cellIndex < fieldsMap.size(); cellIndex++) {

                cell = row.getCell((short) cellIndex);

                cellNum = cellIndex;


                fieldIndex = ((Integer) fieldsMap.get(new Integer(cellNum))).intValue();
//                if (cell == null && !(Util.null2String(voFields[fieldIndex]).trim()).startsWith("field")) {
//
//                    continue;
//                }

                cellValue = getCellValue(cell).trim();

                if (valExcelDataFormate(rowNum, cellNum, cellValue)) {
                    if (fieldIndex == keyFieldIndex) { // 验证通过，如果当前单元格为重复性验证单元格，则把值赋给keyValue
                        keyValue = cellValue;
                    }
                } else {
                    flag = false;
                }

                if (flag) { // 为true则，设置值，否则不需要

                    if (fieldIndex == 0 && getCellValue(1, cellNum).equals("分部")) { // 对于excel分部为空则取上面分部读取的值
                        if (cellValue.equals(""))
                            cellValue = subCompany;
                        else
                            subCompany = cellValue;
                    }

                    if (fieldIndex == 1 && getCellValue(1, cellNum).equals("部门")) { // 对于excel部门为空则取上面部门读取的值
                        if (cellValue.equals(""))
                            cellValue = department;
                        else
                            department = cellValue;
                    }

                    if (fieldIndex <= baseFieldsLastIndex) {
                        setHrmResourceValue(fieldIndex, cellValue, hrmResourceVo, null);
                    }

                    if (fieldIndex > baseFieldsLastIndex && fieldIndex <= baseFieldsLastIndex1) {
                        baseFields = baseFields + "," + voFields[fieldIndex];
                        if (cellValue.equals(""))
                            cellValue = "?";
                        baseFieldsValue += ";" + cellValue;
                        setHrmResourceValue(0, baseFields.substring(1), hrmResourceVo, "baseFields");
                        setHrmResourceValue(0, baseFieldsValue.substring(1), hrmResourceVo, "baseFieldsValue");
                    }

                    if (fieldIndex > baseFieldsLastIndex1 && fieldIndex <= personFieldsLastIndex) {
                        personFields = personFields + "," + voFields[fieldIndex];
                        if (cellValue.equals(""))
                            cellValue = "?";
                        personFieldsValue += ";" + cellValue;
                        setHrmResourceValue(0, personFields.substring(1), hrmResourceVo, "personFields");
                        setHrmResourceValue(0, personFieldsValue.substring(1), hrmResourceVo, "personFieldsValue");
                    }

                    if (fieldIndex > personFieldsLastIndex && fieldIndex <= workFieldLastIndex) {
                        workFields = workFields + "," + voFields[fieldIndex];
                        if (cellValue.equals(""))
                            cellValue = "?";
                        workFieldsValue += ";" + cellValue;
                        setHrmResourceValue(0, workFields.substring(1), hrmResourceVo, "workFields");
                        setHrmResourceValue(0, workFieldsValue.substring(1), hrmResourceVo, "workFieldsValue");
                    }

                }
            }
            //设置status 默认值0 试用
            //if(Util.null2String(hrmResourceVo.getStatus()).length()==0){
            //hrmResourceVo.setStatus("0");hrmResourceVo
            //}
            String currentDate = DateUtil.getCurrentDate();
            if (Util.null2String(hrmResourceVo.getWorkstartdate()).length() > 0 && Util.null2String(hrmResourceVo.getWorkyear()).length() == 0) {//计算工龄
                int day = DateUtil.dayDiff(hrmResourceVo.getWorkstartdate(), currentDate);
                DecimalFormat df = new DecimalFormat("0.0");
                hrmResourceVo.setWorkyear(df.format(day / 365.0));
            }

            if (Util.null2String(hrmResourceVo.getCompanystartdate()).length() > 0 && Util.null2String(hrmResourceVo.getCompanyworkyear()).length() == 0) {//计算司令
                int day = DateUtil.dayDiff(hrmResourceVo.getCompanystartdate(), currentDate);
                DecimalFormat df = new DecimalFormat("0.0");
                hrmResourceVo.setCompanyworkyear(df.format(day / 365.0));
            }

            repeatKeyMap.put(keyValue, "" + (i + 1)); // 用于重复性验证
          if (hrmResourceVo.getAccounttype() != null && !"".equals(hrmResourceVo.getAccounttype())) {
            if ("主账号".equals(hrmResourceVo.getAccounttype())) {
              certificateNums.put(hrmResourceVo.getCertificatenum(), String.valueOf(i + 1));
            }
          }else{
            certificateNums.put(hrmResourceVo.getCertificatenum(), String.valueOf(i + 1));
          }

            if (checkInfo(hrmResourceVo, rowNum)) { // 重复性验证
                continue;
            }

            //新增账号、工号唯一性校验
            for(String key : checkKeys){
                Map<String,String> checkInfo = checkInfos.get(key);
                if(checkInfo==null){
                    checkInfo = new HashMap<>();
                    checkInfos.put(key,checkInfo);
                }else{
                    checkInfo = checkInfos.get(key);
                }
                String val = "";
                if(key.equals("loginid")){
                    val = hrmResourceVo.getLoginid();
                }else if(key.equals("workcode")){
                    val = hrmResourceVo.getWorkcode();
                }else if(key.equals("certificatenum")){
                    val = hrmResourceVo.getCertificatenum();
                }
                checkInfo.put(val,String.valueOf(i + 1));
                checkInfos.put(key,checkInfo);
            }

            if (flag) {
                hrmResourceMap.put(keyValue, hrmResourceVo);
            }
        }
        return errorInfo;
    }

    /**
     * 重复性验证，读取唯一性cell值，然后从HrmResourceMap中查找是否存在
     *
     * @param row       行
     * @param rowNum    行下标
     * @return
     */
    public boolean repeatValidate(Row row, int rowNum) {
        if(certificateNumColumn == -1)
            return false;
        String certificateNum = getCellValue(row.getCell((short) certificateNumColumn)).trim();
        String accounttype = getCellValue(row.getCell((short) accounttypeColumn)).trim();
        String key = getCellValue(row.getCell((short) keyColumn)).trim();
        if (voFields[((Integer) fieldsMap.get(new Integer(keyColumn))).intValue()].equals(""))
            key = getCellValue(row.getCell((short) 0)).trim() + ">" + getCellValue(row.getCell((short) 1)).trim() + "_" + key;
        String repeatRow = (String) repeatKeyMap.get(key);
        String cnRow = (String) certificateNums.get(certificateNum);
        if (repeatRow != null) {
            errorInfo.add((rowNum + 1) + "," + repeatRow + " 行重复");  //重复 num_5,6 行重复
            return true;
        } else if (cnRow != null && certificateNum.length() > 0) {
          if("次账号".equalsIgnoreCase(accounttype)){
            return false;
          }else{
            errorInfo.add((rowNum + 1) + "," + cnRow + " " + SystemEnv.getHtmlLabelName(83623, userlanguage));
            return true;
          }
        } else {
          return false;
        }
    }

    private boolean checkInfo(HrmResourceVo hrmResourceVo, int rowNum){
        //新增账号、工号唯一性校验
        for(String key : checkKeys){
            Map<String,String> checkInfo = checkInfos.get(key);
            if(checkInfo!=null && !checkInfo.isEmpty()){
                String val = "";
                String errorMsg = "";
                if(key.equals("loginid")){
                    val = Util.null2String(hrmResourceVo.getLoginid()).trim();
                    errorMsg = SystemEnv.getHtmlLabelName(520127, userlanguage);
                }else if(key.equals("workcode")){
                    val = Util.null2String(hrmResourceVo.getWorkcode()).trim();
                    errorMsg = SystemEnv.getHtmlLabelName(520128, userlanguage);
                }else if(key.equals("certificatenum")){
                    val = Util.null2String(hrmResourceVo.getCertificatenum()).trim();
                    errorMsg = SystemEnv.getHtmlLabelName(83623, userlanguage);
                }
                String cnRow = Util.null2String(checkInfo.get(val));
                if (cnRow.length()>0 &&  val .length() > 0) {
                    errorInfo.add((rowNum + 1) + "," + cnRow + errorMsg);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 验证数据格式，必填列是否为空
     * @param rowNum      行号
     * @param cellNum     列号
     * @param cellValue   单元格值
     * @return
     */
    public boolean valExcelDataFormate(int rowNum, int cellNum, String cellValue) {

        boolean flag = true;
        boolean result = false;
        String msg = "";
        int fieldIndex = ((Integer) fieldsMap.get(new Integer(cellNum))).intValue();
        String fieldName = voFields[fieldIndex];
        new BaseBean().writeLog("==zj==(fieldName)" + JSON.toJSONString(fieldName));
        //qc20230816 这里判断字段是否有修改权限
        CheckSetUtil checkSetUtil = new CheckSetUtil();
        writeLog("==zj==线程获取"+threadLocal.get());
        try {
            List<String> uploadCheckList = checkSetUtil.uploadCheck(threadLocal.get());
            new BaseBean().writeLog("==zj==(uploadCheckList)" + JSON.toJSONString(uploadCheckList));
            if (uploadCheckList!=null && "1".equals(isOpen)&&!"1".equals(threadLocal.get())){
                Boolean isHave = false;
                for (int i = 0; i < uploadCheckList.size(); i++) {
                    if (fieldName.equals(uploadCheckList.get(i))){
                        isHave = true;
                    }
                }
                //单独处理下登录名，安全级别，密码
                if ("loginid".equals(fieldName) || "seclevel".equals(fieldName) || "password".equals(fieldName)){
                    isHave = true;
                }
                //如果没有修改权限，看下列有没有值，有值则返回错误信息
                if (!isHave){
                    //且列不为空
                    if (!getCellValue(rowNum, cellNum - 1).equals("")) {
                        flag = false;
                        msg = getCellPosition(cellNum, rowNum + 1) + "[" + temFields[fieldIndex] + "]";
                        errorInfo.add(msg + " " + "请确认是否有修改权限");
                        return flag;
                    }
                }
            }
        } catch (Exception e) {
            new BaseBean().writeLog("==zj==(导入判断权限错误)" + JSON.toJSONString(e));
        }

//		if (fieldName.equals("status")&&getCellValue(1, cellNum).equals("状态")) {
//			if("离职".equals(cellValue) || "解聘".equals(cellValue) || "退休".equals(cellValue) || "无效".equals(cellValue)) {
//				msg = getCellPosition(cellNum, rowNum+1)+"["+temFields[fieldIndex]+"]";
//				errorInfo.add(msg+cellValue+SystemEnv.getHtmlLabelName(129587,userlanguage));
//				return flag;
//			}
//		}

        // 人员信息第1行(模板第2行)的分部和部门为必填

        if ((rowNum == 1 || rowNum == 2) && fieldName.equals("subcompanyid1") && getCellValue(1, cellNum).equals("分部") && cellValue.equals("")) {
            flag = false;
            msg = getCellPosition(cellNum, rowNum + 1) + "[" + temFields[fieldIndex] + "]";           //【分部】为必填
            errorInfo.add(msg + " " + SystemEnv.getHtmlLabelName(83626, userlanguage));
            return flag;
        }

        if ((rowNum == 1 || rowNum == 2) && fieldName.equals("departmentid") && getCellValue(1, cellNum).equals("部门") && cellValue.equals("")) {
            flag = false;
            msg = getCellPosition(cellNum, rowNum + 1) + "[" + temFields[fieldIndex] + "]";           //【部门】为必填
            errorInfo.add(msg + " " + SystemEnv.getHtmlLabelName(83626, userlanguage));
            return flag;

        }
        //【分部】不为空则【部门】为必填
        if (fieldName.equals("departmentid") && cellValue.equals("")) {
            if (!getCellValue(rowNum, cellNum - 1).equals("")) { //分部不为空
                flag = false;
                msg = getCellPosition(cellNum, rowNum + 1) + "[" + temFields[fieldIndex] + "]";
                errorInfo.add(msg + " " + SystemEnv.getHtmlLabelName(83626, userlanguage));
                return flag;
            }
        }

        // 姓名
        if (fieldName.equals("lastname") && cellValue.equals("")) {

            flag = false;
            msg = getCellPosition(cellNum, rowNum + 1) + "[" + temFields[fieldIndex] + "]";          //【姓名】为必填
            errorInfo.add(msg + " " + SystemEnv.getHtmlLabelName(83626, userlanguage));
            return flag;
        }

        // 岗位
        if (fieldName.equals("jobtitle") && cellValue.equals("")) {
            flag = false;
            msg = getCellPosition(cellNum, rowNum + 1) + "[" + temFields[fieldIndex] + "]";          //【岗位】为必填
            errorInfo.add(msg + " " + SystemEnv.getHtmlLabelName(83626, userlanguage));
            return flag;
        }

        if (fieldName.equals("jobactivityid") && cellValue.equals("")) {
            flag = false;
            msg = getCellPosition(cellNum, rowNum + 1) + "[" + temFields[fieldIndex] + "]";          //【职务】为必填
            errorInfo.add(msg + " " + SystemEnv.getHtmlLabelName(83626, userlanguage));
            return flag;
        }

//        if (fieldName.equals("companystartdate") && cellValue.equals("")) {
//            flag = false;
//            msg = getCellPosition(cellNum, rowNum + 1) + "[" + temFields[fieldIndex] + "]";          //【入职日期】为必填
//            errorInfo.add(msg + " " + SystemEnv.getHtmlLabelName(83626, userlanguage));
//            return flag;
//        }
//
//        if (fieldName.equals("workstartdate") && cellValue.equals("")) {
//            flag = false;
//            msg = getCellPosition(cellNum, rowNum + 1) + "[" + temFields[fieldIndex] + "]";          //【参加工作日期】为必填
//            errorInfo.add(msg + " " + SystemEnv.getHtmlLabelName(83626, userlanguage));
//            return flag;
//        }

        if (fieldName.equals("jobgroupid") && cellValue.equals("")) {
            flag = false;
            msg = getCellPosition(cellNum, rowNum + 1) + "[" + temFields[fieldIndex] + "]";         //【职务类型】为必填
            errorInfo.add(msg + " " + SystemEnv.getHtmlLabelName(83626, userlanguage));
            return flag;
        }


        // 办公地点
        if (fieldName.equals("locationid") && cellValue.equals("")) {
            flag = false;
            msg = getCellPosition(cellNum, rowNum + 1) + "[" + temFields[fieldIndex] + "]";         //【办公地点】为必填
            errorInfo.add(msg + " " + SystemEnv.getHtmlLabelName(83626, userlanguage));
            return flag;
        }

        //自定义字段为必填设置时
        if (fieldIndex > baseFieldsLastIndex && requiredField.contains(String.valueOf(fieldIndex))) {
            if (cellValue.equals("")) {
                flag = false;
                msg = getCellPosition(cellNum, rowNum + 1) + "[" + temFields[fieldIndex] + "]";
                errorInfo.add(msg + " " + SystemEnv.getHtmlLabelName(83626, userlanguage));
                return flag;
            }
        }

        //基本信息、个人信息、工作信息自定义验证
        if (fieldIndex > baseFieldsLastIndex && !cellValue.equals("")) {
            //自定义信息字段不能包含‘?’问号
            if (cellValue.contains("?") && getStrLength(cellValue) == 1) {
                flag = false;
                msg = getCellPosition(cellNum, rowNum + 1) + "[" + temFields[fieldIndex] + "]";
                errorInfo.add(msg + " " + SystemEnv.getHtmlLabelName(83627, userlanguage));
                return flag;
            }
            String valType = (String) cusFieldValMap.get(new Integer(fieldIndex));
            if (valType != null)
                if (valType.equals("isInt")) {
                    if (!isInteger(cellValue)) {
                        flag = false;
                        msg = getCellPosition(cellNum, rowNum + 1) + "[" + temFields[fieldIndex] + "]";
                        errorInfo.add(msg + " " + SystemEnv.getHtmlLabelName(83628, userlanguage));
                        return flag;
                    }
                    if (getStrLength(cellValue) > 18 && (Integer.MAX_VALUE < Double.parseDouble(cellValue) || Double.parseDouble(cellValue) < Integer.MIN_VALUE)) {
                        flag = false;
                        msg = getCellPosition(cellNum, rowNum + 1) + "[" + temFields[fieldIndex] + "]";
                        errorInfo.add(msg + " " + SystemEnv.getHtmlLabelName(83629, userlanguage) + Integer.MAX_VALUE + SystemEnv.getHtmlLabelName(15508, userlanguage) + Integer.MIN_VALUE);
                        return flag;
                    }

                } else if (valType.equals("isDouble")) {
                    if (!isDecimal(cellValue)) {
                        flag = false;
                        msg = getCellPosition(cellNum, rowNum + 1) + "[" + temFields[fieldIndex] + "]";
                        errorInfo.add(msg + " " + SystemEnv.getHtmlLabelName(83637, userlanguage));
                        return flag;
                    }
                    if (getStrLength(cellValue) > 18) {
                        flag = false;
                        msg = getCellPosition(cellNum, rowNum + 1) + "[" + temFields[fieldIndex] + "]";
                        errorInfo.add(msg + " " + SystemEnv.getHtmlLabelName(83639, userlanguage));
                    }
                } else if (valType.equals("isDate")) {
                    if (getStrLength(cellValue) != 10) {
                        flag = false;
                        msg = getCellPosition(cellNum, rowNum + 1) + "[" + temFields[fieldIndex] + "]";
                        errorInfo.add(msg + " " + SystemEnv.getHtmlLabelName(83640, userlanguage));
                    }

                } else if (isInteger(valType) && getStrLength(cellValue) > Integer.parseInt(valType)) {
                    flag = false;
                    msg = getCellPosition(cellNum, rowNum + 1) + "[" + temFields[fieldIndex] + "]";
                    errorInfo.add(msg + " " + SystemEnv.getHtmlLabelName(83641, userlanguage) + valType + SystemEnv.getHtmlLabelName(20075, userlanguage));
                    return flag;
                } else if (valType.equals("isCheck") && !cellValue.equals("0") && !cellValue.equals("1")) {
                    flag = false;
                    msg = getCellPosition(cellNum, rowNum + 1) + "[" + temFields[fieldIndex] + "]";
                    errorInfo.add(msg + " " + SystemEnv.getHtmlLabelName(83644, userlanguage));
                    return flag;
                }
        }

        //安全级别、职级整数格式验证、大于0验证
        if ((fieldName.equals("seclevel") || fieldName.equals("joblevel")) && !cellValue.equals("")) {
            if (fieldName.equals("seclevel")) {
                //安全级别不能大于999
                if (Util.getIntValue(cellValue) > 999) {
                    flag = false;
                    msg = getCellPosition(cellNum, rowNum + 1) + "[" + temFields[fieldIndex] + "]";
                    errorInfo.add(msg + " " + SystemEnv.getHtmlLabelName(515522, userlanguage));
                    return flag;
                }
                //安全级别不能小于-999
                if (Util.getIntValue(cellValue) < -999) {
                    flag = false;
                    msg = getCellPosition(cellNum, rowNum + 1) + "[" + temFields[fieldIndex] + "]";
                    errorInfo.add(msg + " " + SystemEnv.getHtmlLabelName(515523, userlanguage));
                    return flag;
                }
            } else {
                if (!isInteger(cellValue)) {
                    flag = false;
                    msg = getCellPosition(cellNum, rowNum + 1) + "[" + temFields[fieldIndex] + "]";
                    errorInfo.add(msg + " " + SystemEnv.getHtmlLabelName(83645, userlanguage));
                    return flag;
                }
                if (Integer.parseInt(cellValue) < 0 || Integer.parseInt(cellValue) > 1000) {
                    flag = false;
                    msg = getCellPosition(cellNum, rowNum + 1) + "[" + temFields[fieldIndex] + "]";
                    errorInfo.add(msg + " " + SystemEnv.getHtmlLabelName(83648, userlanguage));
                    return flag;
                }
            }
        }
        //身高、体重数据格式验证、大于0验证
        if ((fieldName.equals("height") || fieldName.equals("weight")) && !cellValue.equals("")) {
            if (!isDecimal(cellValue)) {
                flag = false;
                msg = getCellPosition(cellNum, rowNum + 1) + "[" + temFields[fieldIndex] + "]";
                errorInfo.add(msg + " " + SystemEnv.getHtmlLabelName(83650, userlanguage));
                return flag;
            }
            if (Double.parseDouble(cellValue) < 0 || Double.parseDouble(cellValue) > 1000) {
                flag = false;
                msg = getCellPosition(cellNum, rowNum + 1) + "[" + temFields[fieldIndex] + "]";
                errorInfo.add(msg + " " + SystemEnv.getHtmlLabelName(83648, userlanguage));
                return flag;
            }
        }

        //日期自定义日期字段长度
        if (fieldName.startsWith("datefield") && getStrLength(cellValue) > 10) {
            flag = false;
            msg = getCellPosition(cellNum, rowNum + 1) + "[" + temFields[fieldIndex] + "]";
            errorInfo.add(msg + " " + SystemEnv.getHtmlLabelName(83658, userlanguage) + "10" + SystemEnv.getHtmlLabelName(20075, userlanguage));
            return flag;
        }
        //文本自定义文本字段长度
        if (fieldName.startsWith("textfield") && getStrLength(cellValue) > 100) {
            flag = false;
            msg = getCellPosition(cellNum, rowNum + 1) + "[" + temFields[fieldIndex] + "]";
            errorInfo.add(msg + " " + SystemEnv.getHtmlLabelName(83702, userlanguage) + "100" + SystemEnv.getHtmlLabelName(20075, userlanguage));
            return flag;
        }
        //数字自定义数字字段是否为数字格式判断
        if (fieldName.startsWith("numberfield") && !cellValue.equals("") && !isDecimal(cellValue)) {
            flag = false;
            msg = getCellPosition(cellNum, rowNum + 1) + "[" + temFields[fieldIndex] + "]";
            errorInfo.add(msg + " " + SystemEnv.getHtmlLabelName(83662, userlanguage));
            return flag;
        }

        //判断自定义字段值必须为0或1
        if (fieldName.startsWith("tinyintfield") && !cellValue.equals("") && !cellValue.equals("0") && !cellValue.equals("1")) {
            flag = false;
            msg = getCellPosition(cellNum, rowNum + 1) + "[" + temFields[fieldIndex] + "]";
            errorInfo.add(msg + " " + SystemEnv.getHtmlLabelName(83664, userlanguage));
            return flag;
        }


        //长度为200个字符的标准字段
        if ((fieldName.equals("jobtitle") || fieldName.equals("jobactivityid") || fieldName.equals("jobgroupid") ||
                fieldName.equals("locationid") || fieldName.equals("jobactivitydesc") || fieldName.equals("regresidentplace")) && !cellValue.equals("") && getStrLength(cellValue) > 200) {
            flag = false;
            msg = getCellPosition(cellNum, rowNum + 1) + "[" + temFields[fieldIndex] + "]";
            errorInfo.add(msg + " " + SystemEnv.getHtmlLabelName(83666, userlanguage) + "200" + SystemEnv.getHtmlLabelName(20075, userlanguage));
            return flag;
        }

        //长度为100的标准字段
        if ((fieldName.equals("loginid") || fieldName.equals("password") || fieldName.equals("homeaddress") || fieldName.equals("nativeplace")) && !cellValue.equals("") && getStrLength(cellValue) > 100) {

            flag = false;
            msg = getCellPosition(cellNum, rowNum + 1) + "[" + temFields[fieldIndex] + "]";         //自定义日期字段长度验证
            errorInfo.add(msg + " " + SystemEnv.getHtmlLabelName(83666, userlanguage) + "100" + SystemEnv.getHtmlLabelName(20075, userlanguage));
            return flag;

        }
        //长度为60的标准字段

        if ((fieldName.equals("lastname") || fieldName.equals("password") || fieldName.equals("telephone") || fieldName.equals("mobile") || fieldName.equals("mobilecall")
                || fieldName.equals("email") || fieldName.equals("workroom") || fieldName.equals("workcode") || fieldName.equals("fax")
                || fieldName.equals("certificatenum") || fieldName.equals("jobcall") || fieldName.equals("tempresidentnumber"))
                && !cellValue.equals("") && getStrLength(cellValue) > 60) {

            flag = false;
            msg = getCellPosition(cellNum, rowNum + 1) + "[" + temFields[fieldIndex] + "]";
            errorInfo.add(msg + " " + SystemEnv.getHtmlLabelName(83666, userlanguage) + "60" + SystemEnv.getHtmlLabelName(20075, userlanguage));
            return flag;

        }

        //长度为30个字符的标准字段
        if ((fieldName.equals("degree") || fieldName.equals("policy")) && !cellValue.equals("") && getStrLength(cellValue) > 30) {
            flag = false;
            msg = getCellPosition(cellNum, rowNum + 1) + "[" + temFields[fieldIndex] + "]";
            errorInfo.add(msg + " " + SystemEnv.getHtmlLabelName(83666, userlanguage) + "30" + SystemEnv.getHtmlLabelName(20075, userlanguage));
            return flag;
        }

        //长度为10个字符的标准字段
        if ((fieldName.equals("birthday") || fieldName.equals("bememberdate") || fieldName.equals("bepartydate") ||
                fieldName.equals("startdate") || fieldName.equals("enddate") || fieldName.equals("probationenddate")) && !cellValue.equals("") && getStrLength(cellValue) > 10) {
            flag = false;
            msg = getCellPosition(cellNum, rowNum + 1) + "[" + temFields[fieldIndex] + "]";
            errorInfo.add(msg + " " + SystemEnv.getHtmlLabelName(83666, userlanguage) + "10" + SystemEnv.getHtmlLabelName(20075, userlanguage));
            return flag;
        }

        //分部各级长度验证
        if (fieldName.equals("subcompanyid1") && !cellValue.equals("")) {
            String subcompanyname[] = cellValue.split(">");
            for (int i = 0; i < subcompanyname.length; i++) {
                if (getStrLength(subcompanyname[i]) > 200)
                    flag = false;
            }
            if (!flag) {
                msg = getCellPosition(cellNum, rowNum + 1) + "[" + temFields[fieldIndex] + "]";
                errorInfo.add(msg + " " + SystemEnv.getHtmlLabelName(83667, userlanguage));
                return flag;
            }
        }

        //部门各级长度验证
        if (fieldName.equals("departmentid") && !cellValue.equals("")) {
            String departmentname[] = cellValue.split(">");
            for (int i = 0; i < departmentname.length; i++) {
                if (getStrLength(departmentname[i]) > 200)
                    flag = false;
            }
            if (!flag) {
                msg = getCellPosition(cellNum, rowNum + 1) + "[" + temFields[fieldIndex] + "]";
                errorInfo.add(msg + " " + SystemEnv.getHtmlLabelName(83668, userlanguage));
                return flag;
            }
        }

        //当启用【弱密码禁止保存】时，弱密码导入失败，失败信息为“密码安全性过弱”
        if ("password".equalsIgnoreCase(fieldName)) {
            if (weakPasswordDisable && hrmWeakPasswordUtil.isWeakPsd(cellValue)) {
                msg = getCellPosition(cellNum, rowNum + 1) + "[" + temFields[fieldIndex] + "]";
                errorInfo.add(msg + " " + SystemEnv.getHtmlLabelName(515436, userlanguage));
                return flag;
            }
        }

        if (fieldIndex == keyFieldIndex && cellValue.equals("")) {
            flag = false;
            msg = getCellPosition(cellNum, rowNum + 1) + "[" + temFields[fieldIndex] + "]";      // 重复性验证字段不在模板中
            errorInfo.add(msg + " " + SystemEnv.getHtmlLabelName(83626, userlanguage));
            return flag;
        }

        return flag;
    }

    /**
     * 通过反射为对象赋值
     *
     * @param cellNum     列号
     * @param cellValue   单元格值
     * @param hrmResource 需要赋值的HrmResource
     */
    public void setHrmResourceValue(int cellNum, String cellValue,
                                    HrmResourceVo hrmResource, String field) {
        if (field == null) {
            String excelField = voFields[cellNum];
            String methodName = "set" + excelField.substring(0, 1).toUpperCase()
                    + excelField.substring(1);
            Method method = (Method) parameterTypes.get(methodName);
            try {
                String fieldType = (String) fieldTypes.get(excelField);
                String test[] = new String[]{};
                if (fieldType.equals("java.lang.String"))
                    method.invoke(hrmResource, new Object[]{cellValue});
                else if (fieldType.equals("java.lang.Integer")) {
                    if(!cellValue.equals(""))
                        method.invoke(hrmResource, new Object[]{new Integer(Integer.parseInt(cellValue))});
                    else
                        method.invoke(hrmResource, new Object[]{new Integer(0x7fffffff)});
                } else if (fieldType.equals("java.lang.Float") && !cellValue.equals(""))
                    method.invoke(hrmResource, new Object[]{new Float(Float.parseFloat(cellValue))});
                else if (fieldType.equals("java.lang.Short") && !cellValue.equals("")) {
                    method.invoke(hrmResource, new Object[]{new Short(Short.parseShort(cellValue))});
                }
            } catch (Exception e) {
                writeLog(e);
            }
        } else {
            String methodName = "set" + field.substring(0, 1).toUpperCase()
                    + field.substring(1);
            Method method = (Method) parameterTypes.get(methodName);
            try {
                method.invoke(hrmResource, new Object[]{cellValue});
            } catch (Exception e) {
                writeLog(e);
            }
        }
    }

    /**
     * excel单元格位置转换
     *
     * @param cellIndex   列号
     * @param rowNum      行号
     * @return
     */
    public String getCellPosition(int cellIndex, int rowNum) {

        int count = cellIndex / 26;
        String cellChar = String.valueOf((char) (cellIndex % 26 + 65));
        String cellPosition = "";

        if (count != 0)
            cellPosition = String.valueOf((char) ((count - 1) + 65)) + cellChar;
        else
            cellPosition = cellChar;
        cellPosition += rowNum;
        return cellPosition;
    }

    /**
     *初始化模板字段
     */
    public void initTempFields() {
        RecordSet recordSet = new RecordSet();

        String baseTempFields = "";    //excel模板字段
        String baseFields = "";        //数据库对应字段

//        recordSet.executeProc("Base_FreeField_Select", "hr");
//
//        recordSet.first();
//
//        String fields = "";
//        Map fieldsMap = new LinkedHashMap();
//
//        //获取基本信息字段
//        for (int i = 1; i <= 5; i++) {
//            if (recordSet.getString(i * 2 + 1).equals("1")) {
//                baseFields += "," + "datefield" + i;
//                baseTempFields += "," + SystemEnv.getHtmlLabelName( Integer.parseInt(recordSet.getString(i * 2)) , userlanguage);
//            }
//        }
//
//        for (int i = 1; i <= 5; i++) {
//            if (recordSet.getString(i * 2 + 11).equals("1")) {
//                baseFields += "," + "numberfield" + i;
//                baseTempFields += "," + SystemEnv.getHtmlLabelName( Integer.parseInt(recordSet.getString(i * 2 + 10)) , userlanguage);
//            }
//        }
//        for (int i = 1; i <= 5; i++) {
//            if (recordSet.getString(i * 2 + 21).equals("1")) {
//                baseFields += "," + "textfield" + i;
//                baseTempFields += "," + SystemEnv.getHtmlLabelName( Integer.parseInt(recordSet.getString(i * 2 + 20)) , userlanguage);
//            }
//        }
//        for (int i = 1; i <= 5; i++) {
//
//            if (recordSet.getString(i * 2 + 31).equals("1")) {
//                baseFields += "," + "tinyintfield" + i;
//                baseTempFields += "," + SystemEnv.getHtmlLabelName( Integer.parseInt(recordSet.getString(i * 2 + 30)) , userlanguage);
//
//            }
//        }
//
//
//        voField = voField + baseFields;
//        tempField = tempField + baseTempFields;
        writeLog("initTempFields");

        temFields = tempField.split(",");

        baseFieldsLastIndex = temFields.length - 1;

        int requiredFieldIndex = baseFieldsLastIndex;

        /* end base fields*/

        /*基本信息信息字段*/

        String baseSql = "select t1.fieldid,t1.hrm_fieldlable,t1.ismand,t2.fielddbtype,t2.fieldhtmltype,t2.type from cus_formfield t1, cus_formdict t2 where t1.scope='HrmCustomFieldByInfoType' and t1.scopeid=-1 and t1.fieldid=t2.id order by t1.fieldorder";
        recordSet.execute(baseSql);
        String baseField = "";
        String baseFieldLabel = "";
        String fieldhtmltype = "";
        while (recordSet.next()) {
            fieldhtmltype = recordSet.getString("fieldhtmltype");
            if (fieldhtmltype.equals("1") || fieldhtmltype.equals("2") || fieldhtmltype.equals("3")
                    || fieldhtmltype.equals("4") || fieldhtmltype.equals("5")) {
                baseField = baseField + ",field" + recordSet.getInt("fieldid");
                baseFieldLabel = baseFieldLabel + "," + recordSet.getString("hrm_fieldlable");
                requiredFieldIndex++;
                //验证条件
                //1为html文本类型
                if (fieldhtmltype.equals("1")) {
                    if (recordSet.getString("type").equals("1")) {           //1为文本数据类型
                        String fielddbtype = recordSet.getString("fielddbtype");
                        fielddbtype = fielddbtype.substring(fielddbtype.indexOf("(") + 1, fielddbtype.lastIndexOf(")")); //截取字符设置的长度
                        cusFieldValMap.put(new Integer(requiredFieldIndex), fielddbtype);
                    } else if (recordSet.getString("type").equals("2")) {     // 2为整数数据类型
                        cusFieldValMap.put(new Integer(requiredFieldIndex), "isInt");
                    } else if (recordSet.getString("type").equals("3")) {     // 3为浮点数数据类型
                        cusFieldValMap.put(new Integer(requiredFieldIndex), "isDouble");
                    } else
                        cusFieldValMap.put(new Integer(requiredFieldIndex), "");
                } else if (fieldhtmltype.equals("4")) {                     //4 为check框
                    cusFieldValMap.put(new Integer(requiredFieldIndex), "isCheck");
                } else if (fieldhtmltype.equals("3")) {
                    if (recordSet.getString("type").equals("2")) {
                        cusFieldValMap.put(new Integer(requiredFieldIndex), "isDate");
                    } else if (recordSet.getString("type").equals("19")) {
                        cusFieldValMap.put(new Integer(requiredFieldIndex), "isTime");
                    } else {
                        cusFieldValMap.put(new Integer(requiredFieldIndex), "isBrowser");
                    }
                } else if (fieldhtmltype.equals("5")) {
                    cusFieldValMap.put(new Integer(requiredFieldIndex), "isSelect");
                }
            }
        }
        voField = voField + baseField;
        tempField = tempField + baseFieldLabel;
        temFields = tempField.split(",");
        baseFieldsLastIndex1 = temFields.length - 1;


        /*个人信息字段*/
        String personSql = "select t1.fieldid,t1.hrm_fieldlable,t1.ismand,t2.fielddbtype,t2.fieldhtmltype,t2.type from cus_formfield t1, cus_formdict t2 where t1.scope='HrmCustomFieldByInfoType' and t1.scopeid=1 and t1.fieldid=t2.id order by t1.fieldorder";
        recordSet.execute(personSql);
        String personField = "";
        String personalFieldLabel = "";
        fieldhtmltype = "";
        while (recordSet.next()) {
            fieldhtmltype = recordSet.getString("fieldhtmltype");
            if (fieldhtmltype.equals("1") || fieldhtmltype.equals("2") || fieldhtmltype.equals("3")
                    || fieldhtmltype.equals("4") || fieldhtmltype.equals("5")) {
                personField = personField + ",field" + recordSet.getInt("fieldid");
                personalFieldLabel = personalFieldLabel + "," + recordSet.getString("hrm_fieldlable");
                requiredFieldIndex++;
                //验证条件
                //1为html文本类型
                if (fieldhtmltype.equals("1")) {
                    if (recordSet.getString("type").equals("1")) {           //1为文本数据类型
                        String fielddbtype = recordSet.getString("fielddbtype");
                        fielddbtype = fielddbtype.substring(fielddbtype.indexOf("(") + 1, fielddbtype.lastIndexOf(")")); //截取字符设置的长度
                        cusFieldValMap.put(new Integer(requiredFieldIndex), fielddbtype);
                    } else if (recordSet.getString("type").equals("2")) {     // 2为整数数据类型
                        cusFieldValMap.put(new Integer(requiredFieldIndex), "isInt");
                    } else if (recordSet.getString("type").equals("3")) {     // 3为浮点数数据类型
                        cusFieldValMap.put(new Integer(requiredFieldIndex), "isDouble");
                    } else {
                        cusFieldValMap.put(new Integer(requiredFieldIndex), "");
                    }
                } else if (fieldhtmltype.equals("4")) {                     //4 为check框
                    cusFieldValMap.put(new Integer(requiredFieldIndex), "isCheck");
                } else if (fieldhtmltype.equals("3")) {
                    if (recordSet.getString("type").equals("2")) {
                        cusFieldValMap.put(new Integer(requiredFieldIndex), "isDate");
                    } else if (recordSet.getString("type").equals("19")) {
                        cusFieldValMap.put(new Integer(requiredFieldIndex), "isTime");
                    } else {
                        cusFieldValMap.put(new Integer(requiredFieldIndex), "isBrowser");
                    }
                } else if (fieldhtmltype.equals("5")) {
                    cusFieldValMap.put(new Integer(requiredFieldIndex), "isSelect");
                }
            }
        }
        voField = voField + personField;
        tempField = tempField + personalFieldLabel;
        temFields = tempField.split(",");
        personFieldsLastIndex = temFields.length - 1;

        /*个人信息字段*/
        /*工作信息字段*/
        String workSql = "select t1.fieldid,t1.hrm_fieldlable,t2.fielddbtype,t2.fieldhtmltype,t2.type from cus_formfield t1, cus_formdict t2 where t1.scope='HrmCustomFieldByInfoType' and t1.scopeid=3 and t1.fieldid=t2.id order by t1.fieldorder";
        recordSet.execute(workSql);
        String workField = "";
        String workFieldLabel = "";
        while (recordSet.next()) {
            fieldhtmltype = recordSet.getString("fieldhtmltype");
            if (fieldhtmltype.equals("1") || fieldhtmltype.equals("2") || fieldhtmltype.equals("3")
                    || fieldhtmltype.equals("4") || fieldhtmltype.equals("5")) {
                workField = workField + ",field" + recordSet.getInt("fieldid");
                workFieldLabel = workFieldLabel + "," + recordSet.getString("hrm_fieldlable");
                requiredFieldIndex++;
                //验证条件
                //1为html文本类型
                if (fieldhtmltype.equals("1")) {
                    if (recordSet.getString("type").equals("1")) {           //1为文本数据类型
                        String fielddbtype = recordSet.getString("fielddbtype");
                        fielddbtype = fielddbtype.substring(fielddbtype.indexOf("(") + 1, fielddbtype.lastIndexOf(")")); //截取字符设置的长度
                        cusFieldValMap.put(new Integer(requiredFieldIndex), fielddbtype);
                    } else if (recordSet.getString("type").equals("2")) {     // 2为整数数据类型
                        cusFieldValMap.put(new Integer(requiredFieldIndex), "isInt");
                    } else if (recordSet.getString("type").equals("3")) {     // 3为浮点数数据类型
                        cusFieldValMap.put(new Integer(requiredFieldIndex), "isDouble");
                    } else {
                        cusFieldValMap.put(new Integer(requiredFieldIndex), "");
                    }
                } else if (fieldhtmltype.equals("4")) {                     //4 为check框
                    cusFieldValMap.put(new Integer(requiredFieldIndex), "isCheck");
                } else if (fieldhtmltype.equals("3")) {
                    if (recordSet.getString("type").equals("2")) {
                        cusFieldValMap.put(new Integer(requiredFieldIndex), "isDate");
                    } else if (recordSet.getString("type").equals("19")) {
                        cusFieldValMap.put(new Integer(requiredFieldIndex), "isTime");
                    } else {
                        cusFieldValMap.put(new Integer(requiredFieldIndex), "isBrowser");
                    }
                } else if (fieldhtmltype.equals("5")) {
                    cusFieldValMap.put(new Integer(requiredFieldIndex), "isSelect");
                }
            }
        }
        voField = voField + workField;
        tempField = tempField + workFieldLabel;
        voFields = voField.split(",");
        temFields = tempField.split(",");
        requiredFields = requiredField.split(",");
        workFieldLastIndex = temFields.length - 1;
        /*工作信息字段*/
    }

    /**
     * 获取excel单元格值
     * @param cell   要读取的单元格对象
     * @return
     */
    public String getCellValue(Cell cell) {
        String cellValue = "";
        if (cell == null)
            return "";
        switch (cell.getCellType()) {
            case BOOLEAN:                                  //得到Boolean对象的方法
                cellValue = String.valueOf(cell.getBooleanCellValue());
                break;
            case NUMERIC:
                if (HSSFDateUtil.isCellDateFormatted(cell)) {//先看是否是日期格式
                    SimpleDateFormat sft = new SimpleDateFormat("yyyy-MM-dd");
                    cellValue = String.valueOf(sft.format(cell.getDateCellValue()));   //读取日期格式
                } else {
                    cellValue = String.valueOf(new Double(cell.getNumericCellValue())); //读取数字
                    if (cellValue.endsWith(".0"))
                        cellValue = cellValue.substring(0, cellValue.indexOf("."));
                }
                break;
            case FORMULA:                               //读取公式
                cellValue = cell.getCellFormula();
                break;
            case STRING:                              //读取String
                cellValue = cell.getStringCellValue();
                if(cellValue == null)
                    cellValue = "";
                Date d = validDate(cellValue);
                if(d != null)
                    cellValue = new SimpleDateFormat("yyyy-MM-dd").format(d);
                break;
        }
        cellValue = Util.toHtmlForHrm(cellValue);
        return cellValue;
    }

    private Date validDate(String str) {
        Date d = null;
        SimpleDateFormat fmt1 = new SimpleDateFormat("yyyy-MM-dd");
        fmt1.setLenient(false);
        SimpleDateFormat fmt2 = new SimpleDateFormat("yyyy/MM/dd");
        fmt2.setLenient(false);
        try{
            d = fmt1.parse(str);
        }catch (Exception e){
        }
        try{
            d = fmt2.parse(str);
        }catch (Exception e){

        }
        return d;
    }

    /**
     * 获取指定行、列的单元格值
     * @param rowNum
     * @param cellNum
     * @return
     */
    public String getCellValue(int rowNum, int cellNum) {
        Row row = sheet.getRow(rowNum);
        Cell cell = row.getCell((short) cellNum);
        String cellValue = getCellValue(cell);
        cellValue = Util.toHtmlForHrm(cellValue);
        return cellValue;
    }

    //浮点数判断
    public boolean isDecimal(String str) {
        if (str == null || "".equals(str))
            return false;
        Pattern pattern = Pattern.compile("[0-9]*(\\.?)[0-9]*");
        return pattern.matcher(str).matches();
    }

    //整数判断
    public boolean isInteger(String str) {
        if (str == null)
            return false;
        Pattern pattern = Pattern.compile("[0-9]+");
        return pattern.matcher(str).matches();
    }

    /**
     * 获取字符串字节长度 由于java中中英字符都按1个字符，而数据库中汉字按两个字符计算
     * @param str
     * @return
     */
    public int getStrLength(String str) {
        try {
            if (str == null)
                return 0;
            else
                return new String(str.getBytes("gb2312"), "iso-8859-1").length();
        } catch (Exception e) {
            writeLog(e);
            return 0;
        }
    }

    public int getUserlanguage() {
        return userlanguage;
    }


    public void setUserlanguage(int userlanguage) {
        this.userlanguage = userlanguage;
    }
}
