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

	/** Get destination from DB, based on the CellID specified the P-Access-Network-Info header field */
	public String getDestination(String PAccessNetworkInfo) {

		String destination = null;
		String headercellid = PAccessNetworkInfo.split("=")[1];
		System.out.println("---------------Extracted CELLID:" + headercellid);
		String sqlstatement = "select d.\"DESTINATION\" from public.\"DESTINATION\" d, public.\"CELL\" c where c.\"CELL_ID\" = '" + headercellid + "' and c.\"AREA_CODE\" = d.\"AREA_CODE\"";
		System.out.println("---------------executed query:" + sqlstatement);
		try{
			ResultSet rs = execQuery(sqlstatement);
			rs.next();
			destination = rs.getString(1);
		}
		catch (SQLException ex) {
	        System.err.println(ex);
	    }
		return destination;
	}

}
