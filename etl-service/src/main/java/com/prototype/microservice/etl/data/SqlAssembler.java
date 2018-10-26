package com.prototype.microservice.etl.data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.prototype.microservice.etl.utils.EtlHelper;

@Component
public class SqlAssembler {
	
	public String countDataByColumns(String tableName, List<ColumnMetaInfo> columns, List<String> values){
		StringBuilder sql = new StringBuilder(" SELECT COUNT(1) FROM ");
		String stmt = getCriteriaStmt(tableName, columns, values);
		if(StringUtils.isNotBlank(stmt)){
			sql.append(stmt);			
			return sql.toString();
		}
		return null;
	}
	public String getCriteriaStmt(String tableName, List<ColumnMetaInfo> columns, List<String> values){
		if(StringUtils.isBlank(tableName)||columns==null||values==null||columns.size()!=values.size()){
			return null;
		}
		StringBuilder sql = new StringBuilder("");
		sql.append(tableName.toUpperCase());
		sql.append(" WHERE 1=1 ");
		for(int i=0;i<columns.size();i++){
			ColumnMetaInfo column = columns.get(i);
			if(column!=null){
				sql.append(" AND ");
				sql.append(column.getTableColName().toUpperCase());
				String v = RptColumnParser.parseColumnValueForNative(values.get(i), columns.get(i), null);
				if(v==null){
					sql.append(" IS NULL ");
				}else{
					sql.append(" = ");
					sql.append(v);
				}
			}
		}
		return sql.toString();
	}
	public String genNativeInsertSqlByColumnHeader(String tableName, List<ColumnMetaInfo> columnsInfo, List<String> values, Map<String, String> sysColVal, List<String> nullValFilter){
		if(columnsInfo.size()!=values.size()){
			return null;
		}
		IntStream.range(0, columnsInfo.size()).forEach(i->{
			String v = RptColumnParser.parseColumnValueForNative(values.get(i), columnsInfo.get(i), nullValFilter);
			values.set(i, v);
		});
		List<String> columns = genColumnNames(columnsInfo);
		appendSysColumn(columns, values, sysColVal);
		StringBuilder sql = new StringBuilder("INSERT INTO ");
		String columnsStr =columns.toString();
		String valuesStr = values.toString();
		columnsStr = columnsStr.replace("[", "(");
		columnsStr = columnsStr.replace("]", ")");
		valuesStr = valuesStr.replace("[", "(");
		valuesStr = valuesStr.replace("]", ")");
		sql.append(tableName+" ");
		sql.append(columnsStr);
		sql.append(" VALUES "+valuesStr);
		
		return sql.toString();
	}
	public String genNativeInsertSqlByColumnIndex(String tableName, List<ColumnMetaInfo> columnsInfo, List<String> values, Map<String, String> sysColVal, List<String> nullValFilter){
		if(columnsInfo.size()!=values.size()){
			return null;
		}
		List<String> columns = genColumnNames(columnsInfo);
		List<String> valuesAlignColumnOrder = new ArrayList<String>(values);
		IntStream.range(0, columnsInfo.size()).forEach(i->{
			String v = RptColumnParser.parseColumnValueForNative(values.get(i), columnsInfo.get(i), nullValFilter);
			int index = columnsInfo.get(i).getColIndex();
			valuesAlignColumnOrder.set(index-1, v);
		});
		appendSysColumn(columns, valuesAlignColumnOrder, sysColVal);
		StringBuilder sql = new StringBuilder("INSERT INTO ");
		String columnsStr =columns.toString();
		String valuesStr = valuesAlignColumnOrder.toString();
		columnsStr = columnsStr.replace("[", "(");
		columnsStr = columnsStr.replace("]", ")");
		valuesStr = valuesStr.replace("[", "(");
		valuesStr = valuesStr.replace("]", ")");
		sql.append(tableName+" ");
		sql.append(columnsStr);
		sql.append(" VALUES "+valuesStr);
		
		return sql.toString();
	}
	public String genDeleteSqlByDate(String tableName, List<ColumnMetaInfo> columns, List<String> values){
		StringBuilder sql = new StringBuilder(" DELETE ");
		String stmt = getCriteriaStmt(tableName, columns, values);
		if(StringUtils.isNotBlank(stmt)){
			sql.append(stmt);			
			return sql.toString();
		}
		return null;
	}
	public List<Object> parseValues(List<ColumnMetaInfo> columnsInfo, List<String> values){
		List<Object> valueList = new ArrayList<Object>(values.size());
		IntStream.range(0, values.size()).forEach(i->{
			Object v = RptColumnParser.parseColumnForParam(values.get(i), columnsInfo.get(i));
			valueList.add(i, v);
		});
		appendSysColumnToValue(valueList);
		return valueList;
	}
	public String genInsertSqlWithParam(String tableName, List<ColumnMetaInfo> columnsInfo){
		List<String> columns = genColumnNames(columnsInfo);
		List<String> params = new ArrayList<String>(columns.size());
		IntStream.range(0, columnsInfo.size()).forEach(i->{
			params.add(i, "?"+columnsInfo.get(i).getColIndex());
		});
		appendSysColumnToParam(columns, params);
		StringBuilder sql = new StringBuilder("INSERT INTO ");
		String columnsStr =columns.toString();
		String paramStr = params.toString();
		columnsStr = columnsStr.replace("[", "(");
		columnsStr = columnsStr.replace("]", ")");
		paramStr = paramStr.replace("[", "(");
		paramStr = paramStr.replace("]", ")");
		sql.append(tableName+" ");
		sql.append(columnsStr);
		sql.append(" VALUES "+paramStr);
		
		return sql.toString();
	}
	public static List<String> genColumnNames(List<ColumnMetaInfo> columns){
		List<String> columnList = null;
		if(columns!=null&&columns.size()>0){
			columnList = new ArrayList<String>();
		}
		for(ColumnMetaInfo column:columns){
			if(StringUtils.isNotBlank(column.getTableColName())){
				columnList.add(column.getTableColName());
			}
		}
		columnList.forEach((s)->s=s.trim());
		//System.out.print(columnList.toString());
		return columnList;
	}
	
	public void appendSysColumn(List<String> columns, List<String> values, Map<String, String> sysColVal){
		if(sysColVal!=null&&sysColVal.size()>0){
			for(Map.Entry<String, String> entry: sysColVal.entrySet()){
				if(columns!=null){
					columns.add(entry.getKey());
				}
				if(values!=null){
					String val = entry.getValue();
					values.add(val);
				}
			}
			
		}
	}
	
	public void appendSysColumnToParam(List<String> columns, List<String> params){
		if(columns instanceof ArrayList){
			columns.add("FILE_DATE");
		}
		if(params instanceof ArrayList){
			params.add("?"+(params.size()+1));
		}
	}
	public void appendSysColumnToValue(List<Object> values){
		if(values instanceof ArrayList){
			Date value = EtlHelper.getCurrentDate();
			values.add(value);
		}
	}
}
