package com.mycompany.app;

import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.SQLException;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

final class Utils {

	/** Connect to the database */
	public Connection doGetConnection() {

		Connection connection = null;

		try {
			Context initContext = new InitialContext();
			DataSource ds = (DataSource) initContext.lookup("java:jboss/postgresDS");
			connection = ds.getConnection();
			if (connection != null) {
				System.out.println("This is the DB connection: " + connection.toString());
			}
		} catch (NamingException ex) {
			System.err.println(ex);
		} catch (SQLException ex) {
			System.err.println(ex);
		}
		return connection;
	}

	/** Execute passed SQL query */
	public ResultSet execQuery(String query) throws SQLException {

		Connection connection = doGetConnection();

		return connection.createStatement().executeQuery(query);

	}

	/** Get AreaCode from DB, based on the CellID specified the P-Access-Network-Info header field */
	public String getAreaCode(String PAccessNetworkInfo) {

		String areacode = null;
		String headercellid = PAccessNetworkInfo.split("=")[1];
		System.out.println("---------------Extracted CELLID:" + headercellid);
		String sqlstatement = "select \"AREA_CODE\" from public.\"CELL\" where \"CELL_ID\" = '" + headercellid + "'";
		System.out.println("---------------executed query:" + sqlstatement);
		try{
			ResultSet rs = execQuery(sqlstatement);
			rs.next();
			areacode = rs.getString(1);
		}
		catch (SQLException ex) {
	        System.err.println(ex);
	    }
		return areacode;
	}

}
