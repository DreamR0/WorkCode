insert into htmllabelinfo(indexid,labelname,languageid) values(-20240101,'生效范围(分部)',7);
insert into htmllabelindex(id,indexdesc) values(-20240101,'生效范围(分部)');

insert into htmllabelinfo(indexid,labelname,languageid) values(-20240102,'生效范围(部门)',7);
insert into htmllabelindex(id,indexdesc) values(-20240102,'生效范围(部门)');

alter table kq_ReportShare add subcomIdCustom VARCHAR(10);

alter table kq_ReportShare add deptIdCustom VARCHAR(10);
/